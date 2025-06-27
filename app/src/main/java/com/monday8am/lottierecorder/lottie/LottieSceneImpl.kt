package com.monday8am.lottierecorder.lottie

import android.util.Size
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.monday8am.lottierecorder.model.Scene

internal class LottieSceneImpl(
    private val scene: Scene,
) : LottieScene {

    private val durationInFrames: Int = 0
    private val lottieDrawable: LottieDrawable = LottieDrawable()

    override val name: String
        get() = scene.javaClass.simpleName

    override val totalFrames: Int
        get() = lottieDrawable.composition.durationFrames.toInt()

    override val duration: Float
        get() = lottieDrawable.composition.duration

    override var currentFrame: Int = 0
        private set

    override val frameRate: Int
        get() = lottieDrawable.composition.frameRate.toInt()

    override val size: Size
        get() = Size(lottieDrawable.composition.bounds.width(), lottieDrawable.composition.bounds.height())

    override val hasEnded: Boolean
        get() = currentFrame > durationInFrames

    override var hasLoadedBitmaps: Boolean = false
        private set

    override lateinit var composition: LottieComposition
        private set

    init {
        // load lottie here?
    }

    override fun generateFrame(frameIndex: Int): LottieDrawable {
        lottieDrawable.frame = frameIndex
        currentFrame = frameIndex
        return lottieDrawable
    }
}
