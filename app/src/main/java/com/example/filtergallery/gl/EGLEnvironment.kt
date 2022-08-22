package com.example.filtergallery.gl

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

/**
 * It is necessary to create a GL context in the background for rendering demo images in the filter gallery.
 * Usually, for rendering images, you need a GL context that is created when any GL surface appears on the user's screen.
 * **/
class EGLEnvironment {

    var egl: EGL10? = null
    var eglDisplay: EGLDisplay? = null
    var eglConfig: EGLConfig? = null
    var eglSurface: EGLSurface? = null
    var eglContext: EGLContext? = null
    var gl: GL10? = null

    private var surfaceType = SURFACE_PBUFFER
    private var surfaceNativeObj: Any? = null
    private var red = 8
    private var green = 8
    private var blue = 8
    private var alpha = 8
    private var depth = 16
    private var renderType = 4
    private val bufferType = EGL10.EGL_SINGLE_BUFFER
    private val shareContext = EGL10.EGL_NO_CONTEXT

    fun config(red: Int, green: Int, blue: Int, alpha: Int, depth: Int, renderType: Int) {
        this.red = red
        this.green = green
        this.blue = blue
        this.alpha = alpha
        this.depth = depth
        this.renderType = renderType
    }

    fun setSurfaceType(type: Int, vararg obj: Any?) {
        surfaceType = type
        surfaceNativeObj = obj[0]
    }

    fun eglInit(width: Int, height: Int): GLError {
        val attributes = intArrayOf(
            EGL10.EGL_RED_SIZE, red,  // specify the R size (bits) in RGB
            EGL10.EGL_GREEN_SIZE, green,  // specify the G size
            EGL10.EGL_BLUE_SIZE, blue,  // specify B size
            EGL10.EGL_ALPHA_SIZE, alpha,  // specify the alpha size, the above four items actually specify the pixel format
            EGL10.EGL_DEPTH_SIZE, depth,  //Specify the depth of the Z Buffer
            EGL10.EGL_RENDERABLE_TYPE, renderType,  //Specify the render api version, EGL14.EGL_OPENGL_ES2_BIT
            EGL10.EGL_NONE
        ) //Always ends with EGL10.EGL_NONE

        // Get Display
        egl = EGLContext.getEGL() as EGL10
        eglDisplay = egl?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2) //Main version number and minor version number
        egl?.eglInitialize(eglDisplay, version)
        // Select Config
        val configNum = IntArray(1)
        egl?.eglChooseConfig(eglDisplay, attributes, null, 0, configNum)
        if (configNum[0] == 0) return GLError.ConfigErr
        val c = arrayOfNulls<EGLConfig>(configNum[0])
        egl?.eglChooseConfig(eglDisplay, attributes, c, configNum[0], configNum)
        eglConfig = c[0]
        //Create Surface
        val surAttr = intArrayOf(
            EGL10.EGL_WIDTH, width,
            EGL10.EGL_HEIGHT, height,
            EGL10.EGL_NONE
        )
        eglSurface = createSurface(surAttr)
        // Create a Context
        val contextAttr = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        eglContext = egl?.eglCreateContext(eglDisplay, eglConfig, shareContext, contextAttr)
        makeCurrent()
        return GLError.OK
    }

    fun destroy() {
        egl?.eglMakeCurrent(
            eglDisplay, EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT
        )
        egl?.eglDestroySurface(eglDisplay, eglSurface)
        egl?.eglDestroyContext(eglDisplay, eglContext)
        egl?.eglTerminate(eglDisplay)
    }

    private fun makeCurrent() {
        egl?.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        gl = eglContext?.gl as GL10
    }

    private fun createSurface(attr: IntArray): EGLSurface? {
        return when (surfaceType) {
            SURFACE_WINDOW -> egl?.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceNativeObj, attr)
            SURFACE_PIM -> egl?.eglCreatePixmapSurface(eglDisplay, eglConfig, surfaceNativeObj, attr)
            else -> egl?.eglCreatePbufferSurface(eglDisplay, eglConfig, attr)
        }
    }

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        const val SURFACE_PBUFFER = 1
        const val SURFACE_PIM = 2
        const val SURFACE_WINDOW = 3
    }
}

sealed class GLError {
    object OK : GLError()
    object ConfigErr : GLError()
}