package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch

@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun BatteryHealthCard(
    databaseService: DatabaseService,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    /**
     * voltageFrames var.
     */
    var voltageFrames by remember { mutableStateOf<List<TelemetryFrame>>(emptyList()) }

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            while (isActive) {
                /**
                 * allTelemetry val.
                 */
                val allTelemetry = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
                voltageFrames = allTelemetry.filter { 
                    /**
                     * lower val.
                     */
                    val lower = it.key.lowercase()
                    lower.contains("voltage") || lower.contains("battery")
                }
                if (sessionId != "live-telemetry") break
                delay(1000)
            }
        } else {
            voltageFrames = emptyList()
        }
    }

    /**
     * latestVoltage val.
     */
    val latestVoltage = voltageFrames.lastOrNull()?.value ?: 12.0
    /**
     * minVoltage val.
     */
    val minVoltage = voltageFrames.minOfOrNull { it.value } ?: 12.0

    /**
     * statusColor val.
     */
    val statusColor = when {
        latestVoltage < 11.5 -> AresError
        latestVoltage < 12.5 -> AresAmber
        else -> AresCyan
    }

    /**
     * statusText val.
     */
    val statusText = when {
        latestVoltage < 11.5 -> "CRITICAL BROWNOUT RISK"
        latestVoltage < 12.5 -> "Warning: Voltage Dropping"
        else -> "Healthy State"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (latestVoltage < 11.5) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = statusColor
            )
            Text(
                "Battery Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AresTextPrimary
            )
        }

        HorizontalDivider(color = AresBorder, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LATEST VOLTAGE", fontSize = 10.sp, color = AresTextTertiary)
                Text(
                    String.format("%.2f V", latestVoltage),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("MINIMUM LOGGED", fontSize = 10.sp, color = AresTextTertiary)
                Text(
                    String.format("%.2f V", minVoltage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AresTextSecondary
                )
            }
        }

        Text(
            statusText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        if (voltageFrames.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No battery voltage logged for this session.",
                    color = AresTextTertiary,
                    fontSize = 11.sp
                )
            }
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                BatteryVoltageChart(frames = voltageFrames.sortedBy { it.timestampMs }, statusColor = statusColor)
            }
        }
    }
}

@Composable
private fun BatteryVoltageChart(frames: List<TelemetryFrame>, statusColor: Color) {
    if (frames.size < 2) return

    /**
     * minTime val.
     */
    val minTime = frames.first().timestampMs.toDouble()
    /**
     * maxTime val.
     */
    val maxTime = frames.last().timestampMs.toDouble()
    /**
     * timeRange val.
     */
    val timeRange = (maxTime - minTime).coerceAtLeast(1.0)

    /**
     * minVal val.
     */
    val minVal = (frames.minOf { it.value } - 0.5).coerceAtLeast(0.0)
    /**
     * maxVal val.
     */
    val maxVal = (frames.maxOf { it.value } + 0.5).coerceAtLeast(14.0)
    /**
     * valRange val.
     */
    val valRange = (maxVal - minVal).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, AresBorder, RoundedCornerShape(6.dp))
            .background(AresBackground)
    ) {
        /**
         * w val.
         */
        val w = size.width
        /**
         * h val.
         */
        val h = size.height

        // Draw horizontal grid lines
        /**
         * linesCount val.
         */
        val linesCount = 4
        for (i in 1 until linesCount) {
            /**
             * y val.
             */
            val y = h * (i.toFloat() / linesCount)
            drawLine(color = AresBorder, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        }

        /**
         * splinePath val.
         */
        val splinePath = Path()
        /**
         * fillPath val.
         */
        val fillPath = Path()

        /**
         * firstFrame val.
         */
        val firstFrame = frames.first()
        /**
         * firstX val.
         */
        val firstX = ((firstFrame.timestampMs - minTime) / timeRange * w).toFloat()
        /**
         * firstY val.
         */
        val firstY = (h - ((firstFrame.value - minVal) / valRange * h)).toFloat().coerceIn(0f, h)

        splinePath.moveTo(firstX, firstY)
        fillPath.moveTo(firstX, h)
        fillPath.lineTo(firstX, firstY)

        for (i in 1 until frames.size) {
            /**
             * f val.
             */
            val f = frames[i]
            /**
             * x val.
             */
            val x = ((f.timestampMs - minTime) / timeRange * w).toFloat()
            /**
             * y val.
             */
            val y = (h - ((f.value - minVal) / valRange * h)).toFloat().coerceIn(0f, h)
            splinePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(statusColor.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = splinePath,
            color = statusColor,
            style = Stroke(width = 2f)
        )
    }
}

