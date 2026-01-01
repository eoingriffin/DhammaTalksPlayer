package com.dhammaplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TalkSource
import com.dhammaplayer.data.model.TrackProgress
import com.dhammaplayer.data.repository.DownloadRepository
import com.dhammaplayer.data.repository.TracksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TracksUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val allTracks: List<AudioTrack> = emptyList(),
    val progress: Map<String, TrackProgress> = emptyMap(),
    val downloadedIds: Set<String> = emptySet(),
    val downloadingIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedSource: TalkSource = TalkSource.EVENING
)

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val tracksRepository: TracksRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedSource = MutableStateFlow(TalkSource.EVENING)

    val selectedSource: StateFlow<TalkSource> = _selectedSource.asStateFlow()

    val uiState: StateFlow<TracksUiState> = combine(
        tracksRepository.getAllTracks(),
        tracksRepository.getAllProgress(),
        downloadRepository.getDownloadedTrackIds(),
        _downloadingIds,
        _isLoading,
        _error,
        _selectedSource
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTracks = values[0] as List<AudioTrack>
        val progressList = values[1] as List<TrackProgress>
        val downloadedIds = values[2] as List<String>
        val downloadingIds = values[3] as Set<String>
        val isLoading = values[4] as Boolean
        val error = values[5] as String?
        val selectedSource = values[6] as TalkSource

        // Filter tracks by selected source
        val filteredTracks = allTracks.filter { it.source == selectedSource.name }

        TracksUiState(
            tracks = filteredTracks,
            allTracks = allTracks,
            progress = progressList.associateBy { it.trackId },
            downloadedIds = downloadedIds.toSet(),
            downloadingIds = downloadingIds,
            isLoading = isLoading,
            error = error,
            selectedSource = selectedSource
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TracksUiState()
    )

    init {
        refreshTracks()
    }

    fun setSelectedSource(source: TalkSource) {
        _selectedSource.value = source
    }

    fun refreshTracks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Refresh all sources
            val result = tracksRepository.refreshAllSources()
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to load tracks"
            }

            _isLoading.value = false
        }
    }

    fun downloadTrack(track: AudioTrack) {
        if (_downloadingIds.value.contains(track.id)) return

        viewModelScope.launch {
            _downloadingIds.value = _downloadingIds.value + track.id

            downloadRepository.downloadTrack(track)

            _downloadingIds.value = _downloadingIds.value - track.id
        }
    }

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(trackId)
        }
    }

    suspend fun getTrack(trackId: String): AudioTrack? {
        return tracksRepository.getTrack(trackId)
    }

    suspend fun getAudioUri(trackId: String, streamUrl: String): String {
        return downloadRepository.getLocalFilePath(trackId) ?: streamUrl
    }
}

