package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.EnvironmentService
import com.ares.analytics.shared.WorkspaceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class SettingsState(
    /**
     * config val.
     */
    val config: WorkspaceConfig? = null,
    /**
     * theme val.
     */
    val theme: String = "Dark", // "System", "Light", "Dark"
    /**
     * audibleAlertsEnabled val.
     */
    val audibleAlertsEnabled: Boolean = true,
    /**
     * autoCheckUpdates val.
     */
    val autoCheckUpdates: Boolean = true,
    /**
     * autoSyncEnabled val.
     */
    val autoSyncEnabled: Boolean = false,
    /**
     * saveStatus val.
     */
    val saveStatus: String = "",
    /**
     * isLoading val.
     */
    val isLoading: Boolean = false,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null
)

sealed class SettingsIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadSettings(val config: WorkspaceConfig) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveConfig(val config: WorkspaceConfig) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateTheme(val theme: String) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetAudibleAlertsEnabled(val enabled: Boolean) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetAutoCheckUpdates(val enabled: Boolean) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetAutoSyncEnabled(val enabled: Boolean) : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearDatabase : SettingsIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearSaveStatus : SettingsIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SettingsViewModel(
    private val databaseService: DatabaseService,
    private val environmentService: EnvironmentService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SettingsState())
    /**
     * state val.
     */
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: SettingsIntent) {
        scope.launch {
            when (intent) {
                is SettingsIntent.LoadSettings -> {
                    _state.update { it.copy(config = intent.config) }
                }
                is SettingsIntent.SaveConfig -> {
                    _state.update { it.copy(isLoading = true, saveStatus = "", errorMessage = null) }
                    try {
                        withContext(Dispatchers.IO) {
                            environmentService.saveConfig(intent.config)
                        }
                        _state.update { it.copy(config = intent.config, saveStatus = "Settings saved successfully!") }
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = e.message ?: "Failed to save settings") }
                    } finally {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is SettingsIntent.UpdateTheme -> {
                    _state.update { it.copy(theme = intent.theme) }
                }
                is SettingsIntent.SetAudibleAlertsEnabled -> {
                    _state.update { it.copy(audibleAlertsEnabled = intent.enabled) }
                }
                is SettingsIntent.SetAutoCheckUpdates -> {
                    _state.update { it.copy(autoCheckUpdates = intent.enabled) }
                }
                is SettingsIntent.SetAutoSyncEnabled -> {
                    _state.update { it.copy(autoSyncEnabled = intent.enabled) }
                }
                is SettingsIntent.ClearDatabase -> {
                    _state.update { it.copy(isLoading = true, saveStatus = "", errorMessage = null) }
                    try {
                        withContext(Dispatchers.IO) {
                            /**
                             * sessions val.
                             */
                            val sessions = databaseService.getSessions()
                            sessions.forEach {
                                databaseService.deleteSession(it.sessionId)
                            }
                        }
                        _state.update { it.copy(saveStatus = "Database cleared successfully!") }
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = e.message ?: "Failed to clear database") }
                    } finally {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is SettingsIntent.ClearSaveStatus -> {
                    _state.update { it.copy(saveStatus = "") }
                }
            }
        }
    }
}
