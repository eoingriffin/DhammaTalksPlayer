package com.dhammaplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhammaplayer.data.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Re-register all alarms after device reboot
            CoroutineScope(Dispatchers.IO).launch {
                scheduleRepository.rescheduleAllAlarms()
            }
        }
    }
}

