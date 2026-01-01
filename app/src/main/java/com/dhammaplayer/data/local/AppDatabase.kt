package com.dhammaplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.DaysConverter
import com.dhammaplayer.data.model.DownloadedTrack
import com.dhammaplayer.data.model.Schedule
import com.dhammaplayer.data.model.TrackProgress

@Database(
    entities = [
        AudioTrack::class,
        TrackProgress::class,
        Schedule::class,
        DownloadedTrack::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DaysConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun trackProgressDao(): TrackProgressDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun downloadDao(): DownloadDao
}

