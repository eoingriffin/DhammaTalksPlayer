package com.dhammaplayer.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TrackProgress
import com.dhammaplayer.ui.components.TrackItemCard
import com.dhammaplayer.ui.theme.Indigo600
import com.dhammaplayer.ui.theme.Slate200
import com.dhammaplayer.ui.theme.Slate400
import com.dhammaplayer.ui.theme.Slate500

@Composable
fun DownloadsScreen(
    downloadedTracks: List<AudioTrack>,
    progress: Map<String, TrackProgress>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackSelect: (AudioTrack) -> Unit,
    onPlayPauseTrack: (AudioTrack) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Slate500
                )
            }
            Text(
                text = "All Tracks",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Slate500
            )
        }

        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
                tint = Indigo600,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "OFFLINE DOWNLOADS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Indigo600,
                letterSpacing = 1.sp
            )
        }

        if (downloadedTracks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Slate200,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No downloaded tracks yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = downloadedTracks,
                    key = { it.id }
                ) { track ->
                    val isTrackPlaying = track.id == currentTrackId && isPlaying
                    TrackItemCard(
                        track = track,
                        progress = progress[track.id],
                        isActive = track.id == currentTrackId,
                        isPlaying = isTrackPlaying,
                        isDownloaded = true,
                        isDownloading = false,
                        onSelect = { onTrackSelect(track) },
                        onPlayPause = { onPlayPauseTrack(track) },
                        onDownload = { },
                        onRemove = { onRemoveDownload(track.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

