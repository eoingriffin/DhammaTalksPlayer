package com.dhammaplayer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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

    // Get downloaded tracks
    val downloadedTracks = remember(tracksUiState.tracks, tracksUiState.downloadedIds) {
        tracksUiState.tracks.filter { it.id in tracksUiState.downloadedIds }
    }

    when (currentScreen) {
        AppScreen.PLAYER -> {
            playerUiState.currentTrack?.let { track ->
                PlayerScreen(
                    track = track,
                    albumArt = playerUiState.albumArt,
                    isPlaying = playerUiState.isPlaying,
                    currentPosition = playerUiState.currentPosition,
                    duration = playerUiState.duration,
                    onBackClick = { currentScreen = AppScreen.MAIN },
                    onPlayPause = playerViewModel::togglePlayPause,
                    onSeek = playerViewModel::seekTo,
                    onSkipForward = playerViewModel::skipForward,
                    onSkipBack = playerViewModel::skipBackward,
                    onUpdatePosition = playerViewModel::updatePosition
                )
            }
        }

        AppScreen.MAIN -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Dhamma Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                },
                bottomBar = {
                    Column {
                        // Mini player
                        playerUiState.currentTrack?.let { track ->
                            MiniPlayer(
                                track = track,
                                isPlaying = playerUiState.isPlaying,
                                onTap = { currentScreen = AppScreen.PLAYER },
                                onPlayPause = playerViewModel::togglePlayPause
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
                                isLoading = tracksUiState.isLoading,
                                error = tracksUiState.error,
                                onTrackSelect = { track ->
                                    playerViewModel.playTrack(track)
                                    currentScreen = AppScreen.PLAYER
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
                                onTrackSelect = { track ->
                                    playerViewModel.playTrack(track)
                                    currentScreen = AppScreen.PLAYER
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

