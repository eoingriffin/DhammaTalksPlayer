package com.dhammaplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Represents a scheduled playback time.
 */
@Entity(tableName = "schedules")
@TypeConverters(DaysConverter::class)
data class Schedule(
    @PrimaryKey
    val id: String,
    val time: String, // HH:mm format
    val days: List<Int>, // 0-6 (Sun-Sat)
    val enabled: Boolean,
    val talkSource: String = TalkSource.EVENING.name
)

/**
 * Type converter for List<Int> to store days in Room.
 */
class DaysConverter {
    @TypeConverter
    fun fromDaysList(days: List<Int>): String {
        return days.joinToString(",")
    }

    @TypeConverter
    fun toDaysList(daysString: String): List<Int> {
        return if (daysString.isEmpty()) {
            emptyList()
        } else {
            daysString.split(",").map { it.toInt() }
        }
    }
}

