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
import com.dhammaplayer.data.model.TrackProgress
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
    val currentTrack: AudioTrack? = null,      // Track being viewed in player screen
    val playingTrack: AudioTrack? = null,      // Track actually playing in media controller
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
                    _uiState.value = _uiState.value.copy(
                        currentTrack = track,
                        playingTrack = track
                    )
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
                        _uiState.value = _uiState.value.copy(
                            currentTrack = track,
                            playingTrack = track
                        )
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Selects a track for viewing without affecting playback.
     * Updates the UI to show the selected track's info.
     */
    fun selectTrack(track: AudioTrack) {
        viewModelScope.launch {
            val localFilePath = downloadRepository.getLocalFilePath(track.id)
            val audioUri = localFilePath ?: track.audioUrl
            val isLocalFile = localFilePath != null

            // If this track is currently loaded in the player, sync with player state
            if (_currentTrackId.value == track.id) {
                // Update currentTrack to match playingTrack and sync position from player
                _uiState.value = _uiState.value.copy(
                    currentTrack = track,
                    albumArt = null
                )
                // Sync position from the media controller
                mediaController?.let { controller ->
                    _uiState.value = _uiState.value.copy(
                        currentPosition = controller.currentPosition,
                        duration = controller.duration.coerceAtLeast(0)
                    )
                }
                // For playing track, allow network access for album art
                val albumArt = albumArtExtractor.extractAlbumArtForPlayingTrack(track.id, audioUri)
                _uiState.value = _uiState.value.copy(albumArt = albumArt)
            } else {
                // Viewing a different track - show saved progress
                val savedProgress = tracksRepository.getProgress(track.id)
                val savedPosition = if (savedProgress != null && !savedProgress.finished) {
                    savedProgress.currentTime
                } else {
                    0L
                }
                val savedDuration = savedProgress?.duration ?: 0L

                _uiState.value = _uiState.value.copy(
                    currentTrack = track,
                    albumArt = null,
                    currentPosition = savedPosition,
                    duration = savedDuration
                )

                // Only extract album art from local files to avoid network traffic
                val albumArt = albumArtExtractor.extractAlbumArt(track.id, audioUri, isLocalFile)
                _uiState.value = _uiState.value.copy(albumArt = albumArt)
            }
        }
    }

    /**
     * Plays a track immediately. Use this when the user explicitly presses a play button.
     * If the track is already loaded in the player, just play/resume it.
     * If it's a different track, load it and start playing.
     */
    fun playTrack(track: AudioTrack) {
        viewModelScope.launch {
            val localFilePath = downloadRepository.getLocalFilePath(track.id)
            val audioUri = localFilePath ?: track.audioUrl
            val isStreaming = localFilePath == null

            mediaController?.let { controller ->
                // Check if this track is already loaded in the media controller
                val currentMediaId = controller.currentMediaItem?.mediaId

                if (currentMediaId == track.id) {
                    // Track is already loaded - just play/resume
                    if (!controller.isPlaying) {
                        controller.play()
                    }
                } else {
                    // Different track - load and play it
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

                    controller.setMediaItem(mediaItem, startPosition)
                    controller.prepare()
                    controller.play()

                    // Auto-cache the track if streaming from remote URL
                    if (isStreaming) {
                        launch {
                            downloadRepository.autoCacheTrack(track)
                        }
                    }
                }
            }

            _currentTrackId.value = track.id
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                playingTrack = track,
                albumArt = null
            )

            // Extract album art - allowed for playing track (audio is already streaming)
            val albumArt = albumArtExtractor.extractAlbumArtForPlayingTrack(track.id, audioUri)
            _uiState.value = _uiState.value.copy(albumArt = albumArt)
        }
    }

    fun togglePlayPause() {
        val currentTrack = _uiState.value.currentTrack

        mediaController?.let { controller ->
            val loadedMediaId = controller.currentMediaItem?.mediaId

            // If the viewed track is different from what's loaded, load and play it
            if (currentTrack != null && loadedMediaId != currentTrack.id) {
                playTrack(currentTrack)
                return
            }

            // Same track - just toggle play/pause
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

    /**
     * Updates only the UI position without affecting the media controller.
     * Use this when seeking on a track that's not loaded in the player.
     */
    fun updateViewedPosition(position: Long) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun skipForward() {
        mediaController?.let { controller ->
            val newPosition =
                (controller.currentPosition + 30_000).coerceAtMost(controller.duration)
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

    /**
     * Stops playback, clears the current track, and resets the player state.
     */
    fun stopPlayback() {
        mediaController?.let { controller ->
            controller.stop()
            controller.clearMediaItems()
        }

        _currentTrackId.value = null
        _uiState.value = PlayerUiState(isConnected = _uiState.value.isConnected)
    }

    /**
     * Resets the progress for a track back to the beginning.
     * If the track is currently playing/loaded, stops playback and seeks to start.
     */
    fun resetTrackProgress(trackId: String) {
        viewModelScope.launch {
            // Delete the progress entry to reset it
            tracksRepository.saveProgress(
                TrackProgress(
                    trackId = trackId,
                    currentTime = 0L,
                    duration = 0L,
                    finished = false,
                    lastPlayed = System.currentTimeMillis()
                )
            )

            // If this track is currently loaded in the player, stop it and seek to start
            if (_currentTrackId.value == trackId) {
                mediaController?.let { controller ->
                    controller.stop()
                    controller.seekTo(0)
                }
            }

            // Update UI state to show position at start (for both playing and viewed tracks)
            if (_uiState.value.currentTrack?.id == trackId) {
                _uiState.value = _uiState.value.copy(
                    currentPosition = 0L,
                    isPlaying = false
                )
            }
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

