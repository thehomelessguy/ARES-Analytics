package com.ares.analytics.shared

import kotlinx.serialization.Serializable

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPlannerWaypoint(
    /**
     * anchor val.
     */
    val anchor: PathPoint,
    /**
     * prevControl val.
     */
    val prevControl: PathPoint?,
    /**
     * nextControl val.
     */
    val nextControl: PathPoint?,
    /**
     * isLocked val.
     */
    val isLocked: Boolean = false,
    /**
     * linkedName val.
     */
    val linkedName: String? = null
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPlannerCommand(
    /**
     * type val.
     */
    val type: String = "named",
    /**
     * name val.
     */
    val name: String
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPlannerEventMarker(
    /**
     * name val.
     */
    val name: String,
    /**
     * waypointRelativePos val.
     */
    val waypointRelativePos: Double,
    /**
     * endWaypointRelativePos val.
     */
    val endWaypointRelativePos: Double? = null,
    /**
     * command val.
     */
    val command: PathPlannerCommand?
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathConstraints(
    /**
     * maxVelocity val.
     */
    val maxVelocity: Double = 3.0,
    /**
     * maxAcceleration val.
     */
    val maxAcceleration: Double = 3.0,
    /**
     * maxAngularVelocity val.
     */
    val maxAngularVelocity: Double = 540.0,
    /**
     * maxAngularAcceleration val.
     */
    val maxAngularAcceleration: Double = 720.0,
    /**
     * nominalVoltage val.
     */
    val nominalVoltage: Double = 12.0,
    /**
     * unlimited val.
     */
    val unlimited: Boolean = false
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class GoalEndState(
    /**
     * velocity val.
     */
    val velocity: Double = 0.0,
    /**
     * rotation val.
     */
    val rotation: Double = 0.0
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class IdealStartingState(
    /**
     * velocity val.
     */
    val velocity: Double = 0.0,
    /**
     * rotation val.
     */
    val rotation: Double = 0.0
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class RotationTarget(
    /**
     * waypointRelativePos val.
     */
    val waypointRelativePos: Double,
    /**
     * rotationDegrees val.
     */
    val rotationDegrees: Double
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class ConstraintsZone(
    /**
     * name val.
     */
    val name: String = "Constraints Zone",
    /**
     * minWaypointRelativePos val.
     */
    val minWaypointRelativePos: Double,
    /**
     * maxWaypointRelativePos val.
     */
    val maxWaypointRelativePos: Double,
    /**
     * constraints val.
     */
    val constraints: PathConstraints
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PointTowardsZone(
    /**
     * name val.
     */
    val name: String = "Point Towards Zone",
    /**
     * fieldPosition val.
     */
    val fieldPosition: PathPoint,
    /**
     * rotationOffset val.
     */
    val rotationOffset: Double = 0.0,
    /**
     * minWaypointRelativePos val.
     */
    val minWaypointRelativePos: Double,
    /**
     * maxWaypointRelativePos val.
     */
    val maxWaypointRelativePos: Double
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPlannerFile(
    /**
     * version val.
     */
    val version: String = "2025.0",
    /**
     * waypoints val.
     */
    val waypoints: List<PathPlannerWaypoint>,
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
     * eventMarkers val.
     */
    val eventMarkers: List<PathPlannerEventMarker> = emptyList(),
    /**
     * globalConstraints val.
     */
    val globalConstraints: PathConstraints = PathConstraints(),
    /**
     * goalEndState val.
     */
    val goalEndState: GoalEndState? = null,
    /**
     * idealStartingState val.
     */
    val idealStartingState: IdealStartingState? = null,
    /**
     * reversed val.
     */
    val reversed: Boolean = false,
    /**
     * useDefaultConstraints val.
     */
    val useDefaultConstraints: Boolean = true
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AutoCommandNode(
    /**
     * type val.
     */
    val type: String,
    /**
     * data val.
     */
    val data: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap())
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AutoFile(
    /**
     * version val.
     */
    val version: String = "1.0",
    /**
     * startingPose val.
     */
    val startingPose: AutoStartingPose? = null,
    /**
     * command val.
     */
    val command: AutoCommandNode
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AutoStartingPose(
    /**
     * position val.
     */
    val position: AutoPosition,
    /**
     * rotation val.
     */
    val rotation: Double
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AutoPosition(
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double
)
