package com.example.filtergallery

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.NonNull
import com.example.filtergallery.Utils.getOptimalSize
import com.example.filtergallery.gl.CustomGLSurfaceView

/**
 * Class where the main work with the camera takes place
 * **/
class CustomCamera(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val mainCustomSurfaceView: CustomGLSurfaceView?,
    private val widthMainCamera: Int,
    private val heightMainCamera: Int,
    private val cameraId: String,
    private val cameraHandler: Handler,
    private val deviceOpenCallback: () -> Unit
) {

    val TAG = "Filter_CameraV2"

    private var surfaceTextures: MutableList<CustomFilterSurface> = mutableListOf()
    private var cameraDevice: CameraDevice? = null
    private var previewSize: Size? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var captureRequest: CaptureRequest? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null


    val isOpen
        get() = cameraDevice != null

    fun closeCamera() {
        cameraCaptureSession?.stopRepeating()
        cameraCaptureSession?.close()
        surfaceTextures = mutableListOf()
        cameraDevice?.close()
        cameraDevice = null
    }

    fun setupCamera(): String {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.let { getOptimalSize(it, widthMainCamera, heightMainCamera) }
            setupImageReader()
            Log.i(TAG, "preview width = " + previewSize?.width + ", height = " + previewSize?.height + ", cameraId = " + cameraId)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return cameraId
    }

    fun openCamera(): Boolean {
        return if (context.checkCallingOrSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, stateCallback, cameraHandler)
            true
        } else {
            false
        }
    }

    fun takePicture() {
        lockFocus()
    }

    fun setPreviewTexture(customFilterSurface: CustomFilterSurface) {
        if (!surfaceTextures.any { it.nameOfFilter == customFilterSurface.nameOfFilter }) {
            surfaceTextures.add(customFilterSurface)
        } else {
            surfaceTextures.first { it.nameOfFilter == customFilterSurface.nameOfFilter }.surfaceTexture =
                customFilterSurface.surfaceTexture
        }
    }

    fun startPreview() {
        try {
            cameraDevice?.createCaptureSession(
                surfaceTextures.toSurface(previewSize) + imageReader?.surface,
                cameraPreviewStateCallback,
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val cameraPreviewStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(@NonNull session: CameraCaptureSession) {
            try {
                captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                surfaceTextures.toSurface(previewSize).forEach { captureRequestBuilder?.addTarget(it) }
                captureRequest = captureRequestBuilder?.build()
                cameraCaptureSession = session
                captureRequest?.let { cameraCaptureSession?.setRepeatingRequest(it, null, cameraHandler) }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(@NonNull session: CameraCaptureSession) {}
    }

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(@NonNull camera: CameraDevice) {
            cameraDevice = camera
            deviceOpenCallback.invoke()
        }

        override fun onDisconnected(@NonNull camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(@NonNull camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private val mCaptureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {}
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            capture()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        mainCustomSurfaceView?.saveImage()
        Log.i(TAG, "A photo is available for save")
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(
            previewSize?.width ?: 1920,
            previewSize?.height ?: 1080,
            ImageFormat.JPEG,
            1
        )
        imageReader?.setOnImageAvailableListener(onImageAvailableListener, cameraHandler)
    }

    private fun lockFocus() {
        try {
            captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureRequestBuilder?.let { cameraCaptureSession?.capture(it.build(), mCaptureCallback, cameraHandler) }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun unLockFocus() {
        try {
            captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            cameraCaptureSession?.setRepeatingRequest(captureRequest!!, null, cameraHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun capture() {
        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            imageReader?.let { captureBuilder?.addTarget(it.surface) }
            val captureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Toast.makeText(context, "Image Saved!", Toast.LENGTH_SHORT).show()
                    unLockFocus()
                }
            }
            cameraCaptureSession?.stopRepeating()
            captureBuilder?.build()?.let { cameraCaptureSession?.capture(it, captureCallback, null) }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
}