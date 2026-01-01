package com.dhammaplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TalkSource
import com.dhammaplayer.data.repository.DownloadRepository
import com.dhammaplayer.data.repository.ScheduleRepository
import com.dhammaplayer.data.repository.TracksRepository
import com.dhammaplayer.media.PlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledPlaybackReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tracksRepository: TracksRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    companion object {
        const val ACTION_SCHEDULED_PLAYBACK = "com.dhammaplayer.SCHEDULED_PLAYBACK"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_DAY_OF_WEEK = "day_of_week"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCHEDULED_PLAYBACK) return

        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the schedule to determine which source to use
                val schedule = scheduleRepository.getEnabledSchedules()
                    .find { it.id == scheduleId }

                val talkSource = try {
                    TalkSource.valueOf(schedule?.talkSource ?: TalkSource.EVENING.name)
                } catch (_: Exception) {
                    TalkSource.EVENING
                }

                val allTracks = tracksRepository.getAllTracks().first()
                // Filter tracks by the schedule's source
                val tracks = allTracks.filter { it.source == talkSource.name }
                val isNetworkAvailable = isNetworkAvailable(context)

                // Find a track to play
                val trackToPlay: AudioTrack?
                val audioUri: String?

                if (isNetworkAvailable) {
                    // Online: find first unfinished track from the source, prefer local file if available
                    trackToPlay = tracksRepository.getFirstUnfinishedTrack(tracks)
                    audioUri = trackToPlay?.let { track ->
                        downloadRepository.getLocalFilePath(track.id) ?: track.audioUrl
                    }
                } else {
                    // Offline: find first unfinished downloaded track from the source
                    val downloadedIds = downloadRepository.getDownloadedTrackIds().first().toSet()
                    val downloadedTracks = tracks.filter { it.id in downloadedIds }
                    trackToPlay = tracksRepository.getFirstUnfinishedTrack(downloadedTracks)
                    audioUri = trackToPlay?.let { track ->
                        downloadRepository.getLocalFilePath(track.id)
                    }
                }

                if (trackToPlay != null && !audioUri.isNullOrEmpty()) {
                    // Start playback service with intent (don't bind - not allowed from BroadcastReceiver)
                    val serviceIntent = Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_PLAY_TRACK
                        putExtra(PlaybackService.EXTRA_TRACK_ID, trackToPlay.id)
                        putExtra(PlaybackService.EXTRA_AUDIO_URI, audioUri)
                        putExtra(PlaybackService.EXTRA_TRACK_TITLE, trackToPlay.title)
                    }

                    // Start foreground service to play the track
                    context.startForegroundService(serviceIntent)
                }

                // Reschedule for next week's occurrence
                schedule?.let {
                    scheduleRepository.scheduleAlarm(it)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

