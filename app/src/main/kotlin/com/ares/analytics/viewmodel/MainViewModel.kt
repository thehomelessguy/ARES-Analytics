package com.ares.analytics.viewmodel

import com.ares.analytics.service.EnvironmentService
import com.ares.analytics.service.EventApiService
import com.ares.analytics.service.MatchInfo
import com.ares.analytics.shared.ForensicsResponse
import com.ares.analytics.ui.components.NavigationTarget
import com.ares.analytics.shared.WorkspaceConfig
import com.ares.analytics.shared.League
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val config: WorkspaceConfig? = null,
    val activeNav: NavigationTarget = NavigationTarget.DASHBOARD,
    val matches: List<MatchInfo> = emptyList(),
    val runsIndexReloadTrigger: Int = 0,
    val diagnosticsResponse: ForensicsResponse? = null,
    val isTerminalOpen: Boolean = false,
    val showUpdateBanner: Boolean = true
)

sealed class MainIntent {
    object LoadConfig : MainIntent()
    data class SetActiveNav(val nav: NavigationTarget) : MainIntent()
    data class SetMatches(val matches: List<MatchInfo>) : MainIntent()
    object TriggerRunsIndexReload : MainIntent()
    data class SetDiagnosticsResponse(val response: ForensicsResponse?) : MainIntent()
    data class SetTerminalOpen(val isOpen: Boolean) : MainIntent()
    data class SetShowUpdateBanner(val show: Boolean) : MainIntent()
    data class SaveConfig(val config: WorkspaceConfig) : MainIntent()
}

class MainViewModel(
    private val environmentService: EnvironmentService,
    private val eventApiService: EventApiService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        onIntent(MainIntent.LoadConfig)
    }

    fun onIntent(intent: MainIntent) {
        scope.launch {
            when (intent) {
                is MainIntent.LoadConfig -> {
                    val loaded = environmentService.loadConfig()
                    _state.update { it.copy(config = loaded) }
                    if (loaded != null) {
                        loadMatchSchedule(loaded)
                    }
                }
                is MainIntent.SetActiveNav -> {
                    _state.update { it.copy(activeNav = intent.nav) }
                }
                is MainIntent.SetMatches -> {
                    _state.update { it.copy(matches = intent.matches) }
                }
                is MainIntent.TriggerRunsIndexReload -> {
                    _state.update { it.copy(runsIndexReloadTrigger = it.runsIndexReloadTrigger + 1) }
                }
                is MainIntent.SetDiagnosticsResponse -> {
                    _state.update { it.copy(diagnosticsResponse = intent.response) }
                }
                is MainIntent.SetTerminalOpen -> {
                    _state.update { it.copy(isTerminalOpen = intent.isOpen) }
                }
                is MainIntent.SetShowUpdateBanner -> {
                    _state.update { it.copy(showUpdateBanner = intent.show) }
                }
                is MainIntent.SaveConfig -> {
                    environmentService.saveConfig(intent.config)
                    _state.update { it.copy(config = intent.config) }
                    loadMatchSchedule(intent.config)
                }
            }
        }
    }

    private suspend fun loadMatchSchedule(currentConf: WorkspaceConfig) {
        val eventCode = currentConf.eventCode
        if (eventCode != null && eventCode.isNotEmpty()) {
            val list = if (currentConf.league == League.FTC) {
                val apiKey = currentConf.toaApiKey ?: ""
                eventApiService.fetchFtcEventSchedule(eventCode, apiKey)
            } else {
                val apiKey = currentConf.tbaApiKey ?: ""
                eventApiService.fetchFrcEventSchedule(eventCode, apiKey)
            }
            _state.update { it.copy(matches = list) }
        } else {
            _state.update { it.copy(matches = emptyList()) }
        }
    }
}
