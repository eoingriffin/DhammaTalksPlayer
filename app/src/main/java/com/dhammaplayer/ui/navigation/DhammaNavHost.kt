package com.dhammaplayer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhammaplayer.data.model.ThemeMode
import com.dhammaplayer.ui.components.BottomNavBar
import com.dhammaplayer.ui.components.BottomNavItem
import com.dhammaplayer.ui.components.MiniPlayer
import com.dhammaplayer.ui.screens.DownloadsScreen
import com.dhammaplayer.ui.screens.LibraryScreen
import com.dhammaplayer.ui.screens.PlayerScreen
import com.dhammaplayer.ui.screens.ScheduleScreen
import com.dhammaplayer.viewmodel.PlayerViewModel
import com.dhammaplayer.viewmodel.ScheduleViewModel
import com.dhammaplayer.viewmodel.TracksViewModel

enum class AppScreen {
    MAIN,
    PLAYER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhammaNavHost(
    themeMode: ThemeMode,
    onThemeModeChange: () -> Unit,
    tracksViewModel: TracksViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    scheduleViewModel: ScheduleViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val tracksUiState by tracksViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    val currentTrackId by playerViewModel.currentTrackId.collectAsState()
    val schedules by scheduleViewModel.schedules.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
    var selectedNavItem by remember { mutableStateOf(BottomNavItem.LIBRARY) }

    // Connect to playback service
    LaunchedEffect(Unit) {
        playerViewModel.connectToService()
    }

    // Handle back button - navigate to Library from other screens
    BackHandler(enabled = currentScreen == AppScreen.PLAYER || selectedNavItem != BottomNavItem.LIBRARY) {
        when {
            // If on player screen, go back to main screen
            currentScreen == AppScreen.PLAYER -> {
                currentScreen = AppScreen.MAIN
            }
            // If on any other tab, go back to Library
            selectedNavItem != BottomNavItem.LIBRARY -> {
                selectedNavItem = BottomNavItem.LIBRARY
            }
        }
    }

    // Get downloaded tracks
    val downloadedTracks = remember(tracksUiState.tracks, tracksUiState.downloadedIds) {
        tracksUiState.tracks.filter { it.id in tracksUiState.downloadedIds }
    }

    when (currentScreen) {
        AppScreen.PLAYER -> {
            playerUiState.currentTrack?.let { track ->
                // Check if the viewed track is the same as the playing track
                val isViewingPlayingTrack = track.id == currentTrackId
                // Controls enabled if:
                // 1. Viewing the track that's loaded in the player (playing or paused), OR
                // 2. Nothing is actively playing (isPlaying is false)
                // AND the duration is known (greater than 0)
                val isNothingActivelyPlaying = !playerUiState.isPlaying
                val hasDuration = playerUiState.duration > 0
                val isControlsEnabled =
                    (isViewingPlayingTrack || isNothingActivelyPlaying) && hasDuration

                // Find previous and next tracks in the current list
                val currentIndex = tracksUiState.tracks.indexOfFirst { it.id == track.id }
                val hasPrevious = currentIndex > 0
                val hasNext = currentIndex >= 0 && currentIndex < tracksUiState.tracks.size - 1

                PlayerScreen(
                    track = track,
                    albumArt = playerUiState.albumArt,
                    isPlaying = isViewingPlayingTrack && playerUiState.isPlaying,
                    isControlsEnabled = isControlsEnabled,
                    currentPosition = playerUiState.currentPosition,
                    duration = playerUiState.duration,
                    onBackClick = { currentScreen = AppScreen.MAIN },
                    onPlayPause = {
                        if (isViewingPlayingTrack) {
                            playerViewModel.togglePlayPause()
                        } else {
                            playerViewModel.playTrack(track)
                        }
                    },
                    onSeek = { position ->
                        if (isViewingPlayingTrack) {
                            playerViewModel.seekTo(position)
                        } else {
                            // Update viewed position when nothing is actively playing
                            playerViewModel.updateViewedPosition(position)
                        }
                    },
                    onSkipForward = playerViewModel::skipForward,
                    onSkipBack = playerViewModel::skipBackward,
                    onUpdatePosition = {
                        if (isViewingPlayingTrack) {
                            playerViewModel.updatePosition()
                        }
                    },
                    onReset = {
                        playerViewModel.resetTrackProgress(track.id)
                    },
                    onPrevious = {
                        if (hasPrevious) {
                            val previousTrack = tracksUiState.tracks[currentIndex - 1]
                            playerViewModel.selectTrack(previousTrack)
                        }
                    },
                    onNext = {
                        if (hasNext) {
                            val nextTrack = tracksUiState.tracks[currentIndex + 1]
                            playerViewModel.selectTrack(nextTrack)
                        }
                    },
                    hasPrevious = hasPrevious,
                    hasNext = hasNext
                )
            }
        }

        AppScreen.MAIN -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Dhamma Talks",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        actions = {
                            IconButton(onClick = onThemeModeChange) {
                                Icon(
                                    imageVector = when (themeMode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = when (themeMode) {
                                        ThemeMode.LIGHT -> "Light mode (tap for dark)"
                                        ThemeMode.DARK -> "Dark mode (tap for auto)"
                                        ThemeMode.AUTO -> "Auto mode (tap for light)"
                                    },
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                },
                bottomBar = {
                    Column {
                        // Mini player - shows the track that's actually playing
                        playerUiState.playingTrack?.let { track ->
                            MiniPlayer(
                                track = track,
                                isPlaying = playerUiState.isPlaying,
                                onTap = {
                                    // Navigate to the playing track's player screen
                                    playerViewModel.selectTrack(track)
                                    currentScreen = AppScreen.PLAYER
                                },
                                onPlayPause = playerViewModel::togglePlayPause,
                                onStop = playerViewModel::stopPlayback
                            )
                        }

                        // Bottom navigation
                        BottomNavBar(
                            selectedItem = selectedNavItem,
                            onItemSelected = { selectedNavItem = it }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedNavItem) {
                        BottomNavItem.LIBRARY -> {
                            LibraryScreen(
                                tracks = tracksUiState.tracks,
                                progress = tracksUiState.progress,
                                downloadedIds = tracksUiState.downloadedIds,
                                downloadingIds = tracksUiState.downloadingIds,
                                currentTrackId = currentTrackId,
                                isPlaying = playerUiState.isPlaying,
                                isLoading = tracksUiState.isLoading,
                                error = tracksUiState.error,
                                selectedSource = tracksUiState.selectedSource,
                                onSourceChange = tracksViewModel::setSelectedSource,
                                onTrackSelect = { track ->
                                    playerViewModel.selectTrack(track)
                                    currentScreen = AppScreen.PLAYER
                                },
                                onPlayPauseTrack = { track ->
                                    if (currentTrackId == track.id && playerUiState.isPlaying) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playTrack(track)
                                    }
                                },
                                onDownload = tracksViewModel::downloadTrack,
                                onRemoveDownload = tracksViewModel::removeDownload,
                                onRetry = tracksViewModel::refreshTracks
                            )
                        }

                        BottomNavItem.OFFLINE -> {
                            DownloadsScreen(
                                downloadedTracks = downloadedTracks,
                                progress = tracksUiState.progress,
                                currentTrackId = currentTrackId,
                                isPlaying = playerUiState.isPlaying,
                                onTrackSelect = { track ->
                                    playerViewModel.selectTrack(track)
                                    currentScreen = AppScreen.PLAYER
                                },
                                onPlayPauseTrack = { track ->
                                    if (currentTrackId == track.id && playerUiState.isPlaying) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playTrack(track)
                                    }
                                },
                                onRemoveDownload = tracksViewModel::removeDownload,
                                onBackClick = { selectedNavItem = BottomNavItem.LIBRARY }
                            )
                        }

                        BottomNavItem.SCHEDULE -> {
                            ScheduleScreen(
                                schedules = schedules,
                                canScheduleExactAlarms = scheduleViewModel.canScheduleExactAlarms(),
                                onAddSchedule = { scheduleViewModel.addSchedule() },
                                onUpdateSchedule = scheduleViewModel::updateSchedule,
                                onDeleteSchedule = scheduleViewModel::deleteSchedule,
                                onToggleEnabled = scheduleViewModel::setEnabled,
                                onRequestExactAlarmPermission = {
                                    scheduleViewModel.getExactAlarmSettingsIntent()?.let {
                                        context.startActivity(it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

