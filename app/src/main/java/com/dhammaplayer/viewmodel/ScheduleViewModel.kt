package com.dhammaplayer.viewmodel

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhammaplayer.data.model.Schedule
import com.dhammaplayer.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val schedules: StateFlow<List<Schedule>> = scheduleRepository.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSchedule() {
        // Get current time
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentTime = String.format(Locale.US, "%02d:%02d", hour, minute)

        // All days enabled by default (0=Sunday through 6=Saturday)
        val allDays = listOf(0, 1, 2, 3, 4, 5, 6)

        viewModelScope.launch {
            scheduleRepository.createSchedule(currentTime, allDays)
        }
    }

    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(schedule)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId)
        }
    }

    fun setEnabled(scheduleId: String, enabled: Boolean) {
        viewModelScope.launch {
            scheduleRepository.setEnabled(scheduleId, enabled)
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun getExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }
    }
}

