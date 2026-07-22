package com.ares.analytics.viewmodel.pathing

import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.viewmodel.PathPlannerIntent
import com.ares.analytics.viewmodel.PathPlannerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.hypot

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class WaypointController(
    private val stateFlow: MutableStateFlow<PathPlannerState>,
    private val onWaypointsChanged: () -> Unit
) {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun handleIntent(intent: PathPlannerIntent): Boolean {
        when (intent) {
            is PathPlannerIntent.UpdateWaypoints -> {
                stateFlow.update { it.copy(waypoints = intent.newWaypoints) }
                onWaypointsChanged()
                return true
            }
            is PathPlannerIntent.UpdateWaypoint -> {
                /**
                 * updated val.
                 */
                val updated = stateFlow.value.waypoints.toMutableList().apply { set(intent.index, intent.waypoint) }
                stateFlow.update { it.copy(waypoints = updated) }
                onWaypointsChanged()
                return true
            }
            is PathPlannerIntent.AddWaypoint -> {
                /**
                 * updated val.
                 */
                val updated = stateFlow.value.waypoints.toMutableList().apply { add(intent.waypoint) }
                stateFlow.update { it.copy(waypoints = updated) }
                onWaypointsChanged()
                return true
            }
            is PathPlannerIntent.DeleteWaypoint -> {
                /**
                 * updated val.
                 */
                val updated = stateFlow.value.waypoints.toMutableList().apply { removeAt(intent.index) }
                stateFlow.update { it.copy(waypoints = updated) }
                onWaypointsChanged()
                return true
            }
            is PathPlannerIntent.SelectWaypoint -> {
                stateFlow.update { it.copy(selectedWaypointIndex = intent.index) }
                return true
            }
            is PathPlannerIntent.OptimizePath -> {
                /**
                 * wps val.
                 */
                val wps = stateFlow.value.waypoints
                if (wps.size >= 2) {
                    /**
                     * optimized val.
                     */
                    val optimized = wps.mapIndexed { i, wp ->
                        /**
                         * prevDist var.
                         */
                        var prevDist = 0.5
                        /**
                         * nextDist var.
                         */
                        var nextDist = 0.5
                        if (i > 0) {
                            /**
                             * prevWp val.
                             */
                            val prevWp = wps[i - 1]
                            prevDist = hypot(wp.x - prevWp.x, wp.y - prevWp.y) * 0.4
                        }
                        if (i < wps.size - 1) {
                            /**
                             * nextWp val.
                             */
                            val nextWp = wps[i + 1]
                            nextDist = hypot(nextWp.x - wp.x, nextWp.y - wp.y) * 0.4
                        }
                        wp.copy(prevControlLength = prevDist, nextControlLength = nextDist)
                    }
                    stateFlow.update { it.copy(waypoints = optimized) }
                    onWaypointsChanged()
                }
                return true
            }
            else -> return false
        }
    }
}
