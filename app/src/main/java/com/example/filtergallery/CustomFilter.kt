package com.example.filtergallery

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface

data class CustomFilter(
    var nameOfFilter: String,
    var shaderId: Int
)

data class CustomFilterSurface(
    val nameOfFilter: String,
    var surfaceTexture: SurfaceTexture,
    val viewPortWidth: Int,
    val viewPortHeight: Int
)

fun List<CustomFilterSurface>.toSurface(previewSize: Size?): List<Surface> {
    return this.map {
        it.surfaceTexture.setDefaultBufferSize(
            previewSize?.width ?: 1080,
            previewSize?.height ?: 1920
        )
        Surface(it.surfaceTexture)
    }
}