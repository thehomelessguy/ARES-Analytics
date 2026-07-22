package com.ares.analytics.service.log

import com.ares.analytics.shared.TelemetryFrame

/**
 * Encapsulates topic name mapping, signal normalization, and NT4 framing.
 */
class TelemetryTopicExtractor {

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun extractTopics(frame: TelemetryFrame): TelemetryFrame {
        // TODO: Normalize topics, handle NetworkTables 4.0 mapping, etc.
        return frame
    }
}
