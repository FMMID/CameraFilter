package com.example.filtergallery.gl

import android.content.Context

class SimpleFilter(
    context: Context,
    private val vertexRes: Int,
    private val fragmentRes: Int
) : GeneralFilter(context) {

    override fun onCreate() {
        createProgramByAssetsFile(vertexRes, fragmentRes)
    }

    override fun onSizeChanged(width: Int, height: Int) {}
}