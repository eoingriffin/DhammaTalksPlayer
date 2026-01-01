package com.dhammaplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.DaysConverter
import com.dhammaplayer.data.model.DownloadedTrack
import com.dhammaplayer.data.model.Schedule
import com.dhammaplayer.data.model.TalkSource
import com.dhammaplayer.data.model.TrackProgress

@Database(
    entities = [
        AudioTrack::class,
        TrackProgress::class,
        Schedule::class,
        DownloadedTrack::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DaysConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun trackProgressDao(): TrackProgressDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add source column to audio_tracks table with default EVENING
                db.execSQL("ALTER TABLE audio_tracks ADD COLUMN source TEXT NOT NULL DEFAULT '${TalkSource.EVENING.name}'")
                // Add talkSource column to schedules table with default EVENING
                db.execSQL("ALTER TABLE schedules ADD COLUMN talkSource TEXT NOT NULL DEFAULT '${TalkSource.EVENING.name}'")
            }
        }
    }
}

