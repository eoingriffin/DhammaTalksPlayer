package com.dhammaplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an audio track from the DhammaTalks RSS feed.
 */
@Entity(tableName = "audio_tracks")
data class AudioTrack(
    @PrimaryKey
    val id: String,
    val title: String,
    val link: String,
    val pubDate: String,
    val audioUrl: String,
    val description: String,
    val duration: Long? = null,
    val source: String = TalkSource.EVENING.name
)

