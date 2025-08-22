package com.monday8am.lottierecorder.recording

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.util.UnstableApi
import com.monday8am.lottierecorder.lottie.LottieFrameFactory
import com.monday8am.lottierecorder.lottie.LottieScene
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

sealed class RecordingResult {
    data class Success(val uri: String, val fileSize: Long) : RecordingResult()
    data object Idle : RecordingResult()
    data class Rendering(val progress: Float) : RecordingResult()
    data class Error(val error: String) : RecordingResult()
}

/**
 * A simplified version of RenderVideoUseCase that returns the progress flow of rendering video operation
 */
internal class RenderVideoUseCase(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    @UnstableApi
    fun execute(
        lottieScenes: List<LottieScene>,
        audioUri: String,
        outputPath: String,
    ): Flow<RecordingResult> {
        return callbackFlow {
            trySend(RecordingResult.Idle)

            // Create a HandlerThread if the calling thread has no looper
            val handlerThread = HandlerThread("RecordingThread").apply { start() }
            val handler = Handler(handlerThread.looper)

            recordLottieToVideo(
                context = context,
                lottieFrameFactory = LottieFrameFactory(lottieScenes),
                audioUri = audioUri,
                outputFilePath = outputPath,
                videoWidth = VIDEO_WIDTH_PX,
                videoHeight = VIDEO_HEIGHT_PX,
                handler = handler,
                onProgress = { trySend(RecordingResult.Rendering(it)) },
                onSuccess = { size -> trySend(RecordingResult.Success(outputPath, size)) },
                onError = { trySend(RecordingResult.Error(it.message ?: "")) },
                onRelease = {
                    handlerThread.quitSafely()
                    close()
                },
            )

            awaitClose {
                handlerThread.quitSafely()
                close()
            }
        }
            .catch {
                emit(RecordingResult.Error(it.message ?: "Recording failed!"))
            }
            .flowOn(defaultDispatcher)
    }
}

internal const val VIDEO_WIDTH_PX = 1920
internal const val VIDEO_HEIGHT_PX = 1080