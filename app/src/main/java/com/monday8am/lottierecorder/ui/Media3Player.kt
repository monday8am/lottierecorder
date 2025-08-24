package com.monday8am.lottierecorder.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
internal fun Media3Player(
    uri: String,
    onDestroy: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPosition by rememberSaveable { mutableLongStateOf(0L) }
    val player = generatePlayer(uri)
    val playerView =
        rememberPlayerViewWithLifecycle { position, duration ->
            currentPosition = position

            val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            onDestroy(progress)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1.777f),
    ) {
        PlayerContainer(
            playerView = playerView,
            player = player,
            currentPosition = currentPosition,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlayerContainer(
    playerView: PlayerView,
    player: ExoPlayer,
    currentPosition: Long,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { playerView },
    ) { playerAndroidView ->
        playerAndroidView.player = player
        playerAndroidView.player?.seekTo(currentPosition)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun rememberPlayerLifecycleObserver(playerView: PlayerView): LifecycleEventObserver =
    remember(playerView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    playerView.onResume()
                    playerView.player?.play()
                    playerView.hideController()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    playerView.player?.pause()
                    playerView.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> playerView.player?.release()
                else -> {
                    // NOTHING TO DO HERE
                }
            }
        }
    }

@OptIn(UnstableApi::class)
@Composable
private fun rememberPlayerViewWithLifecycle(onDispose: (Long, Long) -> Unit): PlayerView {
    val context = LocalContext.current
    val playerView =
        remember {
            PlayerView(context).apply {
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        }

    val lifecycleObserver = rememberPlayerLifecycleObserver(playerView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            val position = playerView.player?.currentPosition ?: 0
            val duration = playerView.player?.duration ?: 1
            onDispose(position, duration)
            playerView.player?.stop()
            playerView.player?.release()
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return playerView
}

@Composable
private fun generatePlayer(uri: String): ExoPlayer {
    val player = ExoPlayer.Builder(LocalContext.current).build()
    val mediaItem = MediaItem.fromUri(uri)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
    return player
}
