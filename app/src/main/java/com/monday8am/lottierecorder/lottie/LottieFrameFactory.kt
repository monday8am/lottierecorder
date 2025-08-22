package com.monday8am.lottierecorder.lottie

import android.util.Log
import androidx.annotation.RestrictTo
import com.airbnb.lottie.LottieDrawable
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY)
class LottieFrameFactory(
    private val lottieScenes: List<LottieScene>,
) {
    val totalFrames: Int
        get() = lottieScenes.fold(0) { acc, e -> acc + e.totalFrames }

    val totalDuration: Float
        get() = lottieScenes.fold(0f) { acc, e -> acc + e.duration }

    val frameRate: Int
        get() = lottieScenes.first().frameRate

    var currentSceneIndex: Int = 0
        private set

    private val startEndFrames: MutableList<Pair<Int, Int>> = mutableListOf()

    @VisibleForTesting
    val scenePerInterval: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()

    init {
        var accumulator = 0
        lottieScenes.forEachIndexed { index, lottieScene ->
            val interval = accumulator to accumulator + lottieScene.totalFrames - 1
            startEndFrames.add(interval)
            accumulator += lottieScene.totalFrames
            scenePerInterval[index] = interval
        }
    }

    fun generateFrame(frameIndex: Int): LottieDrawable {
        val sceneIndex = getLottieSceneIndexFor(frameIndex)
        currentSceneIndex = sceneIndex
        val scene = lottieScenes[sceneIndex]
        return scene.generateFrame(frameIndex - startEndFrames[sceneIndex].first)
    }

    fun release() {
    }

    @VisibleForTesting
    fun getLottieSceneIndexFor(frame: Int): Int {
        return startEndFrames.indexOfFirst { frame >= it.first && frame <= it.second }
    }
}
