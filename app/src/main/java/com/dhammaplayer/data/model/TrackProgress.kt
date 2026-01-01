package com.dhammaplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks playback progress for an audio track.
 */
@Entity(tableName = "track_progress")
data class TrackProgress(
    @PrimaryKey
    val trackId: String,
    val currentTime: Long, // in milliseconds
    val duration: Long, // in milliseconds
    val finished: Boolean,
    val lastPlayed: Long // timestamp in milliseconds
)

