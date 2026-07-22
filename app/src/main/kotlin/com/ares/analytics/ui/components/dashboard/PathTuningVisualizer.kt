package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.flow.collect
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
fun PathTuningVisualizer(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    
    // Ring buffers for chart data (last 200 points)
    /**
     * maxPoints val.
     */
    val maxPoints = 200
    /**
     * crossTrackHistory val.
     */
    val crossTrackHistory = remember { mutableStateListOf<Double>() }
    /**
     * alongTrackHistory val.
     */
    val alongTrackHistory = remember { mutableStateListOf<Double>() }
    
    /**
     * currentCrossTrack var.
     */
    var currentCrossTrack by remember { mutableStateOf(0.0) }
    /**
     * currentAlongTrack var.
     */
    var currentAlongTrack by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                /**
                 * key val.
                 */
                val key = frame.key
                when {
                    key.endsWith("Path/Error_CrossTrack") -> {
                        /**
                         * value val.
                         */
                        val value = frame.value as? Double ?: 0.0
                        currentCrossTrack = value
                        if (crossTrackHistory.size >= maxPoints) crossTrackHistory.removeAt(0)
                        crossTrackHistory.add(value)
                    }
                    key.endsWith("Path/Error_AlongTrack") -> {
                        /**
                         * value val.
                         */
                        val value = frame.value as? Double ?: 0.0
                        currentAlongTrack = value
                        if (alongTrackHistory.size >= maxPoints) alongTrackHistory.removeAt(0)
                        alongTrackHistory.add(value)
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(containerColor = AresSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timeline, contentDescription = "Path", tint = AresCyan)
                Spacer(Modifier.width(8.dp))
                Text("Path Tuning Errors", color = AresTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                
                Spacer(Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AresBackground).border(1.dp, AresBorder, RoundedCornerShape(8.dp))) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    /**
                     * w val.
                     */
                    val w = size.width
                    /**
                     * h val.
                     */
                    val h = size.height
                    
                    // Draw center zero-line
                    drawLine(color = AresBorder, start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1f)
                    
                    if (crossTrackHistory.isEmpty()) return@Canvas
                    
                    /**
                     * pointSpacing val.
                     */
                    val pointSpacing = w / (maxPoints - 1)
                    /**
                     * maxAbsValue val.
                     */
                    val maxAbsValue = 0.5f // Scale +/- 0.5 meters
                    
                    /**
                     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
                     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
                     * Canvas-to-field coordinate transformation conventions applied where relevant.
                     *
                     * @param args relevant arguments
                     * @return expected results
                     */
                    fun drawLineChart(data: List<Double>, color: Color, stroke: Float = 2f) {
                        /**
                         * path val.
                         */
                        val path = Path()
                        data.forEachIndexed { index, value ->
                            /**
                             * x val.
                             */
                            val x = index * pointSpacing
                            /**
                             * normalizedY val.
                             */
                            val normalizedY = (value.toFloat() / maxAbsValue).coerceIn(-1f, 1f)
                            /**
                             * y val.
                             */
                            val y = h / 2f - (normalizedY * (h / 2f))
                            
                            if (index == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)
                        }
                        drawPath(path, color, style = Stroke(width = stroke))
                    }
                    
                    drawLineChart(crossTrackHistory, AresCyan, 2f)
                    drawLineChart(alongTrackHistory, AresGold, 2f)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(AresCyan, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Cross-Track (m)", color = AresTextSecondary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(AresGold, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Along-Track (m)", color = AresTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
