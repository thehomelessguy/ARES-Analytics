package com.ares.analytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.ares.analytics.service.ReplayState
import com.ares.analytics.shared.League
import com.ares.analytics.ui.components.dashboard.JoystickVisualizer
import com.ares.analytics.ui.components.dashboard.MechanismVisualizer
import com.ares.analytics.ui.components.dashboard.SwerveVisualizer
import com.ares.analytics.ui.components.dashboard.MecanumVisualizer
import com.ares.analytics.ui.components.pathplanner.FieldCanvas
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.ReplayIntent
import com.ares.analytics.viewmodel.ReplayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReplayScreen(
    viewModel: ReplayViewModel,
    services: com.ares.analytics.di.ServiceRegistry,
    sessionId: String?,
    league: League,
    projectPath: String? = null
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(ReplayIntent.LoadSession(sessionId))
    }

    if (sessionId == null) {
        Box(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(AresSurface).border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Select and load a session in Dashboard index first.", color = AresTextTertiary)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Panel: Controls + Stats
        Surface(
            modifier = Modifier.width(360.dp).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Log Replay Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                Divider(color = AresBorder)

                // Scrubbing Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("0%", fontSize = 11.sp, color = AresTextSecondary)
                    Slider(
                        value = state.progress.toFloat(),
                        onValueChange = { viewModel.onIntent(ReplayIntent.ScrubTo(it.toDouble())) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
                    )
                    Text("100%", fontSize = 11.sp, color = AresTextSecondary)
                }

                // Transport Buttons & Speed multiplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { viewModel.onIntent(ReplayIntent.StepBackward) }) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Step Backward", tint = AresTextPrimary)
                        }
                        if (state.replayState == ReplayState.PLAYING) {
                            IconButton(onClick = { viewModel.onIntent(ReplayIntent.Pause) }) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = AresCyan, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            IconButton(onClick = { viewModel.onIntent(ReplayIntent.Play) }) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = AresCyan, modifier = Modifier.size(28.dp))
                            }
                        }
                        IconButton(onClick = { viewModel.onIntent(ReplayIntent.StepForward) }) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Step Forward", tint = AresTextPrimary)
                        }
                        IconButton(onClick = { viewModel.onIntent(ReplayIntent.Stop) }) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = AresError)
                        }
                    }

                    // Speed dropdown
                    var speedExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { speedExpanded = true }) {
                            Text("${state.speed}x", color = AresCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = speedExpanded,
                            onDismissRequest = { speedExpanded = false },
                            modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder)
                        ) {
                            listOf(0.5, 1.0, 2.0, 5.0).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s}x", color = AresTextPrimary) },
                                    onClick = {
                                        viewModel.onIntent(ReplayIntent.SetSpeed(s))
                                        speedExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Live values list
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(AresBackground).border(1.dp, AresBorder, RoundedCornerShape(6.dp)).padding(12.dp)
                ) {
                    val frame = state.currentFrame
                    if (frame == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No telemetry data loaded.", color = AresTextTertiary)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(frame.values.entries.toList()) { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AresTextSecondary)
                                    Text(String.format("%.4f", value), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AresTextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Export Log Section
                if (sessionId != null) {
                    var isExporting by remember { mutableStateOf(false) }
                    var exportMessage by remember { mutableStateOf("") }
                    val scope = rememberCoroutineScope()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AresSurfaceElevated),
                        border = BorderStroke(1.dp, AresBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Export Telemetry Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AresTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch(Dispatchers.Main) {
                                            val file = selectSaveFile("Export CSV Table", "csv")
                                            if (file != null) {
                                                isExporting = true
                                                exportMessage = "Exporting to CSV..."
                                                try {
                                                    val keys = services.databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
                                                        .map { it.key }.distinct().sorted()
                                                    services.exportService.exportToCsvTable(sessionId, keys, file)
                                                    exportMessage = "CSV Export Succeeded!"
                                                } catch (e: Exception) {
                                                    exportMessage = "Export Failed: ${e.message}"
                                                } finally {
                                                    isExporting = false
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = !isExporting
                                ) {
                                    Text("Export CSV", color = AresBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch(Dispatchers.Main) {
                                            val file = selectSaveFile("Export WPILOG", "wpilog")
                                            if (file != null) {
                                                isExporting = true
                                                exportMessage = "Exporting to WPILOG..."
                                                try {
                                                    val keys = services.databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
                                                        .map { it.key }.distinct().sorted()
                                                    services.exportService.exportToWpiLog(sessionId, keys, file)
                                                    exportMessage = "WPILOG Export Succeeded!"
                                                } catch (e: Exception) {
                                                    exportMessage = "Export Failed: ${e.message}"
                                                } finally {
                                                    isExporting = false
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AresGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = !isExporting
                                ) {
                                    Text("Export WPILOG", color = AresBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (exportMessage.isNotEmpty()) {
                                Text(
                                    text = exportMessage,
                                    color = if (exportMessage.contains("Succeeded")) AresGreen else if (exportMessage.contains("Failed")) AresRed else AresTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Panel: Tabbed Replay Workspace
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        ) {
            val tabs = listOf("Field 2D", "Mechanism Linkage", "Swerve Vector", "Mecanum Vector", "Driver Joystick")

            TabRow(
                selectedTabIndex = state.activeViewportTab,
                containerColor = AresSurface,
                contentColor = AresCyan,
                divider = { Divider(color = AresBorder) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.activeViewportTab == index,
                        onClick = { viewModel.onIntent(ReplayIntent.SelectViewportTab(index)) },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        selectedContentColor = AresCyan,
                        unselectedContentColor = AresTextSecondary
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state.activeViewportTab) {
                    0 -> {
                        val robotX = state.currentFrame?.values?.get("/Drive/Pose_X") ?: 0.0
                        val robotY = state.currentFrame?.values?.get("/Drive/Pose_Y") ?: 0.0
                        val robotHeading = state.currentFrame?.values?.get("/Drive/Pose_Heading") ?: 0.0
                        val currentPose = listOf(Waypoint(robotX, robotY, robotHeading))

                        FieldCanvas(
                            league = league,
                            waypoints = currentPose, // Shows current active robot indicator
                            actualPath = state.actualPath,  // Shows full actual trajectory path
                            onWaypointsChanged = { /* Read only indicator in replay */ },
                            projectPath = projectPath,
                            showPathControls = false,
                            showObstacleControls = false
                        )
                    }
                    1 -> {
                        MechanismVisualizer(currentFrame = state.currentFrame)
                    }
                    2 -> {
                        SwerveVisualizer(currentFrame = state.currentFrame)
                    }
                    3 -> {
                        MecanumVisualizer(currentFrame = state.currentFrame)
                    }
                    4 -> {
                        JoystickVisualizer(currentFrame = state.currentFrame)
                    }
                }
            }
        }
    }
}

private fun selectSaveFile(title: String, extension: String): java.io.File? {
    val frame = java.awt.Frame()
    val dialog = java.awt.FileDialog(frame, title, java.awt.FileDialog.SAVE)
    dialog.file = "*.$extension"
    dialog.isVisible = true
    val file = dialog.file
    val dir = dialog.directory
    dialog.dispose()
    frame.dispose()
    return if (file != null && dir != null) {
        val fullPath = if (file.endsWith(".$extension")) file else "$file.$extension"
        java.io.File(dir, fullPath)
    } else null
}
