package com.ares.analytics.viewmodel

import com.ares.analytics.service.ConstantsParserService
import com.ares.analytics.service.TunableConstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TuningState(
    val constants: List<TunableConstant> = emptyList(),
    val projectPath: String? = null,
    val isLoading: Boolean = false,
    val saveStatus: String = "",
    val errorMessage: String? = null
)

sealed class TuningIntent {
    data class LoadConstants(val projectPath: String) : TuningIntent()
    data class SaveConstant(val constant: TunableConstant, val newValue: Double) : TuningIntent()
    object ClearSaveStatus : TuningIntent()
}

class TuningViewModel(
    private val constantsParserService: ConstantsParserService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TuningState())
    val state: StateFlow<TuningState> = _state.asStateFlow()

    fun onIntent(intent: TuningIntent) {
        scope.launch {
            when (intent) {
                is TuningIntent.LoadConstants -> {
                    val path = intent.projectPath
                    _state.update { it.copy(projectPath = path, isLoading = true, errorMessage = null) }
                    try {
                        val list = withContext(Dispatchers.IO) {
                            constantsParserService.loadTunableConstants(path)
                        }
                        _state.update { it.copy(constants = list, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load tunable constants") }
                    }
                }
                is TuningIntent.SaveConstant -> {
                    _state.update { it.copy(isLoading = true, saveStatus = "", errorMessage = null) }
                    try {
                        withContext(Dispatchers.IO) {
                            constantsParserService.saveConstant(intent.constant, intent.newValue)
                            // Re-load constants after saving
                            val path = _state.value.projectPath
                            if (path != null) {
                                val list = constantsParserService.loadTunableConstants(path)
                                _state.update { it.copy(constants = list, saveStatus = "Successfully saved ${intent.constant.name}!") }
                            }
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = e.message ?: "Failed to save constant") }
                    } finally {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is TuningIntent.ClearSaveStatus -> {
                    _state.update { it.copy(saveStatus = "") }
                }
            }
        }
    }
}
