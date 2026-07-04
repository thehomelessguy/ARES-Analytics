package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun SystemHealthCard(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var loopTimeMs by remember { mutableStateOf<Double?>(null) }
    var cpuUsage by remember { mutableStateOf<Double?>(null) }
    var ramUsage by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                val key = frame.key.lowercase()
                val value = frame.value as? Double ?: return@collect
                
                if (key.contains("looptime") || key.contains("loop_time")) {
                    loopTimeMs = value
                } else if (key.contains("cpu")) {
                    cpuUsage = value
                } else if (key.contains("ram") || key.contains("memory")) {
                    ramUsage = value
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AresSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "System Health",
                    tint = AresGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RoboRIO / Control Hub Health", color = AresTextPrimary, fontWeight = FontWeight.Bold)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = AresBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Loop Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOOP TIME", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val hz = if (loopTimeMs != null && loopTimeMs!! > 0) 1000.0 / loopTimeMs!! else 0.0
                    val loopColor = if (hz < 35.0) AresError else if (hz < 45.0) AresGold else AresGreen
                    Text(
                        text = loopTimeMs?.let { String.format("%.1f ms", it) } ?: "--",
                        color = loopColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (loopTimeMs != null) String.format("(%.0f Hz)", hz) else "",
                        color = loopColor,
                        fontSize = 12.sp
                    )
                }
                
                // CPU
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CPU USAGE", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val cpuColor = if (cpuUsage != null && cpuUsage!! > 85.0) AresError else AresTextPrimary
                    Text(
                        text = cpuUsage?.let { String.format("%.1f %%", it) } ?: "--",
                        color = cpuColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // RAM
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RAM USAGE", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val ramColor = if (ramUsage != null && ramUsage!! > 85.0) AresGold else AresTextPrimary
                    Text(
                        text = ramUsage?.let { String.format("%.1f %%", it) } ?: "--",
                        color = ramColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
