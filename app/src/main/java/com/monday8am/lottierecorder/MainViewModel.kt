package com.monday8am.lottierecorder

import android.app.Application
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.monday8am.lottierecorder.lottie.LottieScene
import com.monday8am.lottierecorder.lottie.LottieSceneImpl
import com.monday8am.lottierecorder.recording.RecordingResult
import com.monday8am.lottierecorder.recording.RenderVideoUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val renderVideoUseCase = RenderVideoUseCase(application.applicationContext)
    private val audioUri = "android.resource://${application.packageName}/${R.raw.sample_15s}".toUri()
    private val scenesFlow = MutableStateFlow(emptyList<LottieScene>())

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    @UnstableApi
    val uiState: StateFlow<RecordingResult> = scenesFlow
        .flatMapLatest { scenes ->
            renderVideoUseCase.execute(
                lottieScenes = scenes,
                audioUri = audioUri.toString(),
                outputPath = "${application.cacheDir}/output.mp4"
            )
        }
        .stateIn(scope = viewModelScope, started = WhileSubscribed(300L), initialValue = RecordingResult.Idle)

    @OptIn(UnstableApi::class)
    fun recordLottie(lottieIds: List<LottieAnimationId>) {
        val scenes = lottieIds.map {
            LottieSceneImpl(context = application.applicationContext, lottieResourceId = it.value)
        }
        scenesFlow.update { scenes }
    }
}
