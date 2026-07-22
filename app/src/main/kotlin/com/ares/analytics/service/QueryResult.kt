package com.ares.analytics.service

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class QueryResult(
    /**
     * columns val.
     */
    val columns: List<String>,
    /**
     * rows val.
     */
    val rows: List<List<String>>
)
