package com.dhammaplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhammaplayer.data.model.TrackProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackProgressDao {

    @Query("SELECT * FROM track_progress")
    fun getAllProgress(): Flow<List<TrackProgress>>

    @Query("SELECT * FROM track_progress WHERE trackId = :trackId")
    suspend fun getProgress(trackId: String): TrackProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: TrackProgress)

    @Query("DELETE FROM track_progress WHERE trackId = :trackId")
    suspend fun deleteProgress(trackId: String)

    @Query("SELECT * FROM track_progress WHERE finished = 0 ORDER BY lastPlayed DESC LIMIT 1")
    suspend fun getLastUnfinishedTrack(): TrackProgress?

    @Query("SELECT trackId FROM track_progress WHERE finished = 1")
    suspend fun getFinishedTrackIds(): List<String>
}

