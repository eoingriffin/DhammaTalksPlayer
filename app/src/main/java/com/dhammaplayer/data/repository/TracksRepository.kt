package com.dhammaplayer.data.repository

import com.dhammaplayer.data.local.AudioTrackDao
import com.dhammaplayer.data.local.TrackProgressDao
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TrackProgress
import com.dhammaplayer.data.remote.RssService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TracksRepository @Inject constructor(
    private val rssService: RssService,
    private val audioTrackDao: AudioTrackDao,
    private val trackProgressDao: TrackProgressDao
) {
    fun getTracks(): Flow<List<AudioTrack>> = audioTrackDao.getAllTracks()

    fun getAllProgress(): Flow<List<TrackProgress>> = trackProgressDao.getAllProgress()

    suspend fun refreshTracks(): Result<List<AudioTrack>> {
        val result = rssService.fetchDhammaTalks()
        result.onSuccess { tracks ->
            audioTrackDao.insertTracks(tracks)
        }
        return result
    }

    suspend fun getTrack(id: String): AudioTrack? = audioTrackDao.getTrack(id)

    suspend fun getProgress(trackId: String): TrackProgress? =
        trackProgressDao.getProgress(trackId)

    suspend fun saveProgress(progress: TrackProgress) {
        trackProgressDao.upsertProgress(progress)
    }

    suspend fun getFirstUnfinishedTrack(tracks: List<AudioTrack>): AudioTrack? {
        val finishedIds = trackProgressDao.getFinishedTrackIds()
        return tracks.firstOrNull { it.id !in finishedIds }
    }

    suspend fun markAsFinished(trackId: String, duration: Long) {
        val existing = trackProgressDao.getProgress(trackId)
        trackProgressDao.upsertProgress(
            TrackProgress(
                trackId = trackId,
                currentTime = duration,
                duration = duration,
                finished = true,
                lastPlayed = System.currentTimeMillis()
            )
        )
    }
}

