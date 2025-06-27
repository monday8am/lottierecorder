package com.monday8am.lottierecorder.recording

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.util.UnstableApi
import com.monday8am.lottierecorder.lottie.LottieFrameFactory
import com.monday8am.lottierecorder.lottie.LottieScene
import com.monday8am.lottierecorder.model.Scene
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

/**
 * A simplified version of RenderVideoUseCase that returns the progress flow of rendering video operation
 */
internal class RenderVideoUseCase(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal sealed class Result {
        data class Success(val uri: String, val fileSize: Long) : Result()
        data object Loading : Result()
        data class Rendering(val progress: Float) : Result()
        data class Error(val error: String) : Result()
    }

    @UnstableApi
    fun execute(outputPath: String): Flow<Result> {
        return callbackFlow {
            trySend(Result.Loading)

            // Create a HandlerThread if the calling thread has no looper
            val handlerThread = HandlerThread("RecordingThread").apply { start() }
            val handler = Handler(handlerThread.looper)

            // Create a simple scene for demo purposes
            val scenes = listOf<Scene>(Scene.End)
            val lottieScenes = listOf<LottieScene>()
            
            // Use a placeholder audio file
            val audioFile = File(context.cacheDir, "placeholder.aac")

            recordLottieToVideo(
                context = context,
                lottieFrameFactory = LottieFrameFactory(lottieScenes),
                audioUri = audioFile.path,
                outputFilePath = outputPath,
                videoWidth = VIDEO_WIDTH_PX,
                videoHeight = VIDEO_HEIGHT_PX,
                handler = handler,
                onProgress = { trySend(Result.Rendering(it)) },
                onSuccess = { size -> trySend(Result.Success(outputPath, size)) },
                onError = { trySend(Result.Error(it.message ?: "")) },
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
                emit(Result.Error(it.message ?: "Recording failed!"))
            }
            .flowOn(defaultDispatcher)
    }
}

internal const val VIDEO_WIDTH_PX = 1920
internal const val VIDEO_HEIGHT_PX = 1080