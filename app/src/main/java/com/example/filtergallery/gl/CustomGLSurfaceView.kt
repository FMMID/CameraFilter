package com.example.filtergallery.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.filtergallery.CustomFilter

/**
 * Surface for dynamic render an image from camera
 * **/
class CustomGLSurfaceView constructor(context: Context, attributes: AttributeSet? = null) : GLSurfaceView(context, attributes) {

    private var customCameraRender: CustomCameraRender? = null

    fun changeFilter(customFilter: CustomFilter) {
        this.customCameraRender?.customFilter = customFilter
        this.customCameraRender?.isRefreshFilterNeeded = true
        this.customCameraRender?.isPreviewStarted = false
        this.customCameraRender?.surfaceView = this
    }

    fun getCustomCameraRender() = customCameraRender

    fun setupCameraRender(customCameraRender: CustomCameraRender) {
        if (this.customCameraRender == null) {
            setEGLContextClientVersion(2)
            this.customCameraRender = customCameraRender
            this.customCameraRender?.surfaceView = this
            setRenderer(this.customCameraRender)
        } else {
            this.customCameraRender?.surfaceView = this
        }
    }

    fun updateRender() {
        this.customCameraRender?.isRefreshFilterNeeded = true
        this.customCameraRender?.isPreviewStarted = false
    }

    fun saveImage() {
        customCameraRender?.isSaveImage = true
    }
}