package com.dhammaplayer.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.dhammaplayer.MainActivity
import com.dhammaplayer.R
import com.dhammaplayer.data.model.TrackProgress
import com.dhammaplayer.data.repository.TracksRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var tracksRepository: TracksRepository

    private var mediaSession: MediaSession? = null
    private var progressJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ACTION_PLAY_TRACK = "com.dhammaplayer.ACTION_PLAY_TRACK"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_AUDIO_URI = "audio_uri"
        const val EXTRA_TRACK_TITLE = "track_title"

        private const val NOTIFICATION_CHANNEL_ID = "dhamma_player_playback"
        private const val NOTIFICATION_ID = 1
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                    saveCurrentProgress()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    markCurrentTrackAsFinished()
                }
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_PLAY_TRACK) {
            val trackId = intent.getStringExtra(EXTRA_TRACK_ID)
            val audioUri = intent.getStringExtra(EXTRA_AUDIO_URI)
            val trackTitle = intent.getStringExtra(EXTRA_TRACK_TITLE)

            if (!trackId.isNullOrEmpty() && !audioUri.isNullOrEmpty()) {
                // Show foreground notification immediately when started from scheduled playback
                showForegroundNotification(trackTitle ?: "Dhamma Talk")
                playTrackFromIntent(trackId, audioUri, trackTitle)
            }
        }

        return START_STICKY
    }

    @OptIn(UnstableApi::class)
    private fun showForegroundNotification(title: String) {
        val session = mediaSession ?: return

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Thanissaro Bhikkhu")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(sessionActivityIntent)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Audio playback controls"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun playTrackFromIntent(trackId: String, audioUri: String, trackTitle: String?) {
        val player = mediaSession?.player ?: return

        val mediaItem = MediaItem.Builder()
            .setMediaId(trackId)
            .setUri(audioUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(trackTitle ?: "Dhamma Talk")
                    .setArtist("Thanissaro Bhikkhu")
                    .build()
            )
            .build()

        serviceScope.launch {
            // Get saved progress
            val savedProgress = tracksRepository.getProgress(trackId)
            val startPosition = if (savedProgress != null && !savedProgress.finished) {
                savedProgress.currentTime
            } else {
                0L
            }

            player.setMediaItem(mediaItem, startPosition)
            player.prepare()
            player.play()
        }
    }

    override fun onDestroy() {
        progressJob?.cancel()
        saveCurrentProgress()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (true) {
                saveCurrentProgress()
                delay(5000) // Save progress every 5 seconds
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun saveCurrentProgress() {
        val player = mediaSession?.player ?: return
        val mediaItem = player.currentMediaItem ?: return
        val trackId = mediaItem.mediaId

        if (trackId.isNotEmpty() && player.duration > 0) {
            val currentTime = player.currentPosition
            val duration = player.duration
            val timeRemaining = duration - currentTime
            val isFinished = timeRemaining <= 15_000L // 15 seconds or less remaining

            serviceScope.launch(Dispatchers.IO) {
                tracksRepository.saveProgress(
                    TrackProgress(
                        trackId = trackId,
                        currentTime = currentTime,
                        duration = duration,
                        finished = isFinished,
                        lastPlayed = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun markCurrentTrackAsFinished() {
        val player = mediaSession?.player ?: return
        val mediaItem = player.currentMediaItem ?: return
        val trackId = mediaItem.mediaId
        val duration = player.duration

        if (trackId.isNotEmpty() && duration > 0) {
            serviceScope.launch(Dispatchers.IO) {
                tracksRepository.markAsFinished(trackId, duration)
            }
        }
    }
}

