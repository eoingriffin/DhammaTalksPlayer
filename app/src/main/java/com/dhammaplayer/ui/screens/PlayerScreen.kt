package com.dhammaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.ui.components.PlaybackControlsBar
import com.dhammaplayer.ui.theme.Indigo100
import com.dhammaplayer.ui.theme.Indigo400
import com.dhammaplayer.ui.theme.Slate500
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    track: AudioTrack,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBack: () -> Unit,
    onUpdatePosition: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Update position periodically while playing
    var displayPosition by remember { mutableLongStateOf(currentPosition) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            onUpdatePosition()
            delay(1000)
        }
    }

    LaunchedEffect(currentPosition) {
        displayPosition = currentPosition
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Back button
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
                text = "Library",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Slate500
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album art placeholder
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Indigo100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = Indigo400,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track title
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Artist
            Text(
                text = "Thanissaro Bhikkhu",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Slate500
            )
        }

        // Playback controls
        PlaybackControlsBar(
            isPlaying = isPlaying,
            currentPosition = displayPosition,
            duration = duration,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSkipForward = onSkipForward,
            onSkipBack = onSkipBack
        )
    }
}

