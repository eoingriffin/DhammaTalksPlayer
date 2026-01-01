package com.dhammaplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a downloaded audio track.
 */
@Entity(tableName = "downloads")
data class DownloadedTrack(
    @PrimaryKey
    val trackId: String,
    val filePath: String,
    val downloadedAt: Long // timestamp in milliseconds
)

