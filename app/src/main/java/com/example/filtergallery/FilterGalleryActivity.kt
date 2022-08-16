package com.example.filtergallery

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.filtergallery.PERMISSION.REQUIRED_PERMISSIONS
import com.example.filtergallery.gl.CustomCameraRender
import com.example.filtergallery.gl.CustomGLSurfaceView
import com.example.filtergallery.gl.EGLBackgroundEnvironment
import com.example.filtergallery.gl.SimpleFilter
import com.example.filtergallery.recycler.CustomItemDecorator
import com.example.filtergallery.recycler.FilterGalleryAdapter
import com.example.filtergallery.recycler.OnSnapPositionChangeListener
import com.example.filtergallery.recycler.SnapOnScrollListener
import com.example.filtergallery.recycler.attachSnapHelperWithListener
import java.io.File


class FilterGalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var filterPopup: TextView
    private lateinit var changeCameraView: View
    private lateinit var makePictureButton: View
    private lateinit var customGLSurfaceView: CustomGLSurfaceView
    private lateinit var loadedPictureFromCameraView: ImageView

    private var linearSnapHelper: LinearSnapHelper? = null
    private var savePictureThread: HandlerThread? = HandlerThread(RENDER_HANDLER)
    private var savePictureHandler: Handler? = null
    private var cameraThread: HandlerThread? = HandlerThread(CAMERA_HANDLER)
    private var cameraHandler: Handler? = null
    private var filterGalleryAdapter: FilterGalleryAdapter? = null
    private var listOfCamera: MutableMap<Int, CustomCamera> = mutableMapOf()
    private var currentCamera: Int = CameraCharacteristics.LENS_FACING_BACK

    companion object {
        private const val RENDER_HANDLER = "RenderHandler"
        private const val CAMERA_HANDLER = "CameraHandler"
        const val DEFAULT_SAVE_FILTER_VIEW_EXP = ".jpg"
        val CUSTOM_FILTER_IMAGES_PATH: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }

    private val listOfFilters: List<CustomFilter> = listOf(
        CustomFilter(
            nameOfFilter = "Default Filter",
            shaderId = R.raw.default_fragment_shader
        ),
        CustomFilter(
            nameOfFilter = "Gray Filter",
            shaderId = R.raw.gray_fragment_shader
        ),
        CustomFilter(
            nameOfFilter = "Inverse Filter",
            shaderId = R.raw.inverse_fragment_shader
        ),
        CustomFilter(
            nameOfFilter = "Negative Filter",
            shaderId = R.raw.negative_fragment_shader
        )
    )

    private val mainCameraFilter = CustomFilter(
        nameOfFilter = "MainCamera",
        shaderId = R.raw.default_fragment_shader
    )

    private fun startBackgroundThreads() {
        savePictureThread?.start()
        cameraThread?.start()
        if (savePictureThread != null && cameraThread != null) {
            savePictureHandler = Handler(savePictureThread!!.looper)
            cameraHandler = Handler(cameraThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        savePictureThread?.quitSafely()
        cameraThread?.quitSafely()
        try {
            savePictureThread?.join()
            cameraThread?.join()
            savePictureThread = null
            cameraThread = null
            savePictureHandler = null
            cameraHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThreads()
        if (allPermissionsGranted()) {
            initFilterGallery()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION.REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundThread()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.filter_gallery_view)
        makePictureButton = findViewById(R.id.filter_gallery_make_picture_button)
        customGLSurfaceView = findViewById(R.id.filter_gallery_custom_gl_surface_view)
        recyclerView = findViewById(R.id.filter_gallery_recycler_view)
        filterPopup = findViewById(R.id.filter_gallery_filter_popup)
        changeCameraView = findViewById(R.id.filter_gallery_change_camera)
        loadedPictureFromCameraView = findViewById(R.id.filter_gallery_loaded_picture)
    }

    private fun initFilterGallery() {
        setupCameras()
        setupListeners()
        setupRecycler()
        createGalleryPictures()
    }

    private fun createGalleryPictures() {
        listOfFilters.forEach { customFilter ->
            val file = File(CUSTOM_FILTER_IMAGES_PATH, customFilter.nameOfFilter + DEFAULT_SAVE_FILTER_VIEW_EXP)
            CUSTOM_FILTER_IMAGES_PATH.mkdirs()
            if (!file.exists()) {
                val backEnv = EGLBackgroundEnvironment(720, 720)
                backEnv.threadOwner = mainLooper.thread.name
                backEnv.setFilter(
                    SimpleFilter(
                        context = baseContext,
                        vertexRes = R.raw.default_vertex_shader,
                        fragmentRes = customFilter.shaderId
                    )
                )
                backEnv.setInput(Utils.loadBitmap(resources, R.drawable.gallery))
                savePictureHandler?.post(ImageSaver(backEnv.bitmap, file))
            }
        }
        setupAdapter()
    }

    private fun changeCameraView() {
        if (listOfCamera[currentCamera]?.isOpen == true) {
            listOfCamera[currentCamera]?.closeCamera()
        }
        currentCamera = if (currentCamera == CameraCharacteristics.LENS_FACING_BACK) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        listOfCamera[currentCamera]?.openCamera()
    }

    private fun setupListeners() {
        changeCameraView.setOnClickListener {
            changeCameraView()
        }
        makePictureButton.setOnClickListener {
            listOfCamera[currentCamera]?.takePicture()
        }
    }

    private fun setupAdapter() {
        filterGalleryAdapter = FilterGalleryAdapter(
            items = listOfFilters,
            onFilterClickListenerCallback = ::onFilterClickListener,
        )
        recyclerView.adapter = filterGalleryAdapter
    }

    private fun setupRecycler() {
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.addItemDecoration(CustomItemDecorator())
        linearSnapHelper = linearSnapHelper ?: LinearSnapHelper()
        recyclerView.attachSnapHelperWithListener(
            linearSnapHelper!!,
            SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
            onSnapItemListener
        )
    }

    private fun setupCameras() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        cameraManager.cameraIdList.forEach { cameraID ->
            val camera = cameraManager.getCameraCharacteristics(cameraID).get(CameraCharacteristics.LENS_FACING)
            if (camera != null) {
                listOfCamera[camera] = CustomCamera(
                    context = this,
                    cameraManager = cameraManager,
                    mainCustomSurfaceView = customGLSurfaceView,
                    widthMainCamera = dm.widthPixels,
                    heightMainCamera = dm.heightPixels,
                    cameraId = cameraID,
                    cameraHandler = cameraHandler ?: Handler(),
                    deviceOpenCallback = ::renderImage
                )
            }
        }
        listOfCamera.forEach {
            it.value.setupCamera()
        }
        listOfCamera[currentCamera]?.openCamera()
    }

    private fun renderImage() {
        if (customGLSurfaceView.getCustomCameraRender() != null) {
            customGLSurfaceView.updateRender()
        } else {
            customGLSurfaceView.setupCameraRender(
                CustomCameraRender(
                    isSaveImage = false,
                    customFilter = mainCameraFilter,
                    availableCustomFilterSurfaceCallback = ::availableCustomSurfaceFilterListener,
                    savePictureCallback = ::savePictureListener,
                    surfaceView = customGLSurfaceView,
                    isPreviewStarted = false
                )
            )
        }
    }

    private fun savePictureListener(bitmap: Bitmap) {
        mainLooper.thread.runCatching {
            loadedPictureFromCameraView.setImageBitmap(bitmap)
        }
        savePictureHandler?.post(ImageSaver(bitmap))
    }

    private val onSnapItemListener = object : OnSnapPositionChangeListener {
        override fun onSnapPositionChange(position: Int) {
            val customFilter = filterGalleryAdapter?.getItemCustomFilter(position)
            filterPopup.text = customFilter?.nameOfFilter
            filterPopup.isVisible = true
            val fadeIn: Animation = AnimationUtils.loadAnimation(this@FilterGalleryActivity, R.anim.fade_in_popup_animation)
            fadeIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    val fadeOut: Animation =
                        AnimationUtils.loadAnimation(this@FilterGalleryActivity, R.anim.fade_out_popup_animation).apply {
                            setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(p0: Animation?) {}
                                override fun onAnimationRepeat(p0: Animation?) {}
                                override fun onAnimationEnd(p0: Animation?) {
                                    filterPopup.isVisible = false
                                }
                            })
                        }
                    filterPopup.startAnimation(fadeOut)
                }
            })
            filterPopup.startAnimation(fadeIn)
            if (customFilter != null) {
                mainCameraFilter.shaderId = customFilter.shaderId
                customGLSurfaceView.changeFilter(mainCameraFilter)
            }
        }
    }

    private fun availableCustomSurfaceFilterListener(customFilterSurface: CustomFilterSurface) {
        listOfCamera.forEach {
            it.value.setPreviewTexture(customFilterSurface)
        }
        listOfCamera[currentCamera]?.startPreview()
    }

    private fun onFilterClickListener(position: Int) {
        recyclerView.smoothScrollToPosition(position)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                finish()
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}