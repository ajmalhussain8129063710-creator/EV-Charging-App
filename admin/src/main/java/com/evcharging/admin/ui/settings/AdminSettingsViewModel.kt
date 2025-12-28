package com.evcharging.admin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.admin.data.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    private val themeStore: ThemeStore
) : ViewModel() {
    val isDarkTheme: StateFlow<Boolean> = themeStore.isDarkTheme

    fun toggleTheme(isDark: Boolean) {
        themeStore.setDarkTheme(isDark)
    }
}
