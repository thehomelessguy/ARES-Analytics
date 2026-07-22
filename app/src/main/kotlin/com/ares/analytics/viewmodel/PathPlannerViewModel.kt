package com.ares.analytics.viewmodel

import com.ares.analytics.shared.AppJson


import com.ares.analytics.service.TrajectoryEstimator
import com.ares.analytics.shared.*
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.ui.components.pathplanner.resolveHeading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPreview(val name: String, val trajectory: Trajectory?)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPlannerState(
    /**
     * pathName val.
     */
    val pathName: String = "autonomous_route",
    /**
     * availablePaths val.
     */
    val availablePaths: List<String> = emptyList(),
    /**
     * saveStatus val.
     */
    val saveStatus: String = "",
    /**
     * waypoints val.
     */
    val waypoints: List<Waypoint> = listOf(
        Waypoint(-1.2, -1.2, 0.0),
        Waypoint(0.0, 0.0, Math.toRadians(45.0)),
        Waypoint(1.2, 1.2, Math.toRadians(90.0))
    ),
    /**
     * eventMarkers val.
     */
    val eventMarkers: List<PathPlannerEventMarker> = emptyList(),
    /**
     * rotationTargets val.
     */
    val rotationTargets: List<RotationTarget> = emptyList(),
    /**
     * constraintZones val.
     */
    val constraintZones: List<ConstraintsZone> = emptyList(),
    /**
     * pointTowardsZones val.
     */
    val pointTowardsZones: List<PointTowardsZone> = emptyList(),
    /**
     * globalConstraints val.
     */
    val globalConstraints: PathConstraints = PathConstraints(),
    /**
     * idealStartingState val.
     */
    val idealStartingState: IdealStartingState? = null,
    /**
     * goalEndState val.
     */
    val goalEndState: GoalEndState? = null,
    /**
     * reversed val.
     */
    val reversed: Boolean = false,
    /**
     * useDefaultConstraints val.
     */
    val useDefaultConstraints: Boolean = true,
    /**
     * estimatedDuration val.
     */
    val estimatedDuration: Double = 0.0,
    /**
     * selectedWaypointIndex val.
     */
    val selectedWaypointIndex: Int? = null,
    /**
     * toolMode val.
     */
    val toolMode: String = "Select",
    /**
     * viewRotation val.
     */
    val viewRotation: Float = 0f,
    /**
     * trajectory val.
     */
    val trajectory: Trajectory? = null,
    /**
     * isPlaying val.
     */
    val isPlaying: Boolean = false,
    /**
     * playbackTime val.
     */
    val playbackTime: Double = 0.0,
    
    // Auto Editor specific state
    /**
     * activeEditorMode val.
     */
    val activeEditorMode: String = "Path", // "Path" or "Auto"
    /**
     * availableAutos val.
     */
    val availableAutos: List<String> = emptyList(),
    /**
     * autoStartingPose val.
     */
    val autoStartingPose: AutoStartingPose? = null,
    /**
     * currentAutoCommands val.
     */
    val currentAutoCommands: List<AutoCommandNode> = emptyList(),
    /**
     * contextAutoName val.
     */
    val contextAutoName: String? = null,
    /**
     * contextTrajectory val.
     */
    val contextTrajectory: Trajectory? = null,
    /**
     * contextWaypoints val.
     */
    val contextWaypoints: List<Waypoint> = emptyList(),
    
    // Browser specific state
    /**
     * showBrowser val.
     */
    val showBrowser: Boolean = false,
    /**
     * availablePathPreviews val.
     */
    val availablePathPreviews: List<PathPreview> = emptyList(),
    /**
     * availableAutoPreviews val.
     */
    val availableAutoPreviews: List<PathPreview> = emptyList()
)

sealed class PathPlannerIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadPath(val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class FetchAvailablePaths(val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SavePath(val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class CreateNewPath(val name: String = "new_path") : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class CreateNewAuto(val name: String = "new_auto") : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object OptimizePath : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdatePathName(val name: String) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateEditorMode(val mode: String) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateWaypoints(val newWaypoints: List<Waypoint>) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateWaypoint(val index: Int, val waypoint: Waypoint) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddWaypoint(val waypoint: Waypoint) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteWaypoint(val index: Int) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SelectWaypoint(val index: Int?) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateToolMode(val mode: String) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateGlobalConstraints(val constraints: PathConstraints) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateStartingState(val state: IdealStartingState?) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateEndState(val state: GoalEndState?) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateReversed(val reversed: Boolean) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateUseDefaultConstraints(val useDefault: Boolean) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateViewRotation(val viewRotation: Float) : PathPlannerIntent()
    
    // Browser
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ToggleBrowser : PathPlannerIntent()
    
    // Playback
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object TogglePlayback : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SeekPlayback(val timeSeconds: Double) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object StopPlayback : PathPlannerIntent()
    
    // Event Markers
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddEventMarker(val marker: PathPlannerEventMarker) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateEventMarker(val index: Int, val marker: PathPlannerEventMarker) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateEventMarkers(val markers: List<PathPlannerEventMarker>) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteEventMarker(val index: Int) : PathPlannerIntent()
    
    // Rotation Targets
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddRotationTarget(val target: RotationTarget) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateRotationTarget(val index: Int, val target: RotationTarget) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateRotationTargets(val targets: List<RotationTarget>) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteRotationTarget(val index: Int) : PathPlannerIntent()
    
    // Point Towards Zones
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddPointTowardsZone(val zone: PointTowardsZone) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdatePointTowardsZone(val index: Int, val zone: PointTowardsZone) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeletePointTowardsZone(val index: Int) : PathPlannerIntent()

    // Constraint Zones
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddConstraintZone(val zone: ConstraintsZone) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateConstraintZone(val index: Int, val zone: ConstraintsZone) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteConstraintZone(val index: Int) : PathPlannerIntent()
    
    // Auto Editor
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadAuto(val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveAuto(val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateAutoStartingPose(val pose: AutoStartingPose?) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddAutoCommand(val node: AutoCommandNode, val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class RemoveAutoCommand(val index: Int, val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class MoveAutoCommand(val fromIndex: Int, val direction: Int, val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateAutoCommand(val index: Int, val node: AutoCommandNode, val projectPath: String?, val league: League) : PathPlannerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateContextAuto(val autoName: String?, val projectPath: String?, val league: League) : PathPlannerIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class PathPlannerViewModel(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PathPlannerState())
    /**
     * state val.
     */
    val state: StateFlow<PathPlannerState> = _state.asStateFlow()

    private var playbackJob: kotlinx.coroutines.Job? = null

    private val undoRedoManager = com.ares.analytics.viewmodel.pathing.PathUndoRedoManager(_state)
    private val waypointController = com.ares.analytics.viewmodel.pathing.WaypointController(_state, this::recalculateDuration)
    private val serializationManager = com.ares.analytics.viewmodel.pathing.PathSerializationManager(scope, _state, this::recalculateDuration)

    private fun recalculateDuration() {
        /**
         * s val.
         */
        val s = _state.value
        /**
         * trajectory val.
         */
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

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
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
                    /**
                     * currentlyPlaying val.
                     */
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
                            /**
                             * lastTime var.
                             */
                            var lastTime = System.currentTimeMillis()
                            while (_state.value.isPlaying) {
                                kotlinx.coroutines.delay(16)
                                /**
                                 * now val.
                                 */
                                val now = System.currentTimeMillis()
                                /**
                                 * dt val.
                                 */
                                val dt = (now - lastTime) / 1000.0
                                lastTime = now
                                /**
                                 * nextTime val.
                                 */
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
}
