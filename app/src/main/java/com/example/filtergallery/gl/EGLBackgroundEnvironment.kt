package com.example.filtergallery.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.nio.IntBuffer

/**
 * Class provide render demo images through EGL surface in the background
 * **/
class EGLBackgroundEnvironment(private val mWidth: Int, private val mHeight: Int) {

    private val eglEnvironment: EGLEnvironment = EGLEnvironment()
    private var generalFilter: GeneralFilter? = null

    var imageBitmap: Bitmap? = null
    var threadOwner: String? = null

    fun setFilter(filter: GeneralFilter?) {
        generalFilter = filter

        // Does this thread own the OpenGL context?
        if (Thread.currentThread().name != threadOwner) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.")
            return
        }
        // Call the renderer initialization routines
        generalFilter?.create()
        generalFilter?.setSize(mWidth, mHeight)
    }

    val bitmap: Bitmap?
        get() {
            if (generalFilter == null) {
                Log.e(TAG, "getBitmap: Renderer was not set.")
                return null
            }
            if (Thread.currentThread().name != threadOwner) {
                Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.")
                return null
            }
            generalFilter?.textureId = createTexture(imageBitmap)
            generalFilter?.draw()
            return convertToBitmap()
        }

    fun destroy() {
        eglEnvironment.destroy()
    }

    private fun convertToBitmap(): Bitmap {
        val iat = IntArray(mWidth * mHeight)
        val ib = IntBuffer.allocate(mWidth * mHeight)
        eglEnvironment.gl?.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib)
        val ia = ib.array()

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (i in 0 until mHeight) {
            System.arraycopy(ia, i * mWidth, iat, (mHeight - i - 1) * mWidth, mWidth)
        }
        val bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat))
        return bitmap
    }

    fun setInput(bitmap: Bitmap?) {
        imageBitmap = bitmap
    }

    private fun createTexture(bmp: Bitmap?): Int {
        val texture = IntArray(1)
        if (bmp != null && !bmp.isRecycled) {
            // Generate texture
            GLES20.glGenTextures(1, texture, 0)
            // Generate texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
            // Set the reduction filter to use the color of the pixel closest to the coordinates in the texture as the pixel color to be drawn
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
            // Set the magnification filter to use the closest color of the coordinates in the texture, through the weighted average algorithm to get the pixel color to be drawn
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            // Set the surrounding direction S, intercept texture coordinates to [1/2n, 1-1/2n]. Will lead to never merge with border
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            //Set the wrap direction T and intercept the texture coordinates to [1/2n, 1-1/2n]. Will lead to never merge with border
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            // Generate a 2D texture according to the parameters specified above
            eglEnvironment.eglContext
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            return texture[0]
        }
        return 0
    }

    companion object {
        const val TAG = "GLES20BackEnv"
    }

    init {
        eglEnvironment.eglInit(mWidth, mHeight)
    }
}