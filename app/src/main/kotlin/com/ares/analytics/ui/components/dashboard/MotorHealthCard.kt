package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
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
fun MotorHealthCard(
    databaseService: DatabaseService,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    /**
     * currentFrames var.
     */
    var currentFrames by remember { mutableStateOf<List<TelemetryFrame>>(emptyList()) }
    
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            while (isActive) {
                // Fetch motor current telemetry keys
                /**
                 * allTelemetry val.
                 */
                val allTelemetry = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
                currentFrames = allTelemetry.filter { it.key.lowercase().contains("current") }
                if (sessionId != "live-telemetry") break
                delay(1000)
            }
        } else {
            currentFrames = emptyList()
        }
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
            Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, tint = AresGold)
            Text(
                "Motor Current Draw",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AresTextPrimary
            )
        }

        HorizontalDivider(color = AresBorder, thickness = 1.dp)

        if (currentFrames.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select a session with current telemetry to view motor logs.", color = AresTextTertiary, fontSize = 12.sp)
            }
        } else {
            /**
             * grouped val.
             */
            val grouped = currentFrames.groupBy { it.key }
            
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Render top 2 motor current charts
                grouped.entries.take(2).forEach { (motorKey, frames) ->
                    /**
                     * motorName val.
                     */
                    val motorName = motorKey.split("/").lastOrNull()?.replace("current", "", ignoreCase = true) ?: "Motor"
                    Text(motorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AresTextSecondary)
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        MotorCurrentChart(frames = frames.sortedBy { it.timestampMs })
                    }
                }
            }
        }
    }
}

@Composable
private fun MotorCurrentChart(frames: List<TelemetryFrame>) {
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
     * maxCurrent val.
     */
    val maxCurrent = (frames.maxOf { it.value }).coerceAtLeast(10.0)

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
        val firstY = (h - (firstFrame.value / maxCurrent * h)).toFloat().coerceIn(0f, h)

        splinePath.moveTo(firstX, firstY)
        fillPath.moveTo(firstX, h) // start at bottom left of fill path
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
            val y = (h - (f.value / maxCurrent * h)).toFloat().coerceIn(0f, h)
            splinePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(w, h) // complete fill shape back to bottom right
        fillPath.close()

        // 1. Draw Faded Gradient Fill under the line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(AresGold.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // 2. Draw Current Line
        drawPath(
            path = splinePath,
            color = AresGold,
            style = Stroke(width = 2.5f)
        )
    }
}
