package com.ares.analytics.service.log

import com.ares.analytics.shared.TelemetryFrame

/**
 * Encapsulates topic name mapping, signal normalization, and NT4 framing.
 */
class TelemetryTopicExtractor {

    fun extractTopics(frame: TelemetryFrame): TelemetryFrame {
        // TODO: Normalize topics, handle NetworkTables 4.0 mapping, etc.
        return frame
    }
}
