package com.ares.analytics.shared

import kotlinx.serialization.json.Json

/**
 * AppJson val.
 */
val AppJson = Json { ignoreUnknownKeys = true }
/**
 * AppJsonPretty val.
 */
val AppJsonPretty = Json { ignoreUnknownKeys = true; prettyPrint = true }
