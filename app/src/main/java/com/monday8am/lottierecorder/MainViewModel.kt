package com.monday8am.lottierecorder

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.monday8am.lottierecorder.lottie.LottieFrameFactory
import com.monday8am.lottierecorder.lottie.LottieSceneImpl
import com.monday8am.lottierecorder.recording.recordLottieToVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Create a HandlerThread if the calling thread has no looper
    private val handlerThread = HandlerThread("RecordingThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    @OptIn(UnstableApi::class)
    fun recordLottie(lottieIds: List<LottieAnimationId>) {
        val scenes = lottieIds.map { LottieSceneImpl(it.value) }
        val factory = LottieFrameFactory(scenes)

        viewModelScope.launch(Dispatchers.IO) {
            recordLottieToVideo(
                context = application.applicationContext,
                lottieFrameFactory = factory,
                audioUri = "",
                outputFilePath = "output.mp4",
                videoWidth = 1080,
                videoHeight = 1920,
                handler = handler,
                onProgress = {},
                onSuccess = { handlerThread.quit() },
                onError = { handlerThread.quit() },
                onRelease = { handlerThread.quit() },
            )
        }
    }
}
