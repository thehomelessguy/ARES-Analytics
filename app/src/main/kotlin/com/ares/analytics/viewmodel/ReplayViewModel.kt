package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.ReplayEngineService
import com.ares.analytics.service.ReplayFrame
import com.ares.analytics.service.ReplayState
import com.ares.analytics.ui.components.pathplanner.Waypoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReplayStateWrapper(
    val sessionId: String? = null,
    val replayState: ReplayState = ReplayState.STOPPED,
    val progress: Double = 0.0,
    val currentFrame: ReplayFrame? = null,
    val speed: Double = 1.0,
    val activeViewportTab: Int = 0,
    val actualPath: List<Waypoint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class ReplayIntent {
    data class LoadSession(val sessionId: String?) : ReplayIntent()
    object Play : ReplayIntent()
    object Pause : ReplayIntent()
    object Stop : ReplayIntent()
    data class SetSpeed(val speed: Double) : ReplayIntent()
    object StepForward : ReplayIntent()
    object StepBackward : ReplayIntent()
    data class ScrubTo(val progress: Double) : ReplayIntent()
    data class SelectViewportTab(val tabIndex: Int) : ReplayIntent()
}

class ReplayViewModel(
    private val databaseService: DatabaseService,
    private val replayEngineService: ReplayEngineService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ReplayStateWrapper())
    val state: StateFlow<ReplayStateWrapper> = _state.asStateFlow()

    init {
        // Collect from ReplayEngineService flows
        scope.launch {
            replayEngineService.state.collectLatest { s ->
                _state.update { it.copy(replayState = s) }
            }
        }
        scope.launch {
            replayEngineService.progress.collectLatest { p ->
                _state.update { it.copy(progress = p) }
            }
        }
        scope.launch {
            replayEngineService.currentFrame.collectLatest { f ->
                _state.update { it.copy(currentFrame = f) }
            }
        }
        scope.launch {
            replayEngineService.speed.collectLatest { sp ->
                _state.update { it.copy(speed = sp) }
            }
        }
    }

    fun onIntent(intent: ReplayIntent) {
        scope.launch {
            when (intent) {
                is ReplayIntent.LoadSession -> {
                    val sessionId = intent.sessionId
                    _state.update { it.copy(sessionId = sessionId, isLoading = true, errorMessage = null) }
                    if (sessionId != null) {
                        try {
                            withContext(Dispatchers.IO) {
                                replayEngineService.loadSession(sessionId)

                                // Reconstruct actual path from db
                                val xFrames = databaseService.getTelemetryForKey(sessionId, "/Drive/Pose_X")
                                val yFrames = databaseService.getTelemetryForKey(sessionId, "/Drive/Pose_Y")
                                val headingFrames = databaseService.getTelemetryForKey(sessionId, "/Drive/Pose_Heading")

                                val path = mutableListOf<Waypoint>()
                                val size = minOf(xFrames.size, yFrames.size)
                                for (i in 0 until size) {
                                    val x = xFrames[i].value
                                    val y = yFrames[i].value
                                    val heading = headingFrames.getOrNull(i)?.value ?: 0.0
                                    path.add(Waypoint(x, y, heading))
                                }
                                _state.update { it.copy(actualPath = path, isLoading = false) }
                            }
                        } catch (e: Exception) {
                            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load session") }
                        }
                    } else {
                        replayEngineService.stop()
                        _state.update { it.copy(actualPath = emptyList(), isLoading = false) }
                    }
                }
                is ReplayIntent.Play -> {
                    replayEngineService.play()
                }
                is ReplayIntent.Pause -> {
                    replayEngineService.pause()
                }
                is ReplayIntent.Stop -> {
                    replayEngineService.stop()
                }
                is ReplayIntent.SetSpeed -> {
                    replayEngineService.setSpeed(intent.speed)
                }
                is ReplayIntent.StepForward -> {
                    replayEngineService.stepForward()
                }
                is ReplayIntent.StepBackward -> {
                    replayEngineService.stepBackward()
                }
                is ReplayIntent.ScrubTo -> {
                    replayEngineService.scrubTo(intent.progress)
                }
                is ReplayIntent.SelectViewportTab -> {
                    _state.update { it.copy(activeViewportTab = intent.tabIndex) }
                }
            }
        }
    }
}
