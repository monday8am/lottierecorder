package com.monday8am.lottierecorder.recording.gl

import android.media.Image
import android.opengl.EGLContext
import android.opengl.GLES20
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DefaultGlObjectsProvider
import java.nio.ByteBuffer

@UnstableApi
internal fun createOpenGlObjects(): EGLContext {
    val eglDisplay = GlUtil.getDefaultEglDisplay()
    val glObjectsProvider = DefaultGlObjectsProvider(null)
    val eglContext = glObjectsProvider.createEglContext(eglDisplay, 2, GlUtil.EGL_CONFIG_ATTRIBUTES_RGBA_8888)
    glObjectsProvider.createFocusedPlaceholderEglSurface(eglContext, eglDisplay)
    return eglContext
}

@UnstableApi
internal fun uploadImageToGLTexture(image: Image): Int {
    val textureId = IntArray(1)
    GLES20.glGenTextures(1, textureId, 0) // Generate OpenGL texture
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]) // Bind texture

    // Set texture parameters for smooth scaling
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

    // Extract pixel buffer
    val buffer = extractPixelsFromImage(image)
    val width = image.width
    val height = image.height

    // Upload pixel data to OpenGL texture
    GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer,
    )

    // Unbind texture
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

    image.close() // Important: release the image!
    return textureId[0] // Return the OpenGL texture ID
}

private fun extractPixelsFromImage(image: Image): ByteBuffer {
    val plane = image.planes[0] // Assume RGBA_8888 format (one plane)
    val buffer = plane.buffer
    return buffer
}
