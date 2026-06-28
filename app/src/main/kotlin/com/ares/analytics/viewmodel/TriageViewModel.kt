package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.shared.ForensicsResponse
import com.ares.analytics.shared.HardwareTopology
import com.ares.analytics.shared.TopologyNode
import com.ares.analytics.ui.components.triage.TriageChecklistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TriageState(
    val topology: HardwareTopology? = null,
    val selectedNode: TopologyNode? = null,
    val diagnosticsResponse: ForensicsResponse? = null,
    val checklistItems: List<TriageChecklistItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class TriageIntent {
    data class LoadTopology(val robotId: String) : TriageIntent()
    data class SelectNode(val node: TopologyNode?) : TriageIntent()
    data class SetDiagnostics(val diagnostics: ForensicsResponse?) : TriageIntent()
    data class ToggleChecklistItem(val index: Int, val currentUser: String) : TriageIntent()
}

class TriageViewModel(
    private val databaseService: DatabaseService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TriageState())
    val state: StateFlow<TriageState> = _state.asStateFlow()

    fun onIntent(intent: TriageIntent) {
        scope.launch {
            when (intent) {
                is TriageIntent.LoadTopology -> {
                    _state.update { it.copy(isLoading = true, errorMessage = null) }
                    try {
                        val top = withContext(Dispatchers.IO) {
                            databaseService.getTopology(intent.robotId)
                        }
                        _state.update { it.copy(topology = top, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load topology") }
                    }
                }
                is TriageIntent.SelectNode -> {
                    _state.update { it.copy(selectedNode = intent.node) }
                }
                is TriageIntent.SetDiagnostics -> {
                    val checklist = intent.diagnostics?.recommendedActions?.map {
                        TriageChecklistItem(action = it)
                    } ?: emptyList()
                    _state.update { it.copy(diagnosticsResponse = intent.diagnostics, checklistItems = checklist) }
                }
                is TriageIntent.ToggleChecklistItem -> {
                    val currentList = _state.value.checklistItems.toMutableList()
                    if (intent.index in currentList.indices) {
                        val item = currentList[intent.index]
                        val isChecked = !item.isChecked
                        currentList[intent.index] = item.copy(
                            isChecked = isChecked,
                            completedAt = if (isChecked) System.currentTimeMillis() else null,
                            completedBy = if (isChecked) intent.currentUser else null
                        )
                        _state.update { it.copy(checklistItems = currentList) }
                    }
                }
            }
        }
    }
}
