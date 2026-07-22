package com.gravo.grabadora.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** Reproductor del detalle: ExoPlayer con velocidad variable y seek exacto (afinado ±0,1 s). */
class PlayerController(context: Context) {
    val speedSteps = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setSeekParameters(SeekParameters.EXACT)
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                }
            }
        })
        // ticker de 100 ms para el display MM:SS,d
        scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0)
                delay(100)
            }
        }
    }

    fun load(file: File) {
        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
        _positionMs.value = player.currentPosition.coerceAtLeast(0)
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun stepSpeed(direction: Int) {
        val idx = speedSteps.indexOfFirst { it == _speed.value }.coerceAtLeast(0)
        val next = speedSteps[(idx + direction).coerceIn(0, speedSteps.size - 1)]
        _speed.value = next
        player.playbackParameters = PlaybackParameters(next)
    }

    fun pause() = player.pause()

    fun release() {
        scope.cancel()
        player.release()
    }
}
