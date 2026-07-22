package com.ares.analytics.service.log

import com.ares.analytics.shared.TelemetryFrame
import com.areslib.telemetry.TelemetryTopicNormalizer

/**
 * Encapsulates topic name mapping, signal normalization, and NT4 framing.
 */
object TelemetryTopicExtractor {

    fun normalizeTopic(key: String): String {
        return TelemetryTopicNormalizer.normalizeTopic(key)
    }

    fun extractTopics(frame: TelemetryFrame): TelemetryFrame {
        return frame.copy(key = normalizeTopic(frame.key))
    }
}
