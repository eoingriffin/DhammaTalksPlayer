package com.dhammaplayer.data.repository

import com.dhammaplayer.data.local.AudioTrackDao
import com.dhammaplayer.data.local.TrackProgressDao
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TalkSource
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
    fun getAllTracks(): Flow<List<AudioTrack>> = audioTrackDao.getAllTracks()

    fun getTracksBySource(source: TalkSource): Flow<List<AudioTrack>> =
        audioTrackDao.getTracksBySource(source.name)

    fun getAllProgress(): Flow<List<TrackProgress>> = trackProgressDao.getAllProgress()

    suspend fun refreshTracks(source: TalkSource): Result<List<AudioTrack>> {
        val result = rssService.fetchDhammaTalks(source)
        result.onSuccess { tracks ->
            audioTrackDao.insertTracks(tracks)
        }
        return result
    }

    suspend fun refreshAllSources(): Result<Unit> {
        var hasError = false
        TalkSource.entries.forEach { source ->
            val result = refreshTracks(source)
            if (result.isFailure) hasError = true
        }
        return if (hasError) Result.failure(Exception("Failed to refresh some sources"))
        else Result.success(Unit)
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

    suspend fun getFirstUnfinishedTrackBySource(
        source: TalkSource,
        allTracks: List<AudioTrack>
    ): AudioTrack? {
        val finishedIds = trackProgressDao.getFinishedTrackIds()
        return allTracks
            .filter { it.source == source.name }
            .firstOrNull { it.id !in finishedIds }
    }

    suspend fun markAsFinished(trackId: String, duration: Long) {
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

