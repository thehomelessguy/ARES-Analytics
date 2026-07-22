package com.ares.analytics.viewmodel.field

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class FieldCameraGestureController {
    var zoomLevel: Float = 1.0f
    var panOffsetX: Float = 0.0f
    var panOffsetY: Float = 0.0f

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun reset() {
        zoomLevel = 1.0f
        panOffsetX = 0.0f
        panOffsetY = 0.0f
    }
}
