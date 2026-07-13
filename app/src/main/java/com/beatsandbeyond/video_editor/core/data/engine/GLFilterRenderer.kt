package com.beatsandbeyond.video_editor.core.data.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.os.Handler
import android.os.HandlerThread
import com.beatsandbeyond.video_editor.core.utils.Logger

/**
 * Helper class that sets up an EGL context and runs a GLSL shader pass
 * between the video decoder output and the video encoder input surface.
 * Applies brightness, contrast, and saturation color adjustments in real-time.
 */
class GLFilterRenderer(
    private val inputSurface: Surface,
    private val width: Int,
    private val height: Int
) : SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private const val TAG = "GLFilterRenderer"
        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
    }

    private val triangleVerticesData = floatArrayOf(
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
         1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
         1.0f,  1.0f, 0.0f, 1.0f, 1.0f
    )

    private val triangleVertices: FloatBuffer

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        uniform mat4 uSTMatrix;
        void main() {
            gl_Position = aPosition;
            vTextureCoord = (uSTMatrix * aTextureCoord).xy;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision medium float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        uniform float uBrightness;
        uniform float uContrast;
        uniform float uSaturation;
        uniform float uVintage;
        uniform float uVignette;
        uniform float uLut;
        uniform sampler2D uLutTexture;
        void main() {
            vec4 color = texture2D(sTexture, vTextureCoord);
            vec3 rgb = color.rgb * uBrightness;
            rgb = (rgb - 0.5) * uContrast + 0.5;
            float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
            rgb = mix(vec3(luma), rgb, uSaturation);
            
            if (uVintage > 0.0) {
                vec3 sepia = vec3(
                    dot(rgb, vec3(0.393, 0.769, 0.189)),
                    dot(rgb, vec3(0.349, 0.686, 0.168)),
                    dot(rgb, vec3(0.272, 0.534, 0.131))
                );
                rgb = mix(rgb, sepia, uVintage);
            }
            
            if (uVignette > 0.0) {
                vec2 uv = vTextureCoord - 0.5;
                float dist = length(uv);
                float vfactor = smoothstep(0.8 - uVignette * 0.4, 0.4 - uVignette * 0.2, dist);
                rgb *= vfactor;
            }
            
            if (uLut > 0.0) {
                float blueColor = rgb.b * 63.0;
                
                vec2 quad1;
                quad1.y = floor(floor(blueColor) / 8.0);
                quad1.x = floor(blueColor) - (quad1.y * 8.0);
                
                vec2 quad2;
                quad2.y = floor(ceil(blueColor) / 8.0);
                quad2.x = ceil(blueColor) - (quad2.y * 8.0);
                
                vec2 texPos1;
                texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.r);
                texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.g);
                
                vec2 texPos2;
                texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.r);
                texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * rgb.g);
                
                vec3 newColor1 = texture2D(uLutTexture, texPos1).rgb;
                vec3 newColor2 = texture2D(uLutTexture, texPos2).rgb;
                
                vec3 lutColor = mix(newColor1, newColor2, fract(blueColor));
                rgb = mix(rgb, lutColor, uLut);
            }
            
            gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
        }
    """.trimIndent()

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var textureId = -1
    private var program = -1
    private var aPositionHandle = -1
    private var aTextureCoordHandle = -1
    private var uSTMatrixHandle = -1
    private var uBrightnessHandle = -1
    private var uContrastHandle = -1
    private var uSaturationHandle = -1
    private var uVintageHandle = -1
    private var uVignetteHandle = -1
    private var uLutHandle = -1
    private var uLutTextureHandle = -1
    private var lutTextureId = -1

    private val stMatrix = FloatArray(16)
    var surfaceTexture: SurfaceTexture? = null
        private set
    var decoderSurface: Surface? = null
        private set

    private val handlerThread = HandlerThread("GLFilterRendererCallback").apply { start() }
    private val callbackHandler = Handler(handlerThread.looper)
    private val frameSyncObject = Object()
    private var frameAvailable = false

    var brightness: Float = 1.0f
    var contrast: Float = 1.0f
    var saturation: Float = 1.0f
    var vintage: Float = 0.0f
    var vignette: Float = 0.0f
    var lutStrength: Float = 0.0f
    var lutTextureBitmap: android.graphics.Bitmap? = null
    var lutChanged: Boolean = false

    init {
        triangleVertices = ByteBuffer.allocateDirect(triangleVerticesData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        triangleVertices.put(triangleVerticesData).position(0)

        Matrix.setIdentityM(stMatrix, 0)
        eglSetup()
        makeCurrent()
        glSetup()
    }

    private fun eglSetup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL14")
        }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            throw RuntimeException("unable to find RGB888+ES2 EGL config")
        }

        val config = configs[0]
        val attribListCtx = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, attribListCtx, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Failed to create context")
        }

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, inputSurface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Failed to create window surface")
        }
    }

    private fun glSetup() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            program = -1
            throw RuntimeException("Could not link program: $log")
        }

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        uBrightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness")
        uContrastHandle = GLES20.glGetUniformLocation(program, "uContrast")
        uSaturationHandle = GLES20.glGetUniformLocation(program, "uSaturation")
        uVintageHandle = GLES20.glGetUniformLocation(program, "uVintage")
        uVignetteHandle = GLES20.glGetUniformLocation(program, "uVignette")
        uLutHandle = GLES20.glGetUniformLocation(program, "uLut")
        uLutTextureHandle = GLES20.glGetUniformLocation(program, "uLutTexture")

        val textures = IntArray(2)
        GLES20.glGenTextures(2, textures, 0)
        textureId = textures[0]
        lutTextureId = textures[1]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture?.setOnFrameAvailableListener(this, callbackHandler)
        decoderSurface = Surface(surfaceTexture)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $type: $log")
        }
        return shader
    }

    fun makeCurrent() {
        val currentContext = EGL14.eglGetCurrentContext()
        val currentDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val currentReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        if (currentContext == eglContext &&
            currentDrawSurface == eglSurface &&
            currentReadSurface == eglSurface) {
            return
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun drawFrame() {
        makeCurrent()
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(stMatrix)

        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
        )
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            aTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
        )
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glUniform1f(uBrightnessHandle, brightness)
        GLES20.glUniform1f(uContrastHandle, contrast)
        GLES20.glUniform1f(uSaturationHandle, saturation)
        GLES20.glUniform1f(uVintageHandle, vintage)
        GLES20.glUniform1f(uVignetteHandle, vignette)
        GLES20.glUniform1f(uLutHandle, lutStrength)

        if (lutStrength > 0f && lutTextureBitmap != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
            if (lutChanged) {
                android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lutTextureBitmap, 0)
                lutChanged = false
            }
            GLES20.glUniform1i(uLutTextureHandle, 1)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
    }

    private var overlayProgram = -1
    private var overlayAPositionHandle = -1
    private var overlayATextureCoordHandle = -1
    private var overlayTextureId = -1

    private fun initOverlayShader() {
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = aPosition;
                vTextureCoord = aTextureCoord;
            }
        """.trimIndent()

        val fragmentShader = """
            precision medium float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture2D;
            void main() {
                gl_FragColor = texture2D(sTexture2D, vTextureCoord);
            }
        """.trimIndent()

        overlayProgram = createProgram(vertexShader, fragmentShader)
        overlayAPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "aPosition")
        overlayATextureCoordHandle = GLES20.glGetAttribLocation(overlayProgram, "aTextureCoord")
        
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        overlayTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, fragmentShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            return -1
        }
        return prog
    }

    fun drawOverlay(bitmap: android.graphics.Bitmap, x: Float, y: Float, w: Float, h: Float) {
        makeCurrent()
        if (overlayProgram == -1) {
            initOverlayShader()
        }
        
        GLES20.glUseProgram(overlayProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId)
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        
        val x1 = x - w / 2f
        val x2 = x + w / 2f
        val y1 = y - h / 2f
        val y2 = y + h / 2f
        
        val vertices = floatArrayOf(
            x1, y1, 0.0f, 0.0f, 1.0f,
            x2, y1, 0.0f, 1.0f, 1.0f,
            x1, y2, 0.0f, 0.0f, 0.0f,
            x2, y2, 0.0f, 1.0f, 0.0f
        )
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
        
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(overlayAPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer)
        GLES20.glEnableVertexAttribArray(overlayAPositionHandle)
        
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(overlayATextureCoordHandle, 2, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer)
        GLES20.glEnableVertexAttribArray(overlayATextureCoordHandle)
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(overlayAPositionHandle)
        GLES20.glDisableVertexAttribArray(overlayATextureCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun swapBuffers() {
        makeCurrent()
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        synchronized(frameSyncObject) {
            if (frameAvailable) {
                Logger.w(TAG, "Frame already available, resetting")
            }
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    fun awaitFrame(timeoutMs: Long = 2500L) {
        synchronized(frameSyncObject) {
            val endTime = System.currentTimeMillis() + timeoutMs
            while (!frameAvailable) {
                val delay = endTime - System.currentTimeMillis()
                if (delay <= 0) {
                    throw RuntimeException("Timeout waiting for frame-availability signal")
                }
                try {
                    frameSyncObject.wait(delay)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            frameAvailable = false
        }
    }

    fun setPresentationTime(timeNs: Long) {
        android.opengl.EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timeNs)
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE

        surfaceTexture?.release()
        decoderSurface?.release()
        handlerThread.quitSafely()
    }
}
