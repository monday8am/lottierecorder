package com.monday8am.lottierecorder.lottie

import android.util.Size
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable

interface LottieScene {
    val name: String
    val totalFrames: Int
    val duration: Float
    val currentFrame: Int
    val frameRate: Int
    val size: Size
    val hasEnded: Boolean
    val composition: LottieComposition
    fun generateFrame(frameIndex: Int): LottieDrawable
}
