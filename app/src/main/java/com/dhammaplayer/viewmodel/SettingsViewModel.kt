package com.dhammaplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhammaplayer.data.model.ThemeMode
import com.dhammaplayer.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.AUTO
        )

    fun cycleThemeMode() {
        viewModelScope.launch {
            val nextMode = when (themeMode.value) {
                ThemeMode.AUTO -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.AUTO
            }
            settingsRepository.setThemeMode(nextMode)
        }
    }
}

