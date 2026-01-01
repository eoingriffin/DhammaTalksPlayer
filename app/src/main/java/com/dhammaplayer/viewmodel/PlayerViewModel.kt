package com.dhammaplayer.viewmodel

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.repository.DownloadRepository
import com.dhammaplayer.data.repository.TracksRepository
import com.dhammaplayer.media.PlaybackService
import com.dhammaplayer.util.AlbumArtExtractor
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isConnected: Boolean = false,
    val albumArt: Bitmap? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tracksRepository: TracksRepository,
    private val downloadRepository: DownloadRepository,
    private val albumArtExtractor: AlbumArtExtractor
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePosition()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                _currentTrackId.value = item.mediaId
                viewModelScope.launch {
                    val track = tracksRepository.getTrack(item.mediaId)
                    _uiState.value = _uiState.value.copy(currentTrack = track)
                }
            }
        }
    }

    fun connectToService() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            _uiState.value = _uiState.value.copy(isConnected = true)

            // Sync current state
            mediaController?.let { controller ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = controller.isPlaying,
                    currentPosition = controller.currentPosition,
                    duration = controller.duration.coerceAtLeast(0)
                )
                controller.currentMediaItem?.let { item ->
                    _currentTrackId.value = item.mediaId
                    viewModelScope.launch {
                        val track = tracksRepository.getTrack(item.mediaId)
                        _uiState.value = _uiState.value.copy(currentTrack = track)
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    fun playTrack(track: AudioTrack) {
        viewModelScope.launch {
            val audioUri = downloadRepository.getLocalFilePath(track.id) ?: track.audioUrl

            // Fetch saved progress before setting up media
            val savedProgress = tracksRepository.getProgress(track.id)
            val startPosition = if (savedProgress != null && !savedProgress.finished) {
                savedProgress.currentTime
            } else {
                0L
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(audioUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist("Thanissaro Bhikkhu")
                        .build()
                )
                .build()

            mediaController?.let { controller ->
                controller.setMediaItem(mediaItem, startPosition)
                controller.prepare()

                controller.play()
            }

            _currentTrackId.value = track.id
            _uiState.value = _uiState.value.copy(currentTrack = track, albumArt = null)

            // Extract album art in background
            val albumArt = albumArtExtractor.extractAlbumArt(audioUri)
            _uiState.value = _uiState.value.copy(albumArt = albumArt)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun skipForward() {
        mediaController?.let { controller ->
            val newPosition = (controller.currentPosition + 30_000).coerceAtMost(controller.duration)
            controller.seekTo(newPosition)
        }
    }

    fun skipBackward() {
        mediaController?.let { controller ->
            val newPosition = (controller.currentPosition - 10_000).coerceAtLeast(0)
            controller.seekTo(newPosition)
        }
    }

    fun updatePosition() {
        mediaController?.let { controller ->
            _uiState.value = _uiState.value.copy(
                currentPosition = controller.currentPosition,
                duration = controller.duration.coerceAtLeast(0)
            )
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

