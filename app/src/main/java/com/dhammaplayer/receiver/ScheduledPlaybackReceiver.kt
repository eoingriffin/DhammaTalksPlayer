package com.dhammaplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                // Get tracks and find first unfinished one
                val tracks = tracksRepository.getTracks().first()
                val unfinishedTrack = tracksRepository.getFirstUnfinishedTrack(tracks)

                unfinishedTrack?.let { track ->
                    // Check if we have a local file, otherwise use stream URL
                    val audioUri = downloadRepository.getLocalFilePath(track.id)
                        ?: track.audioUrl

                    // Start playback service with intent (don't bind - not allowed from BroadcastReceiver)
                    val serviceIntent = Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_PLAY_TRACK
                        putExtra(PlaybackService.EXTRA_TRACK_ID, track.id)
                        putExtra(PlaybackService.EXTRA_AUDIO_URI, audioUri)
                        putExtra(PlaybackService.EXTRA_TRACK_TITLE, track.title)
                    }

                    // Start foreground service to play the track
                    context.startForegroundService(serviceIntent)
                }

                // Reschedule for next occurrence
                val schedule = scheduleRepository.getEnabledSchedules()
                    .find { it.id == scheduleId }
                schedule?.let {
                    scheduleRepository.scheduleAlarm(it)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

