package com.dhammaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhammaplayer.data.model.Schedule
import com.dhammaplayer.data.model.TalkSource
import com.dhammaplayer.ui.theme.Amber50
import com.dhammaplayer.ui.theme.Amber800
import com.dhammaplayer.ui.theme.Indigo600
import com.dhammaplayer.ui.theme.Red400
import com.dhammaplayer.ui.theme.Slate200
import com.dhammaplayer.ui.theme.Slate300
import com.dhammaplayer.ui.theme.Slate400
import com.dhammaplayer.ui.theme.Slate50
import com.dhammaplayer.ui.theme.Slate500
import java.util.Locale

@Composable
fun ScheduleScreen(
    schedules: List<Schedule>,
    canScheduleExactAlarms: Boolean,
    onAddSchedule: () -> Unit,
    onUpdateSchedule: (Schedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Schedules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Auto-play latest unplayed talk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddSchedule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Permission warning for Android 12+
        if (!canScheduleExactAlarms) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Amber50
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Permission Required",
                            fontWeight = FontWeight.Bold,
                            color = Amber800
                        )
                        Text(
                            text = "To schedule exact alarms, please grant the 'Alarms & reminders' permission in settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber800
                        )
                        Button(
                            onClick = onRequestExactAlarmPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Amber800
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }

        // Schedule items
        items(
            items = schedules,
            key = { it.id }
        ) { schedule ->
            ScheduleItemCard(
                schedule = schedule,
                onTimeChange = { newTime ->
                    onUpdateSchedule(schedule.copy(time = newTime))
                },
                onDaysChange = { newDays ->
                    onUpdateSchedule(schedule.copy(days = newDays))
                },
                onSourceChange = { newSource ->
                    onUpdateSchedule(schedule.copy(talkSource = newSource.name))
                },
                onToggleEnabled = { enabled ->
                    onToggleEnabled(schedule.id, enabled)
                },
                onDelete = { onDeleteSchedule(schedule.id) }
            )
        }

        // Note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Note: Scheduled playback will start the first unfinished talk at the scheduled time. Make sure your device is not in Do Not Disturb mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleItemCard(
    schedule: Schedule,
    onTimeChange: (String) -> Unit,
    onDaysChange: (List<Int>) -> Unit,
    onSourceChange: (TalkSource) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showSourceDropdown by remember { mutableStateOf(false) }

    val currentSource = try {
        TalkSource.valueOf(schedule.talkSource)
    } catch (_: Exception) {
        TalkSource.EVENING
    }

    // Time picker dialog
    if (showTimePicker) {
        // Parse current time from schedule when dialog opens
        val timeParts = schedule.time.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 6
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTime = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        onTimeChange(newTime)
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time display (tap to edit)
                Text(
                    text = schedule.time,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { showTimePicker = true }
                )

                // Day selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                    dayLabels.forEachIndexed { index, label ->
                        val isSelected = schedule.days.contains(index)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Indigo600 else Slate50
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Indigo600 else Slate200,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val newDays = if (isSelected) {
                                        schedule.days.filter { it != index }
                                    } else {
                                        schedule.days + index
                                    }
                                    onDaysChange(newDays)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Slate400
                            )
                        }
                    }
                }

                // Source selector dropdown
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(
                        onClick = { showSourceDropdown = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = currentSource.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select source",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false }
                    ) {
                        TalkSource.entries.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = source.displayName,
                                        fontWeight = if (source == currentSource) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSourceChange(source)
                                    showSourceDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Indigo600,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Slate300
                    )
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete schedule",
                        tint = Red400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

