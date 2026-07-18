package com.ares.analytics.ui.components.pathplanner

data class Waypoint(
    val x: Double,
    val y: Double,
    val headingRad: Double = 0.0,
    val tangentMagnitude: Double = 1.0,
    /** Robot rotation in degrees (holonomic). null = unspecified (planner interpolates or PointTowardsZone applies). */
    val rotationDeg: Double? = null
)
