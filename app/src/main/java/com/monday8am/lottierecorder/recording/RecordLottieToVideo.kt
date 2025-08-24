package com.monday8am.lottierecorder.recording

import android.content.Context
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.OnInputFrameProcessedListener
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DebugTraceUtil
import androidx.media3.effect.DefaultGlObjectsProvider
import androidx.media3.effect.DefaultVideoFrameProcessor
import androidx.media3.transformer.AssetLoader
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.RawAssetLoader
import androidx.media3.transformer.Transformer
import com.monday8am.lottierecorder.lottie.LottieFrameFactory
import com.monday8am.lottierecorder.recording.gl.FlipEffect
import com.monday8am.lottierecorder.recording.gl.createOpenGlObjects
import com.monday8am.lottierecorder.recording.gl.uploadImageToGLTexture
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
internal suspend fun recordLottieToVideo(
    context: Context,
    lottieFrameFactory: LottieFrameFactory,
    audioInput: AudioInput,
    outputFilePath: String,
    videoWidth: Int,
    videoHeight: Int,
    handler: Handler,
    onProgress: (Float) -> Unit,
    onSuccess: (Long) -> Unit,
    onError: (Exception) -> Unit,
    onRelease: () -> Unit,
) = withContext(handler.asCoroutineDispatcher("RenderingDispatcher")) {
    DebugTraceUtil.enableTracing = true
    val frameRate = lottieFrameFactory.frameRate
    val durationUs = (lottieFrameFactory.totalFrames.toFloat() / frameRate.toFloat() * 1000L * 1000L).toLong()
    val rectVideoSize = Rect(0, 0, videoWidth, videoHeight)
    val paint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
    }

    val assetLoaderDeferred = CompletableDeferred<RawAssetLoader>()
    var awaitReadyForInput: CompletableDeferred<Unit>? = null
    var awaitForLastImage: CompletableDeferred<Image>? = null
    var awaitForAudioChunk: CompletableDeferred<Pair<ByteBuffer, Long>>? = null

    val transformerListener: Transformer.Listener =
        object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                onSuccess(result.fileSizeBytes)
                onRelease()
            }

            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                onError(exception)
                onRelease()
            }
        }

    val audioFormat = Format.Builder()
        .setSampleMimeType(MimeTypes.AUDIO_RAW)
        .setChannelCount(2)
        .setSampleRate(44100)
        .setPcmEncoding(C.ENCODING_PCM_16BIT)
        .build()

    val videoFormat = Format.Builder()
        .setWidth(videoWidth)
        .setHeight(videoHeight)
        .build()

    // Create OpenGL context
    val eglContext = createOpenGlObjects()
    val videoFrameProcessorFactory = DefaultVideoFrameProcessor.Factory.Builder()
        .setGlObjectsProvider(DefaultGlObjectsProvider(eglContext))
        .build()

    // Initialize the Transformer
    val transformer = Transformer.Builder(context)
        .addListener(transformerListener)
        .setVideoFrameProcessorFactory(videoFrameProcessorFactory)
        .setAssetLoaderFactory(
            RawAssetLoaderFactory(
                audioFormat = audioFormat,
                videoFormat = videoFormat,
                onFrameProcessed = {
                    awaitReadyForInput?.complete(Unit)
                },
                onRawAssetLoaderCreated = { rawAssetLoader ->
                    assetLoaderDeferred.complete(rawAssetLoader)
                },
            ),
        )
        .build()

    // Start the Transformer with the raw surface / audio composition
    val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.EMPTY))
        .setDurationUs(durationUs)
        .setEffects(Effects(/* audioProcessors = */ listOf(), /* videoEffects = */ listOf(FlipEffect())))
        .build()
    transformer.start(editedMediaItem, outputFilePath)

    val rawAssetLoader = assetLoaderDeferred.await()

    // Queues data to the asset loader, retrying until successful.
    fun safeQueue(action: () -> Boolean) {
        var result = false
        while (!result) {
            result = action()
        }
    }

    // Launch coroutine to render lottie frames
    launch {
        // Use an ImageReader to render bitmaps onto an input Surface
        val imageReader = ImageReader.newInstance(videoWidth, videoHeight, PixelFormat.RGBA_8888, 10)
        val imageReaderListener = ImageReader.OnImageAvailableListener { imageReader ->
            awaitForLastImage?.complete(imageReader.acquireLatestImage())
        }
        imageReader.setOnImageAvailableListener(imageReaderListener, handler)

        repeat(lottieFrameFactory.totalFrames) { frameIndex ->
            awaitReadyForInput = CompletableDeferred()
            awaitForLastImage = CompletableDeferred()

            lottieFrameFactory.generateFrame(frameIndex).let { lottieFrame ->
                val presentationTime = frameIndex * C.MICROS_PER_SECOND / frameRate

                // Draw lottie on imageReader surface using hardware canvas
                val hCanvas = imageReader.surface.lockHardwareCanvas()
                try {
                    hCanvas.drawRect(Rect(0,0, hCanvas.width, hCanvas.height), paint)
                    // Fit lottie inside Canvas
                    val bounds = calculateFitInsideBounds(
                        contentWidth = lottieFrame.minimumWidth,
                        contentHeight = lottieFrame.minimumHeight,
                        canvasWidth = hCanvas.width,
                        canvasHeight = hCanvas.height
                    )
                    lottieFrame.bounds = bounds
                    lottieFrame.draw(hCanvas)
                } finally {
                    imageReader.surface.unlockCanvasAndPost(hCanvas)
                }

                // get the image from imageReader and queue it to imageWriter
                val image = awaitForLastImage.await()
                val textureId = uploadImageToGLTexture(image)

                safeQueue { rawAssetLoader.queueInputTexture(textureId, presentationTime) }
            }
            onProgress(frameIndex.toFloat() / lottieFrameFactory.totalFrames)
            awaitReadyForInput.await()
        }
        rawAssetLoader.signalEndOfVideoInput()
    }

    // Launch coroutine in a separated thread to render audio frames
    launch(Dispatchers.Default) {
        var lastPresentationTime: Long
        var endOfStream = false
        val audioDecoder = AudioDecoder(
            context = context,
            audioInput = audioInput,
            callback = object : AudioDecoder.AudioCallback {
                override fun onAudioDecoded(buffer: ByteBuffer, size: Int, presentationTimeUs: Long) {
                    awaitForAudioChunk?.complete(Pair(buffer, presentationTimeUs))
                }

                override fun onEndOfStream() {
                    var result = false
                    while (result.not()) {
                        result = rawAssetLoader.queueAudioData(ByteBuffer.allocate(0), durationUs, true)
                    }
                    endOfStream = true
                }

                override fun onError(e: Exception) {
                    endOfStream = true
                    onError(e)
                }
            },
        )
        while (endOfStream.not()) {
            awaitForAudioChunk = CompletableDeferred()
            audioDecoder.decodeNextChunk()
            val (audioChunk, presentationTimeUs) = awaitForAudioChunk.await()

            lastPresentationTime = presentationTimeUs
            val isLast = lastPresentationTime >= durationUs
            endOfStream = isLast
            safeQueue { rawAssetLoader.queueAudioData(audioChunk, presentationTimeUs, isLast) }
        }

        audioDecoder.release()
    }
}

@UnstableApi
private class RawAssetLoaderFactory(
    private val audioFormat: Format?,
    private val videoFormat: Format?,
    private val onFrameProcessed: () -> Unit,
    private val onRawAssetLoaderCreated: (RawAssetLoader) -> Unit,
) : AssetLoader.Factory {

    override fun createAssetLoader(
        editedMediaItem: EditedMediaItem,
        looper: Looper,
        listener: AssetLoader.Listener,
        compositionSettings: AssetLoader.CompositionSettings,
    ): RawAssetLoader {
        val frameProcessedListener = OnInputFrameProcessedListener { texId, syncObject ->
            try {
                GlUtil.deleteTexture(texId)
                GlUtil.deleteSyncObject(syncObject)
                onFrameProcessed()
            } catch (e: GlUtil.GlException) {
                throw VideoFrameProcessingException(e)
            }
        }
        val rawAssetLoader = RawAssetLoader(
            editedMediaItem,
            listener,
            audioFormat,
            videoFormat,
            frameProcessedListener,
        )
        onRawAssetLoaderCreated(rawAssetLoader)
        return rawAssetLoader
    }
}
