package com.ares.analytics.ui.components.pathplanner

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class Waypoint(
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double,
    /** Spline tangent heading in radians. null = auto-calculate (point toward next waypoint). */
    val headingRad: Double? = null,
    /**
     * prevControlLength val.
     */
    val prevControlLength: Double = 0.5,
    /**
     * nextControlLength val.
     */
    val nextControlLength: Double = 0.5,
    /** Robot rotation in degrees (holonomic). null = unspecified (planner interpolates or PointTowardsZone applies). */
    val rotationDeg: Double? = null
)

/**
 * Resolves the effective heading for a waypoint. If [Waypoint.headingRad] is non-null, returns it.
 * Otherwise auto-calculates: points from previous toward next waypoint (Catmull-Rom style for
 * intermediate waypoints), giving the most direct path.
 */
fun resolveHeading(waypoints: List<Waypoint>, index: Int): Double {
    /**
     * wp val.
     */
    val wp = waypoints[index]
    if (wp.headingRad != null) return wp.headingRad

    return when {
        waypoints.size <= 1 -> 0.0
        index == 0 -> {
            /**
             * next val.
             */
            val next = waypoints[1]
            kotlin.math.atan2(next.y - wp.y, next.x - wp.x)
        }
        index == waypoints.size - 1 -> {
            /**
             * prev val.
             */
            val prev = waypoints[index - 1]
            kotlin.math.atan2(wp.y - prev.y, wp.x - prev.x)
        }
        else -> {
            // Average direction from prev→this and this→next (Catmull-Rom tangent)
            /**
             * prev val.
             */
            val prev = waypoints[index - 1]
            /**
             * next val.
             */
            val next = waypoints[index + 1]
            /**
             * a1 val.
             */
            val a1 = kotlin.math.atan2(wp.y - prev.y, wp.x - prev.x)
            /**
             * a2 val.
             */
            val a2 = kotlin.math.atan2(next.y - wp.y, next.x - wp.x)
            kotlin.math.atan2(kotlin.math.sin(a1) + kotlin.math.sin(a2), kotlin.math.cos(a1) + kotlin.math.cos(a2))
        }
    }
}
