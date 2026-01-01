package com.dhammaplayer.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dhammaplayer.data.local.ScheduleDao
import com.dhammaplayer.data.model.Schedule
import com.dhammaplayer.receiver.ScheduledPlaybackReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleDao: ScheduleDao
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun getAllSchedules(): Flow<List<Schedule>> = scheduleDao.getAllSchedules()

    suspend fun getEnabledSchedules(): List<Schedule> = scheduleDao.getEnabledSchedules()

    suspend fun createSchedule(time: String, days: List<Int>): Schedule {
        val schedule = Schedule(
            id = UUID.randomUUID().toString(),
            time = time,
            days = days,
            enabled = true
        )
        scheduleDao.upsertSchedule(schedule)
        scheduleAlarm(schedule)
        return schedule
    }

    suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.upsertSchedule(schedule)
        if (schedule.enabled) {
            scheduleAlarm(schedule)
        } else {
            cancelAlarm(schedule)
        }
    }

    suspend fun deleteSchedule(scheduleId: String) {
        val schedule = scheduleDao.getSchedule(scheduleId)
        schedule?.let { cancelAlarm(it) }
        scheduleDao.deleteSchedule(scheduleId)
    }

    suspend fun setEnabled(scheduleId: String, enabled: Boolean) {
        scheduleDao.setEnabled(scheduleId, enabled)
        val schedule = scheduleDao.getSchedule(scheduleId)
        schedule?.let {
            if (enabled) {
                scheduleAlarm(it)
            } else {
                cancelAlarm(it)
            }
        }
    }

    fun scheduleAlarm(schedule: Schedule) {
        if (!schedule.enabled || schedule.days.isEmpty()) return

        val timeParts = schedule.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Schedule for each enabled day
        schedule.days.forEach { dayOfWeek ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Convert to Java Calendar day (1=Sunday, 7=Saturday)
                val javaDayOfWeek = if (dayOfWeek == 0) Calendar.SUNDAY else dayOfWeek + 1
                set(Calendar.DAY_OF_WEEK, javaDayOfWeek)

                // If this time has already passed this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, ScheduledPlaybackReceiver::class.java).apply {
                action = ScheduledPlaybackReceiver.ACTION_SCHEDULED_PLAYBACK
                putExtra(ScheduledPlaybackReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                putExtra(ScheduledPlaybackReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            }

            val requestCode = "${schedule.id}_$dayOfWeek".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Handle case where exact alarm permission is not granted
                e.printStackTrace()
            }
        }
    }

    private fun cancelAlarm(schedule: Schedule) {
        schedule.days.forEach { dayOfWeek ->
            val intent = Intent(context, ScheduledPlaybackReceiver::class.java).apply {
                action = ScheduledPlaybackReceiver.ACTION_SCHEDULED_PLAYBACK
                putExtra(ScheduledPlaybackReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                putExtra(ScheduledPlaybackReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            }
            val requestCode = "${schedule.id}_$dayOfWeek".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Also cancel the PendingIntent itself
        }
    }

    suspend fun rescheduleAllAlarms() {
        val enabledSchedules = scheduleDao.getEnabledSchedules()
        enabledSchedules.forEach { schedule ->
            scheduleAlarm(schedule)
        }
    }
}

