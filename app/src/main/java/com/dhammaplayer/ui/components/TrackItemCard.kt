package com.dhammaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TrackProgress
import com.dhammaplayer.ui.theme.Green500
import com.dhammaplayer.ui.theme.Green600
import com.dhammaplayer.ui.theme.Indigo100
import com.dhammaplayer.ui.theme.Indigo600
import com.dhammaplayer.ui.theme.Slate100
import com.dhammaplayer.ui.theme.Slate400
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TrackItemCard(
    track: AudioTrack,
    progress: TrackProgress?,
    isActive: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onSelect: () -> Unit,
    onPlayPause: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFinished = progress?.finished == true
    val completion = if (progress != null && progress.duration > 0) {
        (progress.currentTime.toFloat() / progress.duration) * 100f
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Indigo100 else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) Indigo600 else Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary else Slate400,
                        modifier = Modifier.size(24.dp)
                    )

                    if (isFinished) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 0.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Green500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Title and metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isActive) Indigo600 else MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(track.pubDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )

                        if (isDownloaded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Green600.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Save,
                                        contentDescription = null,
                                        tint = Green600,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "OFFLINE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green600
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Indigo600
                    )
                } else if (isDownloaded) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove download",
                            tint = Slate400
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Slate400
                        )
                    }
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isActive) Indigo600 else Slate400,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Progress bar
            if (completion > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = completion / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isFinished) Green500 else Indigo600)
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString.take(12)
    }
}

