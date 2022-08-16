package com.example.filtergallery.gl

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.example.filtergallery.Utils
import com.example.filtergallery.gl.CustomCameraFilterEngine.Companion.POSITION_ATTRIBUTE
import com.example.filtergallery.gl.CustomCameraFilterEngine.Companion.TEXTURE_COORD_ATTRIBUTE
import com.example.filtergallery.gl.CustomCameraFilterEngine.Companion.TEXTURE_MATRIX_UNIFORM
import com.example.filtergallery.gl.CustomCameraFilterEngine.Companion.TEXTURE_SAMPLER_UNIFORM
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Class for prepare filter fragment and vertex shaders for rendering static images in background (EGL surface)
 * **/
abstract class GeneralFilter(protected var context: Context) {

    private var mProgram = 0
    private var hPosition = 0
    private var hCoord = 0
    private var hMatrix = 0
    private var hTexture = 0
    private var verBuffer: FloatBuffer? = null
    private var texBuffer: FloatBuffer? = null

    var matrix: FloatArray = originalMatrix.copyOf(16)
    var textureType = 0
    var textureId = 0

    private val pos = floatArrayOf(
        -1.0f, 1.0f,
        -1.0f, -1.0f,
        1.0f, 1.0f,
        1.0f, -1.0f
    )

    private val coord = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    fun create() {
        onCreate()
    }

    fun setSize(width: Int, height: Int) {
        onSizeChanged(width, height)
    }

    fun draw() {
        onClear()
        onUseProgram()
        onSetExpandData()
        onBindTexture()
        onDraw()
    }

    protected abstract fun onCreate()
    protected abstract fun onSizeChanged(width: Int, height: Int)
    protected fun createProgram(vertex: String?, fragment: String?) {
        mProgram = uCreateGlProgram(vertex, fragment)
        hPosition = GLES20.glGetAttribLocation(mProgram, POSITION_ATTRIBUTE)
        hCoord = GLES20.glGetAttribLocation(mProgram, TEXTURE_COORD_ATTRIBUTE)
        hMatrix = GLES20.glGetUniformLocation(mProgram, TEXTURE_MATRIX_UNIFORM)
        hTexture = GLES20.glGetUniformLocation(mProgram, TEXTURE_SAMPLER_UNIFORM)
    }

    protected fun createProgramByAssetsFile(vertexId: Int, fragmentId: Int) {
        val vertexShader = Utils.readShaderFromResourceForRenderImage(context, vertexId, Utils.TypeOfShader.VERTEX_SHADER)
        val fragmentShader = Utils.readShaderFromResourceForRenderImage(context, fragmentId, Utils.TypeOfShader.FRAGMENT_SHADER)
        createProgram(vertexShader, fragmentShader)
    }

    private fun initBuffer() {
        val a = ByteBuffer.allocateDirect(32)
        a.order(ByteOrder.nativeOrder())
        verBuffer = a.asFloatBuffer()
        verBuffer?.put(pos)
        verBuffer?.position(0)
        val b = ByteBuffer.allocateDirect(32)
        b.order(ByteOrder.nativeOrder())
        texBuffer = b.asFloatBuffer()
        texBuffer?.put(coord)
        texBuffer?.position(0)
    }

    private fun onUseProgram() {
        GLES20.glUseProgram(mProgram)
    }

    private fun onDraw() {
        GLES20.glEnableVertexAttribArray(hPosition)
        GLES20.glVertexAttribPointer(hPosition, 2, GLES20.GL_FLOAT, false, 0, verBuffer)
        GLES20.glEnableVertexAttribArray(hCoord)
        GLES20.glVertexAttribPointer(hCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(hPosition)
        GLES20.glDisableVertexAttribArray(hCoord)
    }

    private fun onClear() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    private fun onSetExpandData() {
        GLES20.glUniformMatrix4fv(hMatrix, 1, false, matrix, 0)
    }

    private fun onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureType)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(hTexture, textureType)
    }

    companion object {
        private const val TAG = "Filter"
        var DEBUG = true
        val originalMatrix = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

        private fun glError(code: Int, index: Any) {
            if (DEBUG && code != 0) {
                Log.e(TAG, "glError:$code---$index")
            }
        }

        fun uCreateGlProgram(vertexSource: String?, fragmentSource: String?): Int {
            val vertex = uLoadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            if (vertex == 0) return 0
            val fragment = uLoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (fragment == 0) return 0
            var program = GLES20.glCreateProgram()
            if (program != 0) {
                GLES20.glAttachShader(program, vertex)
                GLES20.glAttachShader(program, fragment)
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    glError(1, "Could not link program:" + GLES20.glGetProgramInfoLog(program))
                    GLES20.glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }

        private fun uLoadShader(shaderType: Int, source: String?): Int {
            var shader = GLES20.glCreateShader(shaderType)
            if (0 != shader) {
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    glError(1, "Could not compile shader:$shaderType")
                    glError(1, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader))
                    GLES20.glDeleteShader(shader)
                    shader = 0
                }
            }
            return shader
        }
    }

    init {
        initBuffer()
    }
}