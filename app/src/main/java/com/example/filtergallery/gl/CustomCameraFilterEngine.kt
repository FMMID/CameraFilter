package com.example.filtergallery.gl

import android.content.Context
import android.opengl.GLES20
import com.example.filtergallery.CustomFilter
import com.example.filtergallery.R
import com.example.filtergallery.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Utils for download shader data for Camera Rendering**/
class CustomCameraFilterEngine(OESTextureId: Int, context: Context?, private val customFilter: CustomFilter) {

    private var сontext: Context? = context
    private var buffer: FloatBuffer? = null
    private var vertexShader = -1
    private var fragmentShader = -1

    private var shaderProgram = -1

    private val vertexData = floatArrayOf(
        1f, 1f, 1f, 1f,
        -1f, 1f, 0f, 1f,
        -1f, -1f, 0f, 0f,
        1f, 1f, 1f, 1f,
        -1f, -1f, 0f, 0f,
        1f, -1f, 1f, 0f
    )

    init {
        buffer = createBuffer(vertexData)
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
            сontext?.let { Utils.readShaderFromResource(it, R.raw.default_vertex_shader) })
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
            сontext?.let { Utils.readShaderFromResource(it, customFilter.shaderId) })
        shaderProgram = linkProgram(vertexShader, fragmentShader)
    }

    companion object {
        const val POSITION_ATTRIBUTE = "aPosition"
        const val TEXTURE_COORD_ATTRIBUTE = "aTextureCoordinate"
        const val TEXTURE_MATRIX_UNIFORM = "uTextureMatrix"
        const val TEXTURE_SAMPLER_UNIFORM = "uTextureSampler"
    }

    private fun createBuffer(vertexData: FloatArray): FloatBuffer? {
        val buffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertexData, 0, vertexData.size).position(0)
        return buffer
    }

    private fun loadShader(type: Int, shaderSource: String?): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Create Shader Failed!" + GLES20.glGetError())
        }
        GLES20.glShaderSource(shader, shaderSource)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun linkProgram(verShader: Int, fragShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            throw java.lang.RuntimeException("Create Program Failed!" + GLES20.glGetError())
        }
        GLES20.glAttachShader(program, verShader)
        GLES20.glAttachShader(program, fragShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)
        return program
    }

    fun getShaderProgram(): Int {
        return shaderProgram
    }

    fun getBuffer(): FloatBuffer? {
        return buffer
    }
}