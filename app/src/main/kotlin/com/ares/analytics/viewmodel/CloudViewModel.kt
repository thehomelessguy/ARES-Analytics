package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.FirebaseClientService
import com.ares.analytics.service.SyncEngineService
import com.ares.analytics.shared.SessionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CloudState(
    val summaries: List<SessionSummary> = emptyList(),
    val isSyncing: Boolean = false,
    val uploadingSessionId: String? = null,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

sealed class CloudIntent {
    object RefreshSessions : CloudIntent()
    data class PerformDeltaSync(val teamId: String, val seasonId: String) : CloudIntent()
    data class UploadSession(val sessionId: String) : CloudIntent()
    object ClearError : CloudIntent()
}

class CloudViewModel(
    private val databaseService: DatabaseService,
    private val syncEngineService: SyncEngineService,
    private val firebaseClientService: FirebaseClientService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState> = _state.asStateFlow()

    init {
        checkAuth()
        onIntent(CloudIntent.RefreshSessions)
    }

    private fun checkAuth() {
        val hasToken = firebaseClientService.getFirebaseToken() != null || firebaseClientService.isDevMode()
        _state.update { it.copy(isAuthenticated = hasToken) }
    }

    fun onIntent(intent: CloudIntent) {
        scope.launch {
            when (intent) {
                is CloudIntent.RefreshSessions -> {
                    checkAuth()
                    val summaries = databaseService.getAllSessionSummaries().sortedByDescending { it.createdAt }
                    _state.update { it.copy(summaries = summaries) }
                }
                is CloudIntent.PerformDeltaSync -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        syncEngineService.performDeltaSync(intent.teamId, intent.seasonId)
                        val summaries = databaseService.getAllSessionSummaries().sortedByDescending { it.createdAt }
                        _state.update { it.copy(summaries = summaries, isSyncing = false) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Sync failed") }
                    }
                }
                is CloudIntent.UploadSession -> {
                    _state.update { it.copy(uploadingSessionId = intent.sessionId, errorMessage = null) }
                    try {
                        syncEngineService.uploadSession(intent.sessionId)
                        _state.update { it.copy(uploadingSessionId = null) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(uploadingSessionId = null, errorMessage = e.message ?: "Upload failed") }
                    }
                }
                is CloudIntent.ClearError -> {
                    _state.update { it.copy(errorMessage = null) }
                }
            }
        }
    }
}
