package com.example.filtergallery

import android.Manifest
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Build
import android.util.Size
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import javax.microedition.khronos.opengles.GL10

object Utils {

    enum class TypeOfShader {
        VERTEX_SHADER,
        FRAGMENT_SHADER
    }

    fun createOESTextureObject(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }

    fun readShaderFromResource(context: Context, resourceId: Int): String {
        return readString(context, resourceId)
    }

    fun readShaderFromResourceForRenderImage(context: Context, resourceId: Int, type: TypeOfShader): String {
        var shader = readString(context, resourceId)
        return when (type) {
            TypeOfShader.VERTEX_SHADER -> shader
            TypeOfShader.FRAGMENT_SHADER -> {
                shader = shader.replace("#extension GL_OES_EGL_image_external : require\n", "")
                shader = shader.replace("samplerExternalOES", "sampler2D")
                shader
            }
        }
    }

    fun loadBitmap(resources: Resources, resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, resourceId)
    }

    fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size? {
        val sizeList: MutableList<Size> = ArrayList()
        sizeMap.forEach { option ->
            when {
                width > height && option.width > width && option.height > height -> {
                    sizeList.add(option)
                }
                option.width > height && option.height > width -> {
                    sizeList.add(option)
                }
            }
        }
        return if (sizeList.size > 0) {
            Collections.min(sizeList) { lhs, rhs -> java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong()) }
        } else sizeMap[0]
    }

    private fun readString(context: Context, resourceId: Int): String {
        val builder = StringBuilder()
        var inputStream: InputStream? = null
        var inputStreamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = context.resources.openRawResource(resourceId)
            inputStreamReader = InputStreamReader(inputStream)
            bufferedReader = BufferedReader(inputStreamReader)
            bufferedReader.readLines().forEach { line ->
                builder.append(line + "\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            inputStreamReader?.close()
            bufferedReader?.close()
        }
        return builder.toString()
    }
}

object PERMISSION {
    const val REQUEST_CODE_PERMISSIONS = 10
    val REQUIRED_PERMISSIONS =
        mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
}