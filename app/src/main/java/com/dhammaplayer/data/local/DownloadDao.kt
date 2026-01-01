package com.dhammaplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhammaplayer.data.model.DownloadedTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    // Only return manual downloads for UI display
    @Query("SELECT * FROM downloads WHERE isManualDownload = 1 ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    suspend fun getDownload(trackId: String): DownloadedTrack?

    // Only return manual download IDs for UI display
    @Query("SELECT trackId FROM downloads WHERE isManualDownload = 1")
    fun getDownloadedTrackIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadedTrack)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE trackId = :trackId AND isManualDownload = 1)")
    suspend fun isDownloaded(trackId: String): Boolean

    // Auto-cache specific queries
    @Query("SELECT * FROM downloads WHERE isManualDownload = 0 ORDER BY downloadedAt ASC")
    suspend fun getAutoCachedFiles(): List<DownloadedTrack>

    @Query("SELECT COUNT(*) FROM downloads WHERE isManualDownload = 0")
    suspend fun getAutoCacheCount(): Int

    @Query("DELETE FROM downloads WHERE trackId IN (:trackIds)")
    suspend fun deleteDownloads(trackIds: List<String>)
}

