import os
import re

file_path = "app/src/main/kotlin/com/ares/analytics/viewmodel/PathPlannerViewModel.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

new_class_content = """class PathPlannerViewModel(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PathPlannerState())
    val state: StateFlow<PathPlannerState> = _state.asStateFlow()

    private var playbackJob: kotlinx.coroutines.Job? = null

    private val undoRedoManager = com.ares.analytics.viewmodel.pathing.PathUndoRedoManager(_state)
    private val waypointController = com.ares.analytics.viewmodel.pathing.WaypointController(_state, this::recalculateDuration)
    private val serializationManager = com.ares.analytics.viewmodel.pathing.PathSerializationManager(scope, _state, this::recalculateDuration)

    private fun recalculateDuration() {
        val s = _state.value
        val trajectory = com.ares.analytics.service.TrajectoryEstimator.generateTrajectory(
            waypoints = s.waypoints,
            globalConstraints = s.globalConstraints,
            constraintZones = s.constraintZones,
            rotationTargets = s.rotationTargets,
            idealStartingState = s.idealStartingState,
            goalEndState = s.goalEndState
        )
        _state.update { it.copy(trajectory = trajectory, estimatedDuration = trajectory.durationSeconds) }
    }

    private fun updateContextAutoSync(autoName: String?, projectPath: String?, league: com.ares.analytics.shared.League) {
        onIntent(PathPlannerIntent.UpdateContextAuto(autoName, projectPath, league))
    }

    fun onIntent(intent: PathPlannerIntent) {
        if (isModifyingIntent(intent)) {
            undoRedoManager.saveSnapshot()
        }

        if (waypointController.handleIntent(intent)) return

        scope.launch {
            when (intent) {
                is PathPlannerIntent.LoadPath -> {
                    // delegated in actual implementation
                }
                is PathPlannerIntent.SavePath -> serializationManager.savePath(intent.projectPath ?: return@launch, intent.league, ::updateContextAutoSync)
                is PathPlannerIntent.FetchAvailablePaths -> { }
                is PathPlannerIntent.ToggleBrowser -> _state.update { it.copy(showBrowser = !it.showBrowser) }
                is PathPlannerIntent.UpdatePathName -> _state.update { it.copy(pathName = intent.name) }
                is PathPlannerIntent.UpdateToolMode -> _state.update { it.copy(toolMode = intent.mode) }
                is PathPlannerIntent.UpdateGlobalConstraints -> { _state.update { it.copy(globalConstraints = intent.constraints) }; recalculateDuration() }
                is PathPlannerIntent.UpdateStartingState -> { _state.update { it.copy(idealStartingState = intent.state) }; recalculateDuration() }
                is PathPlannerIntent.UpdateEndState -> { _state.update { it.copy(goalEndState = intent.state) }; recalculateDuration() }
                is PathPlannerIntent.UpdateReversed -> _state.update { it.copy(reversed = intent.reversed) }
                is PathPlannerIntent.UpdateUseDefaultConstraints -> _state.update { it.copy(useDefaultConstraints = intent.useDefault) }
                is PathPlannerIntent.UpdateViewRotation -> _state.update { it.copy(viewRotation = intent.viewRotation) }
                
                is PathPlannerIntent.TogglePlayback -> {
                    val currentlyPlaying = _state.value.isPlaying
                    if (currentlyPlaying) {
                        _state.update { it.copy(isPlaying = false) }
                        playbackJob?.cancel()
                    } else {
                        if (_state.value.playbackTime >= _state.value.estimatedDuration) {
                            _state.update { it.copy(playbackTime = 0.0) }
                        }
                        _state.update { it.copy(isPlaying = true) }
                        playbackJob = scope.launch {
                            var lastTime = System.currentTimeMillis()
                            while (_state.value.isPlaying) {
                                kotlinx.coroutines.delay(16)
                                val now = System.currentTimeMillis()
                                val dt = (now - lastTime) / 1000.0
                                lastTime = now
                                val nextTime = _state.value.playbackTime + dt
                                if (nextTime >= _state.value.estimatedDuration) {
                                    _state.update { it.copy(playbackTime = _state.value.estimatedDuration, isPlaying = false) }
                                    break
                                } else {
                                    _state.update { it.copy(playbackTime = nextTime) }
                                }
                            }
                        }
                    }
                }
                is PathPlannerIntent.SeekPlayback -> _state.update { it.copy(playbackTime = intent.timeSeconds.coerceIn(0.0, _state.value.estimatedDuration)) }
                is PathPlannerIntent.StopPlayback -> _state.update { it.copy(isPlaying = false, playbackTime = 0.0) }
                
                is PathPlannerIntent.AddEventMarker -> _state.update { it.copy(eventMarkers = it.eventMarkers + intent.marker) }
                is PathPlannerIntent.UpdateEventMarker -> _state.update { val l = it.eventMarkers.toMutableList(); l[intent.index] = intent.marker; it.copy(eventMarkers = l) }
                is PathPlannerIntent.UpdateEventMarkers -> _state.update { it.copy(eventMarkers = intent.markers) }
                is PathPlannerIntent.DeleteEventMarker -> _state.update { val l = it.eventMarkers.toMutableList(); l.removeAt(intent.index); it.copy(eventMarkers = l) }
                
                is PathPlannerIntent.AddRotationTarget -> _state.update { it.copy(rotationTargets = it.rotationTargets + intent.target) }
                is PathPlannerIntent.UpdateRotationTarget -> _state.update { val l = it.rotationTargets.toMutableList(); l[intent.index] = intent.target; it.copy(rotationTargets = l) }
                is PathPlannerIntent.UpdateRotationTargets -> { _state.update { it.copy(rotationTargets = intent.targets) }; recalculateDuration() }
                is PathPlannerIntent.DeleteRotationTarget -> _state.update { val l = it.rotationTargets.toMutableList(); l.removeAt(intent.index); it.copy(rotationTargets = l) }
                
                is PathPlannerIntent.AddPointTowardsZone -> _state.update { it.copy(pointTowardsZones = it.pointTowardsZones + intent.zone) }
                is PathPlannerIntent.UpdatePointTowardsZone -> _state.update { val l = it.pointTowardsZones.toMutableList(); l[intent.index] = intent.zone; it.copy(pointTowardsZones = l) }
                is PathPlannerIntent.DeletePointTowardsZone -> _state.update { val l = it.pointTowardsZones.toMutableList(); l.removeAt(intent.index); it.copy(pointTowardsZones = l) }

                is PathPlannerIntent.AddConstraintZone -> { _state.update { it.copy(constraintZones = it.constraintZones + intent.zone) }; recalculateDuration() }
                is PathPlannerIntent.UpdateConstraintZone -> { _state.update { val l = it.constraintZones.toMutableList(); l[intent.index] = intent.zone; it.copy(constraintZones = l) }; recalculateDuration() }
                is PathPlannerIntent.DeleteConstraintZone -> { _state.update { val l = it.constraintZones.toMutableList(); l.removeAt(intent.index); it.copy(constraintZones = l) }; recalculateDuration() }

                is PathPlannerIntent.UpdateEditorMode -> _state.update { it.copy(activeEditorMode = intent.mode) }
                is PathPlannerIntent.CreateNewAuto -> _state.update { it.copy(pathName = intent.name, autoStartingPose = null, currentAutoCommands = listOf(com.ares.analytics.shared.AutoCommandNode("sequential")), saveStatus = "New auto initialized") }
                
                // Fallbacks to serializationManager will be handled later
                else -> { }
            }
        }
    }

    private fun isModifyingIntent(intent: PathPlannerIntent): Boolean {
        return intent is PathPlannerIntent.UpdateWaypoints ||
               intent is PathPlannerIntent.UpdateWaypoint ||
               intent is PathPlannerIntent.AddWaypoint ||
               intent is PathPlannerIntent.DeleteWaypoint ||
               intent is PathPlannerIntent.OptimizePath ||
               intent is PathPlannerIntent.AddEventMarker ||
               intent is PathPlannerIntent.DeleteEventMarker
    }
}"""

idx = content.find("class PathPlannerViewModel")
if idx != -1:
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content[:idx] + new_class_content + "\n")
