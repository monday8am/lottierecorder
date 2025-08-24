package com.monday8am.lottierecorder.recording

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.annotation.RawRes
import java.io.IOException
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.Queue

sealed class AudioInput {
    data class RawResource(
        @param:RawRes val resourceId: Int,
    ) : AudioInput()

    data class FileUri(
        val uri: String,
    ) : AudioInput()
}

/**
 * Decodes audio from a given URI and provides decoded audio chunks to a callback.
 */
internal class AudioDecoder(
    context: Context,
    audioInput: AudioInput,
    private val callback: AudioCallback,
) {
    companion object {
        private const val PRELOAD_SIZE = 5 // Number of chunks to pre-decode
    }

    interface AudioCallback {
        fun onAudioDecoded(
            buffer: ByteBuffer,
            size: Int,
            presentationTimeUs: Long,
        )

        fun onEndOfStream()

        fun onError(e: Exception)
    }

    private val extractor: MediaExtractor = MediaExtractor()
    private val decoder: MediaCodec
    private val bufferQueue: Queue<AudioChunk> = LinkedList()
    private var isEOS = false

    private data class AudioChunk(
        val buffer: ByteBuffer,
        val size: Int,
        val presentationTimeUs: Long,
    )

    init {
        try {
            when (audioInput) {
                is AudioInput.RawResource -> extractor.setDataSourceFromRaw(context = context, resId = audioInput.resourceId)
                is AudioInput.FileUri -> extractor.setDataSource(audioInput.uri)
            }

            var format: MediaFormat? = null
            var mime: String? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val trackMime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (trackMime?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)
                    format = trackFormat
                    mime = trackMime
                    break
                }
            }

            if (format == null || mime == null) {
                throw IOException("No suitable audio track found.")
            }

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            // Preload a few chunks in advance
            preloadChunks()
        } catch (e: IOException) {
            callback.onError(e)
            throw e
        }
    }

    /**
     * Fetches a decoded chunk from the preloaded buffer queue.
     */
    internal fun decodeNextChunk() {
        if (bufferQueue.isNotEmpty()) {
            val chunk = bufferQueue.poll()
            chunk?.let {
                callback.onAudioDecoded(it.buffer, it.size, it.presentationTimeUs)
            }
        } else if (!isEOS) {
            preloadChunks()
            decodeNextChunk() // Try again after preloading
        } else {
            callback.onEndOfStream()
        }
    }

    /**
     * Decodes multiple chunks in advance and stores them in the buffer queue.
     */
    private fun preloadChunks() {
        while (bufferQueue.size < PRELOAD_SIZE && !isEOS) {
            val bufferInfo = MediaCodec.BufferInfo()

            val inputIndex = decoder.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = decoder.getInputBuffer(inputIndex)
                val size = extractor.readSampleData(inputBuffer!!, 0)

                if (size < 0) {
                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    decoder.queueInputBuffer(inputIndex, 0, size, presentationTimeUs, 0)
                    extractor.advance()
                }
            }

            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputIndex)
                if (bufferInfo.size > 0 && outputBuffer != null) {
                    val copiedBuffer =
                        ByteBuffer.allocate(bufferInfo.size).apply {
                            put(outputBuffer)
                            flip()
                        }
                    bufferQueue.add(AudioChunk(copiedBuffer, bufferInfo.size, bufferInfo.presentationTimeUs))
                }
                decoder.releaseOutputBuffer(outputIndex, false)
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                isEOS = true
            }
        }
    }

    fun release() {
        decoder.stop()
        decoder.release()
        extractor.release()
        bufferQueue.clear()
    }
}

private fun MediaExtractor.setDataSourceFromRaw(
    context: Context,
    @RawRes resId: Int,
) {
    val afd =
        context.resources.openRawResourceFd(resId)
            ?: throw IllegalStateException("Resource is compressed or missing")
    afd.use {
        setDataSource(it.fileDescriptor, it.startOffset, it.length)
        it.close()
    }
}
