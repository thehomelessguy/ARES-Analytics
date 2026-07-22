package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class TimeFilter {
    FULL, AUTO, TELEOP, ENABLED
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class AnalysisMode {
    SINGLE, MULTI_NORMAL, ABSOLUTE_ERROR, RELATIVE_ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun StatisticsPanel(
    databaseService: DatabaseService,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    /**
     * availableKeys var.
     */
    var availableKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    /**
     * selectedKey1 var.
     */
    var selectedKey1 by remember { mutableStateOf<String?>(null) }
    /**
     * selectedKey2 var.
     */
    var selectedKey2 by remember { mutableStateOf<String?>(null) }
    /**
     * timeFilter var.
     */
    var timeFilter by remember { mutableStateOf(TimeFilter.FULL) }
    /**
     * analysisMode var.
     */
    var analysisMode by remember { mutableStateOf(AnalysisMode.SINGLE) }

    // Dropdown expanded states
    /**
     * key1Expanded var.
     */
    var key1Expanded by remember { mutableStateOf(false) }
    /**
     * key2Expanded var.
     */
    var key2Expanded by remember { mutableStateOf(false) }
    /**
     * filterExpanded var.
     */
    var filterExpanded by remember { mutableStateOf(false) }
    /**
     * modeExpanded var.
     */
    var modeExpanded by remember { mutableStateOf(false) }

    // Loaded data states
    /**
     * telemetry1 var.
     */
    var telemetry1 by remember { mutableStateOf<List<TelemetryFrame>>(emptyList()) }
    /**
     * telemetry2 var.
     */
    var telemetry2 by remember { mutableStateOf<List<TelemetryFrame>>(emptyList()) }
    /**
     * stateFrames var.
     */
    var stateFrames by remember { mutableStateOf<List<TelemetryFrame>>(emptyList()) }
    /**
     * isLoading var.
     */
    var isLoading by remember { mutableStateOf(false) }

    // Fetch keys when session changes
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            isLoading = true
            scope.launch {
                try {
                    /**
                     * allFrames val.
                     */
                    val allFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
                    availableKeys = allFrames.map { it.key }.distinct().sorted()
                    stateFrames = allFrames.filter { 
                        it.key.contains("state", ignoreCase = true) || 
                        it.key.contains("mode", ignoreCase = true) || 
                        it.key.contains("enabled", ignoreCase = true) 
                    }
                    if (selectedKey1 == null || !availableKeys.contains(selectedKey1)) {
                        selectedKey1 = availableKeys.firstOrNull()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        } else {
            availableKeys = emptyList()
            selectedKey1 = null
            selectedKey2 = null
            telemetry1 = emptyList()
            telemetry2 = emptyList()
        }
    }

    // Fetch telemetry data for selected keys
    LaunchedEffect(sessionId, selectedKey1, selectedKey2) {
        if (sessionId != null && selectedKey1 != null) {
            scope.launch {
                telemetry1 = databaseService.getTelemetryForKey(sessionId, selectedKey1!!)
                telemetry2 = if (selectedKey2 != null) {
                    databaseService.getTelemetryForKey(sessionId, selectedKey2!!)
                } else {
                    emptyList()
                }
            }
        }
    }

    // Determine the active analysis mode
    LaunchedEffect(selectedKey2) {
        if (selectedKey2 == null) {
            analysisMode = AnalysisMode.SINGLE
        } else if (analysisMode == AnalysisMode.SINGLE) {
            analysisMode = AnalysisMode.MULTI_NORMAL
        }
    }

    if (sessionId == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(AresSurface)
                .border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = AresTextTertiary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Select a session to view statistics", color = AresTextTertiary, fontSize = 14.sp)
            }
        }
        return
    }

    // Helper: Interpolation for aligning Signal 2 to Signal 1 timestamps
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun interpolate(t: Long, times: List<Long>, values: List<Double>): Double {
        if (times.isEmpty()) return 0.0
        /**
         * idx val.
         */
        val idx = times.binarySearch(t)
        if (idx >= 0) return values[idx]
        /**
         * insertIdx val.
         */
        val insertIdx = -idx - 1
        if (insertIdx == 0) return values.first()
        if (insertIdx >= times.size) return values.last()
        /**
         * t0 val.
         */
        val t0 = times[insertIdx - 1]
        /**
         * t1 val.
         */
        val t1 = times[insertIdx]
        /**
         * v0 val.
         */
        val v0 = values[insertIdx - 1]
        /**
         * v1 val.
         */
        val v1 = values[insertIdx]
        if (t1 == t0) return v0
        /**
         * pct val.
         */
        val pct = (t - t0).toDouble() / (t1 - t0).toDouble()
        return v0 + pct * (v1 - v0)
    }

    // Filter telemetry based on active TimeFilter
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun filterTelemetry(raw: List<TelemetryFrame>): List<TelemetryFrame> {
        if (raw.isEmpty()) return emptyList()
        /**
         * startMs val.
         */
        val startMs = raw.first().timestampMs
        /**
         * durationMs val.
         */
        val durationMs = raw.last().timestampMs - startMs

        // Check if there is an explicit "/RobotState/Mode" or "/RobotState/Enabled" in telemetry
        /**
         * enabledKey val.
         */
        val enabledKey = stateFrames.firstOrNull { it.key.endsWith("enabled", ignoreCase = true) }?.key
        /**
         * modeKey val.
         */
        val modeKey = stateFrames.firstOrNull { it.key.endsWith("mode", ignoreCase = true) }?.key

        return when (timeFilter) {
            TimeFilter.FULL -> raw
            TimeFilter.AUTO -> {
                if (modeKey != null) {
                    /**
                     * modeValues val.
                     */
                    val modeValues = stateFrames.filter { it.key == modeKey }
                    raw.filter { frame ->
                        /**
                         * activeMode val.
                         */
                        val activeMode = interpolate(frame.timestampMs, modeValues.map { it.timestampMs }, modeValues.map { it.value })
                        activeMode == 1.0 // Heuristic: 1.0 is Auto
                    }
                } else {
                    // Fallback to first 15 seconds
                    /**
                     * cutoff val.
                     */
                    val cutoff = startMs + 15000L
                    raw.filter { it.timestampMs <= cutoff }
                }
            }
            TimeFilter.TELEOP -> {
                if (modeKey != null) {
                    /**
                     * modeValues val.
                     */
                    val modeValues = stateFrames.filter { it.key == modeKey }
                    raw.filter { frame ->
                        /**
                         * activeMode val.
                         */
                        val activeMode = interpolate(frame.timestampMs, modeValues.map { it.timestampMs }, modeValues.map { it.value })
                        activeMode == 2.0 // Heuristic: 2.0 is Teleop
                    }
                } else {
                    // Fallback to post-15 seconds
                    /**
                     * cutoff val.
                     */
                    val cutoff = startMs + 15000L
                    raw.filter { it.timestampMs > cutoff }
                }
            }
            TimeFilter.ENABLED -> {
                if (enabledKey != null) {
                    /**
                     * enabledValues val.
                     */
                    val enabledValues = stateFrames.filter { it.key == enabledKey }
                    raw.filter { frame ->
                        /**
                         * isEnabled val.
                         */
                        val isEnabled = interpolate(frame.timestampMs, enabledValues.map { it.timestampMs }, enabledValues.map { it.value })
                        isEnabled > 0.5
                    }
                } else {
                    raw // Fallback to all if no enabled key
                }
            }
        }
    }

    // Processed primary signal values
    /**
     * filtered1 val.
     */
    val filtered1 = filterTelemetry(telemetry1)
    /**
     * filtered2 val.
     */
    val filtered2 = filterTelemetry(telemetry2)

    // Compute active dataset to analyze based on mode
    /**
     * activeValues val.
     */
    val activeValues = remember(filtered1, filtered2, analysisMode) {
        /**
         * list val.
         */
        val list = mutableListOf<Double>()
        if (analysisMode == AnalysisMode.SINGLE || filtered2.isEmpty()) {
            list.addAll(filtered1.map { it.value })
        } else {
            /**
             * t2List val.
             */
            val t2List = filtered2.map { it.timestampMs }
            /**
             * v2List val.
             */
            val v2List = filtered2.map { it.value }
            for (f1 in filtered1) {
                /**
                 * val1 val.
                 */
                val val1 = f1.value
                /**
                 * val2 val.
                 */
                val val2 = interpolate(f1.timestampMs, t2List, v2List)
                when (analysisMode) {
                    AnalysisMode.ABSOLUTE_ERROR -> list.add(abs(val1 - val2))
                    AnalysisMode.RELATIVE_ERROR -> list.add(abs(val1 - val2) / max(abs(val2), 1e-6))
                    else -> list.add(val1) // Fallback normal comparison
                }
            }
        }
        list
    }

    // Compute Descriptive Statistics
    /**
     * stats val.
     */
    val stats = remember(activeValues) {
        if (activeValues.isEmpty()) null else {
            /**
             * sorted val.
             */
            val sorted = activeValues.sorted()
            /**
             * n val.
             */
            val n = sorted.size
            /**
             * minVal val.
             */
            val minVal = sorted.first()
            /**
             * maxVal val.
             */
            val maxVal = sorted.last()
            /**
             * sumVal val.
             */
            val sumVal = sorted.sum()
            /**
             * meanVal val.
             */
            val meanVal = sumVal / n

            // Median
            /**
             * medianVal val.
             */
            val medianVal = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0

            // Mode
            /**
             * counts val.
             */
            val counts = mutableMapOf<Double, Int>()
            sorted.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            /**
             * modeVal val.
             */
            val modeVal = counts.maxByOrNull { it.value }?.key ?: 0.0

            // StdDev
            /**
             * variance val.
             */
            val variance = sorted.fold(0.0) { acc, d -> acc + (d - meanVal).pow(2) } / n
            /**
             * stdDevVal val.
             */
            val stdDevVal = sqrt(variance)

            // Percentiles
            /**
             * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
             * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
             * Canvas-to-field coordinate transformation conventions applied where relevant.
             *
             * @param args relevant arguments
             * @return expected results
             */
            fun getPercentile(p: Double): Double {
                /**
                 * index val.
                 */
                val index = (p * (n - 1))
                /**
                 * lower val.
                 */
                val lower = index.toInt()
                /**
                 * upper val.
                 */
                val upper = (lower + 1).coerceAtMost(n - 1)
                /**
                 * weight val.
                 */
                val weight = index - lower
                return sorted[lower] * (1.0 - weight) + sorted[upper] * weight
            }

            /**
             * p5 val.
             */
            val p5 = getPercentile(0.05)
            /**
             * p25 val.
             */
            val p25 = getPercentile(0.25)
            /**
             * p50 val.
             */
            val p50 = medianVal
            /**
             * p75 val.
             */
            val p75 = getPercentile(0.75)
            /**
             * p95 val.
             */
            val p95 = getPercentile(0.95)
            /**
             * iqr val.
             */
            val iqr = p75 - p25

            // Skewness
            /**
             * m3 val.
             */
            val m3 = sorted.fold(0.0) { acc, d -> acc + (d - meanVal).pow(3) } / n
            /**
             * skewnessVal val.
             */
            val skewnessVal = if (stdDevVal > 1e-9) m3 / stdDevVal.pow(3) else 0.0

            mapOf(
                "Count" to n.toDouble(),
                "Min" to minVal,
                "Max" to maxVal,
                "Mean" to meanVal,
                "Median" to medianVal,
                "Mode" to modeVal,
                "Std Dev" to stdDevVal,
                "IQR" to iqr,
                "Skewness" to skewnessVal,
                "5th %" to p5,
                "25th %" to p25,
                "75th %" to p75,
                "95th %" to p95
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal 1 selector
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { key1Expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(AresBorder))
                ) {
                    Text(selectedKey1?.substringAfterLast("/") ?: "Select Signal A", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = key1Expanded,
                    onDismissRequest = { key1Expanded = false },
                    modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder).width(240.dp)
                ) {
                    availableKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key, color = AresTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                selectedKey1 = key
                                key1Expanded = false
                            }
                        )
                    }
                }
            }

            // Signal 2 selector
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { key2Expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(AresBorder))
                ) {
                    Text(selectedKey2?.substringAfterLast("/") ?: "Compare with B (None)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = key2Expanded,
                    onDismissRequest = { key2Expanded = false },
                    modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder).width(240.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("None (Single Mode)", color = AresTextSecondary) },
                        onClick = {
                            selectedKey2 = null
                            key2Expanded = false
                        }
                    )
                    availableKeys.filter { it != selectedKey1 }.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key, color = AresTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                selectedKey2 = key
                                key2Expanded = false
                            }
                        )
                    }
                }
            }

            // Analysis Mode (if compare active)
            if (selectedKey2 != null) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { modeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AresCyan),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(AresCyanGlow))
                    ) {
                        Text(analysisMode.name.replace("_", " "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false },
                        modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder)
                    ) {
                        listOf(AnalysisMode.MULTI_NORMAL, AnalysisMode.ABSOLUTE_ERROR, AnalysisMode.RELATIVE_ERROR).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.name.replace("_", " "), color = AresTextPrimary) },
                                onClick = {
                                    analysisMode = m
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Time Filter Selector
            Box {
                OutlinedButton(
                    onClick = { filterExpanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(AresBorder))
                ) {
                    Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timeFilter.name)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                    modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder)
                ) {
                    TimeFilter.values().forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.name, color = AresTextPrimary) },
                            onClick = {
                                timeFilter = filter
                                filterExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Stats Display Workspace
        if (activeValues.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No data points match filters", color = AresTextTertiary)
            }
        } else {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Table of statistics
                Card(
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = AresBackground),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(AresBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (analysisMode == AnalysisMode.SINGLE) "SIGNAL STATISTICS" else "ERROR FORENSICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AresCyan
                        )
                        Spacer(Modifier.height(4.dp))
                        stats?.forEach { (name, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, color = AresTextSecondary, fontSize = 12.sp)
                                /**
                                 * formatted val.
                                 */
                                val formatted = if (name == "Count") value.toInt().toString() else String.format("%.5f", value)
                                Text(formatted, color = AresTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = AresBorder, thickness = 0.5.dp)
                        }
                    }
                }

                // Canvas Histogram
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AresBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    /**
                     * mean val.
                     */
                    val mean = stats?.get("Mean") ?: 0.0
                    /**
                     * median val.
                     */
                    val median = stats?.get("Median") ?: 0.0

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        /**
                         * width val.
                         */
                        val width = size.width
                        /**
                         * height val.
                         */
                        val height = size.height

                        // Bin parameters
                        /**
                         * binCount val.
                         */
                        val binCount = 18
                        /**
                         * minVal val.
                         */
                        val minVal = activeValues.minOrNull() ?: 0.0
                        /**
                         * maxVal val.
                         */
                        val maxVal = activeValues.maxOrNull() ?: 0.0
                        /**
                         * range val.
                         */
                        val range = maxVal - minVal
                        /**
                         * binWidth val.
                         */
                        val binWidth = if (range == 0.0) 1.0 else range / binCount

                        /**
                         * bins val.
                         */
                        val bins = IntArray(binCount)
                        for (v in activeValues) {
                            /**
                             * binIdx var.
                             */
                            var binIdx = if (range == 0.0) 0 else ((v - minVal) / binWidth).toInt()
                            if (binIdx >= binCount) binIdx = binCount - 1
                            if (binIdx < 0) binIdx = 0
                            bins[binIdx]++
                        }

                        /**
                         * maxBinCount val.
                         */
                        val maxBinCount = bins.maxOrNull()?.coerceAtLeast(1) ?: 1

                        // Draw Grid lines
                        /**
                         * gridLines val.
                         */
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            /**
                             * y val.
                             */
                            val y = height - (i * (height - 30.dp.toPx()) / gridLines) - 20.dp.toPx()
                            drawLine(
                                color = AresBorder,
                                start = Offset(40.dp.toPx(), y),
                                end = Offset(width - 10.dp.toPx(), y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Bins
                        /**
                         * startX val.
                         */
                        val startX = 40.dp.toPx()
                        /**
                         * endX val.
                         */
                        val endX = width - 10.dp.toPx()
                        /**
                         * availableWidth val.
                         */
                        val availableWidth = endX - startX
                        /**
                         * barWidth val.
                         */
                        val barWidth = availableWidth / binCount

                        for (i in 0 until binCount) {
                            /**
                             * binHeight val.
                             */
                            val binHeight = (bins[i].toFloat() / maxBinCount) * (height - 50.dp.toPx())
                            /**
                             * x val.
                             */
                            val x = startX + i * barWidth
                            /**
                             * y val.
                             */
                            val y = height - 20.dp.toPx() - binHeight

                            drawRect(
                                color = if (i % 2 == 0) AresCyan.copy(alpha = 0.85f) else AresCyanDark.copy(alpha = 0.85f),
                                topLeft = Offset(x + 2f, y),
                                size = Size(barWidth - 4f, binHeight),
                                style = androidx.compose.ui.graphics.drawscope.Fill
                            )
                        }

                        // Bottom Axis Line
                        drawLine(
                            color = AresTextSecondary,
                            start = Offset(startX, height - 20.dp.toPx()),
                            end = Offset(endX, height - 20.dp.toPx()),
                            strokeWidth = 2f
                        )

                        // Draw ticks and labels (Min, Max, Mean)
                        if (range > 0) {
                            // Mean line
                            /**
                             * meanPct val.
                             */
                            val meanPct = (mean - minVal) / range
                            /**
                             * meanX val.
                             */
                            val meanX = startX + meanPct.toFloat() * availableWidth
                            if (meanX in startX..endX) {
                                drawLine(
                                    color = AresRed,
                                    start = Offset(meanX, 0f),
                                    end = Offset(meanX, height - 20.dp.toPx()),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            // Median line
                            /**
                             * medianPct val.
                             */
                            val medianPct = (median - minVal) / range
                            /**
                             * medianX val.
                             */
                            val medianX = startX + medianPct.toFloat() * availableWidth
                            if (medianX in startX..endX) {
                                drawLine(
                                    color = AresGold,
                                    start = Offset(medianX, 0f),
                                    end = Offset(medianX, height - 20.dp.toPx()),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                            }
                        }
                    }

                    // Legend overlaid
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).background(AresSurfaceElevated.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp, 2.dp).background(AresRed))
                            Text("Mean", color = AresTextPrimary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp, 2.dp).background(AresGold))
                            Text("Median", color = AresTextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
