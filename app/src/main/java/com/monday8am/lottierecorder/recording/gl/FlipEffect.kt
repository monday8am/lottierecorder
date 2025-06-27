package com.monday8am.lottierecorder.recording.gl

import android.opengl.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlMatrixTransformation

@UnstableApi
class FlipEffect : GlMatrixTransformation {

    override fun getGlMatrixArray(presentationTimeUs: Long): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)

        Matrix.rotateM(matrix, 0, 180f, 0f, 0f, 1f)

        // Apply horizontal mirroring by flipping along the X-axis
        Matrix.scaleM(matrix, 0, -1f, 1f, 1f)
        return matrix
    }
}
