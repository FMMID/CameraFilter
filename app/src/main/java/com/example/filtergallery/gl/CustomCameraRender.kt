package com.example.filtergallery.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.filtergallery.CustomFilter
import com.example.filtergallery.CustomFilterSurface
import com.example.filtergallery.Utils
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Class for rendering frames from camera to custom GL surface
 * **/
class CustomCameraRender(
    var isSaveImage: Boolean = false,
    var surfaceView: CustomGLSurfaceView?,
    var isPreviewStarted: Boolean,
    var customFilter: CustomFilter,
    var isRefreshFilterNeeded: Boolean = false,
    private val availableCustomFilterSurfaceCallback: (CustomFilterSurface) -> Unit,
    private val savePictureCallback: (Bitmap) -> Unit
) : GLSurfaceView.Renderer {

    private val TAG = "Filter_CameraV2Renderer"
    private var oesTextureId = -1
    private var surfaceTexture: SurfaceTexture? = null
    private var transformMatrix = FloatArray(16)
    private var customCameraFilterEngine: CustomCameraFilterEngine? = null
    private var dataBuffer: FloatBuffer? = null
    private var shaderProgram = -1
    private var aPositionLocation = -1
    private var aTextureCoordLocation = -1
    private var uTextureMatrixLocation = -1
    private var uTextureSamplerLocation = -1
    private var fboIds = IntArray(1)

    private var currentHeight: Int = 1920
    private var currentWidth: Int = 1080

    /**
     * Called when created a GL context where you can use GL function
     * **/
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        if (surfaceView == null) return
        oesTextureId = Utils.createOESTextureObject()
        customCameraFilterEngine = CustomCameraFilterEngine(oesTextureId, surfaceView?.context, customFilter)
        dataBuffer = customCameraFilterEngine!!.getBuffer()
        shaderProgram = customCameraFilterEngine!!.getShaderProgram()
        GLES20.glGenFramebuffers(1, fboIds, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboIds[0])
        Log.i(TAG, "onSurfaceCreated: mFBOId: " + fboIds[0])
    }

    /**
     * Called when a device changed orientation
     * **/
    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        if (surfaceView == null) return
        currentHeight = height
        currentWidth = width
        GLES20.glViewport(0, 0, width, height)
        Log.i(TAG, "onSurfaceChanged: $width, $height")
    }

    /**
     * It is called constantly when there is a broadcast from the camera, here there is a steady rendering of the image
     * from camera
     * **/
    override fun onDrawFrame(p0: GL10?) {
        if (surfaceView == null) return
        val t1 = System.currentTimeMillis()

        if (isRefreshFilterNeeded) {
            isRefreshFilterNeeded = false
            oesTextureId = Utils.createOESTextureObject()
            customCameraFilterEngine = CustomCameraFilterEngine(oesTextureId, surfaceView?.context, customFilter)
            dataBuffer = customCameraFilterEngine!!.getBuffer()
            shaderProgram = customCameraFilterEngine!!.getShaderProgram()
            GLES20.glGenFramebuffers(1, fboIds, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboIds[0])
            Log.i(TAG, "onSurfaceCreated: mFBOId: " + fboIds[0])
        }

        if (!isPreviewStarted) {
            isPreviewStarted = true
            isPreviewStarted = initSurfaceTexture(surfaceView)
            return
        }

        if (surfaceTexture != null) {
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(transformMatrix)
        }

        //glClear(GL_COLOR_BUFFER_BIT);

        //glClear(GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, CustomCameraFilterEngine.POSITION_ATTRIBUTE)
        aTextureCoordLocation = GLES20.glGetAttribLocation(shaderProgram, CustomCameraFilterEngine.TEXTURE_COORD_ATTRIBUTE)
        uTextureMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, CustomCameraFilterEngine.TEXTURE_MATRIX_UNIFORM)
        uTextureSamplerLocation = GLES20.glGetUniformLocation(shaderProgram, CustomCameraFilterEngine.TEXTURE_SAMPLER_UNIFORM)

        GLES20.glActiveTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureSamplerLocation, 0)
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0)

        if (dataBuffer != null) {
            dataBuffer?.position(0)
            GLES20.glEnableVertexAttribArray(aPositionLocation)
            GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 16, dataBuffer)
            dataBuffer?.position(2)
            GLES20.glEnableVertexAttribArray(aTextureCoordLocation)
            GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 16, dataBuffer)
        }

        //glDrawElements(GL_TRIANGLE_FAN, 6,GL_UNSIGNED_INT, 0);
        //glDrawArrays(GL_TRIANGLE_FAN, 0 , 6);

        //glDrawElements(GL_TRIANGLE_FAN, 6,GL_UNSIGNED_INT, 0);
        //glDrawArrays(GL_TRIANGLE_FAN, 0 , 6);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        //glDrawArrays(GL_TRIANGLES, 3, 3);
        //glDrawArrays(GL_TRIANGLES, 3, 3);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        if (isSaveImage) {
            isSaveImage = false
            val iat = IntArray(currentWidth * currentHeight)
            val ib = IntBuffer.allocate(currentWidth * currentHeight)
            GLES20.glReadPixels(0, 0, currentWidth, currentHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib)
            val ia = ib.array()
            for (i in 0 until currentHeight) {
                System.arraycopy(ia, i * currentWidth, iat, (currentHeight - i - 1) * currentWidth, currentWidth)
            }
            val bitmap = Bitmap.createBitmap(currentWidth, currentHeight, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat))
            savePictureCallback.invoke(bitmap)
        }
        val t2 = System.currentTimeMillis()
        val t = t2 - t1
        Log.i(TAG, "onDrawFrame: time: $t")
    }

    /**
     * You need to create a special surface to pass a link to it to the camera, this texture will be bound to it
     * **/
    private fun initSurfaceTexture(surfaceView: CustomGLSurfaceView? = null): Boolean {
        if (surfaceView == null) {
            Log.i(TAG, "mCamera or mGLSurfaceView is null!")
            return false
        }
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture?.setOnFrameAvailableListener { surfaceView.requestRender() }
        val customFilterSurface = CustomFilterSurface(
            nameOfFilter = customFilter.nameOfFilter,
            surfaceTexture = surfaceTexture!!,
            viewPortWidth = currentWidth,
            viewPortHeight = currentHeight
        )
        availableCustomFilterSurfaceCallback.invoke(customFilterSurface)
        return true
    }
}