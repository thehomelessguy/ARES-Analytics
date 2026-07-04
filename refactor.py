import os

base_path = r'c:\Users\david\dev\robotics\ftc\ARES-Analytics\app\src\main\kotlin\com\ares\analytics\ui\components\pathplanner'
screen_path = r'c:\Users\david\dev\robotics\ftc\ARES-Analytics\app\src\main\kotlin\com\ares\analytics\ui\screens\PathPlannerScreen.kt'

with open(screen_path, 'r', encoding='utf-8') as f:
    original_code = f.read()

cards_start = original_code.find('@Composable\nprivate fun WaypointCard')
cards_code = original_code[cards_start:]
cards_code = cards_code.replace('private fun', 'fun')
cards_code = cards_code.replace('internal fun', 'fun')

path_planner_cards = '''package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.theme.*

''' + cards_code

with open(os.path.join(base_path, 'PathPlannerCards.kt'), 'w', encoding='utf-8') as f:
    f.write(path_planner_cards)

toolbar_code = '''package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*

@Composable
fun PlannerToolbar(
    estimatedDuration: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Path Planner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = AresCyan.copy(alpha = 0.2f)
            ) {
                Text(
                    text = String.format("~%.2fs", estimatedDuration),
                    color = AresCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        HorizontalDivider(color = AresBorder, modifier = Modifier.padding(vertical = 8.dp))
    }
}
'''
with open(os.path.join(base_path, 'PlannerToolbar.kt'), 'w', encoding='utf-8') as f:
    f.write(toolbar_code)

action_bar_code = '''package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerActionBar(
    pathName: String,
    availablePaths: List<String>,
    saveStatus: String,
    onPathNameChange: (String) -> Unit,
    onPathSelected: (String) -> Unit,
    onCreateNewPath: () -> Unit,
    onSavePath: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Path Name", fontSize = 11.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = pathName,
                onValueChange = onPathNameChange,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availablePaths.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p) },
                        onClick = {
                            onPathSelected(p)
                            expanded = false
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCreateNewPath,
                colors = ButtonDefaults.buttonColors(containerColor = AresSurface, contentColor = AresTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, AresBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("New Path", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSavePath,
                colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Path", color = AresBackground, fontWeight = FontWeight.Bold)
            }
        }
        if (saveStatus.isNotEmpty()) {
            Text(
                text = saveStatus,
                color = if (saveStatus.contains("failed") || saveStatus.contains("No")) AresError else AresGreen,
                fontSize = 11.sp
            )
        }
        HorizontalDivider(color = AresBorder, modifier = Modifier.padding(vertical = 8.dp))
    }
}
'''
with open(os.path.join(base_path, 'PlannerActionBar.kt'), 'w', encoding='utf-8') as f:
    f.write(action_bar_code)

panel_code = '''package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.PathPlannerIntent
import com.ares.analytics.viewmodel.PathPlannerState

@Composable
fun WaypointEditorPanel(
    state: PathPlannerState,
    projectPath: String?,
    league: League,
    onIntent: (PathPlannerIntent) -> Unit
) {
    Surface(
        modifier = Modifier.width(360.dp).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = AresSurface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PlannerToolbar(estimatedDuration = state.estimatedDuration)
            }
            item {
                PlannerActionBar(
                    pathName = state.pathName,
                    availablePaths = state.availablePaths,
                    saveStatus = state.saveStatus,
                    onPathNameChange = { onIntent(PathPlannerIntent.UpdatePathName(it)) },
                    onPathSelected = { 
                        onIntent(PathPlannerIntent.UpdatePathName(it))
                        onIntent(PathPlannerIntent.LoadPath(projectPath, league))
                    },
                    onCreateNewPath = { onIntent(PathPlannerIntent.CreateNewPath()) },
                    onSavePath = { onIntent(PathPlannerIntent.SavePath(projectPath, league)) }
                )
            }
            
            // WAYPOINTS
            item {
                CollapsibleSection(title = "Waypoints", badgeCount = state.waypoints.size) {
                    state.waypoints.forEachIndexed { idx, wp ->
                        WaypointCard(
                            idx = idx,
                            wp = wp,
                            onChanged = { updatedWp ->
                                onIntent(PathPlannerIntent.UpdateWaypoint(idx, updatedWp))
                            },
                            onDelete = {
                                onIntent(PathPlannerIntent.DeleteWaypoint(idx))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val last = state.waypoints.lastOrNull() ?: Waypoint(0.0, 0.0)
                            onIntent(PathPlannerIntent.AddWaypoint(Waypoint(last.x + 0.3, last.y + 0.3, last.headingRad)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                    ) {
                        Text("Add Waypoint", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EVENT MARKERS
            item {
                CollapsibleSection(title = "Event Markers", badgeCount = state.eventMarkers.size) {
                    val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                    state.eventMarkers.forEachIndexed { idx, marker ->
                        EventMarkerCard(
                            idx = idx,
                            marker = marker,
                            maxPos = maxPos,
                            onChanged = { updatedMarker ->
                                onIntent(PathPlannerIntent.UpdateEventMarker(idx, updatedMarker))
                            },
                            onDelete = {
                                onIntent(PathPlannerIntent.DeleteEventMarker(idx))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onIntent(
                                PathPlannerIntent.AddEventMarker(
                                    PathPlannerEventMarker(
                                        name = "event_${state.eventMarkers.size + 1}",
                                        waypointRelativePos = 0.0,
                                        command = PathPlannerCommand(name = "event_${state.eventMarkers.size + 1}")
                                    )
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                    ) {
                        Text("Add Event Marker", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ROTATION TARGETS
            item {
                CollapsibleSection(title = "Rotation Targets", badgeCount = state.rotationTargets.size) {
                    val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                    state.rotationTargets.forEachIndexed { idx, target ->
                        RotationTargetCard(
                            idx = idx,
                            target = target,
                            maxPos = maxPos,
                            onChanged = { updatedTarget ->
                                onIntent(PathPlannerIntent.UpdateRotationTarget(idx, updatedTarget))
                            },
                            onDelete = {
                                onIntent(PathPlannerIntent.DeleteRotationTarget(idx))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onIntent(PathPlannerIntent.AddRotationTarget(RotationTarget(waypointRelativePos = 0.5, rotationDegrees = 0.0)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                    ) {
                        Text("Add Rotation Target", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // POINT TOWARDS ZONES
            item {
                CollapsibleSection(title = "Point Towards Zones", badgeCount = state.pointTowardsZones.size) {
                    val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                    state.pointTowardsZones.forEachIndexed { idx, zone ->
                        PointTowardsZoneCard(
                            idx = idx,
                            zone = zone,
                            maxPos = maxPos,
                            onChanged = { updatedZone ->
                                onIntent(PathPlannerIntent.UpdatePointTowardsZone(idx, updatedZone))
                            },
                            onDelete = {
                                onIntent(PathPlannerIntent.DeletePointTowardsZone(idx))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onIntent(
                                PathPlannerIntent.AddPointTowardsZone(
                                    PointTowardsZone(
                                        name = "Aim Zone ${state.pointTowardsZones.size + 1}",
                                        fieldPosition = PathPoint(2.0, 2.0),
                                        rotationOffset = 0.0,
                                        minWaypointRelativePos = 0.0,
                                        maxWaypointRelativePos = maxPos
                                    )
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                    ) {
                        Text("Add Aim Zone", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // STARTING STATE
            item {
                val startVelText by remember(state.idealStartingState.velocity) { mutableStateOf(String.format("%.2f", state.idealStartingState.velocity)) }
                val startRotText by remember(state.idealStartingState.rotation) { mutableStateOf(String.format("%.1f", state.idealStartingState.rotation)) }
                CollapsibleSection(title = "Ideal Starting State", badgeText = "${startVelText} M/S") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startVelText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateStartingState(state.idealStartingState.copy(velocity = it)))
                                }
                            },
                            label = { Text("Velocity (M/S)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                        OutlinedTextField(
                            value = startRotText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateStartingState(state.idealStartingState.copy(rotation = it)))
                                }
                            },
                            label = { Text("Rotation (Deg)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                    }
                }
            }

            // END STATE
            item {
                val endVelText by remember(state.goalEndState.velocity) { mutableStateOf(String.format("%.2f", state.goalEndState.velocity)) }
                val endRotText by remember(state.goalEndState.rotation) { mutableStateOf(String.format("%.1f", state.goalEndState.rotation)) }
                CollapsibleSection(title = "Goal End State", badgeText = "${endVelText} M/S") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = endVelText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateEndState(state.goalEndState.copy(velocity = it)))
                                }
                            },
                            label = { Text("Velocity (M/S)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                        OutlinedTextField(
                            value = endRotText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateEndState(state.goalEndState.copy(rotation = it)))
                                }
                            },
                            label = { Text("Rotation (Deg)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                    }
                }
            }

            // GLOBAL CONSTRAINTS
            item {
                val globalVelText by remember(state.globalConstraints.maxVelocity) { mutableStateOf(String.format("%.2f", state.globalConstraints.maxVelocity)) }
                val globalAccText by remember(state.globalConstraints.maxAcceleration) { mutableStateOf(String.format("%.2f", state.globalConstraints.maxAcceleration)) }
                val globalAngVelText by remember(state.globalConstraints.maxAngularVelocity) { mutableStateOf(String.format("%.1f", state.globalConstraints.maxAngularVelocity)) }
                val globalAngAccText by remember(state.globalConstraints.maxAngularAcceleration) { mutableStateOf(String.format("%.1f", state.globalConstraints.maxAngularAcceleration)) }
                val globalVoltsText by remember(state.globalConstraints.nominalVoltage) { mutableStateOf(String.format("%.1f", state.globalConstraints.nominalVoltage)) }

                CollapsibleSection(title = "Global Constraints") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = globalVelText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxVelocity = it)))
                                }
                            },
                            label = { Text("Max Vel (M/S)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                        OutlinedTextField(
                            value = globalAccText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAcceleration = it)))
                                }
                            },
                            label = { Text("Max Accel (M/S²)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = globalAngVelText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAngularVelocity = it)))
                                }
                            },
                            label = { Text("Max Ang Vel (°/s)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                        OutlinedTextField(
                            value = globalAngAccText,
                            onValueChange = { newValue ->
                                newValue.toDoubleOrNull()?.let {
                                    onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAngularAcceleration = it)))
                                }
                            },
                            label = { Text("Max Ang Accel (°/s²)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )
                    }

                    OutlinedTextField(
                        value = globalVoltsText,
                        onValueChange = { newValue ->
                            newValue.toDoubleOrNull()?.let {
                                onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(nominalVoltage = it)))
                            }
                        },
                        label = { Text("Nominal Voltage (V)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                    )
                }
            }

            // CONSTRAINT ZONES
            item {
                CollapsibleSection(title = "Constraint Zones", badgeCount = state.constraintZones.size) {
                    val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                    state.constraintZones.forEachIndexed { idx, zone ->
                        ConstraintsZoneCard(
                            idx = idx,
                            zone = zone,
                            maxPos = maxPos,
                            onChanged = { updatedZone ->
                                onIntent(PathPlannerIntent.UpdateConstraintZone(idx, updatedZone))
                            },
                            onDelete = {
                                onIntent(PathPlannerIntent.DeleteConstraintZone(idx))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onIntent(
                                PathPlannerIntent.AddConstraintZone(
                                    ConstraintsZone(
                                        name = "Speed Limit ${state.constraintZones.size + 1}",
                                        minWaypointRelativePos = 0.0,
                                        maxWaypointRelativePos = maxPos,
                                        constraints = PathConstraints(maxVelocity = 1.5, maxAcceleration = 1.5)
                                    )
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                    ) {
                        Text("Add Constraint Zone", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
'''
with open(os.path.join(base_path, 'WaypointEditorPanel.kt'), 'w', encoding='utf-8') as f:
    f.write(panel_code)

screen_code = '''package com.ares.analytics.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.components.pathplanner.*
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.PathPlannerIntent
import com.ares.analytics.viewmodel.PathPlannerViewModel

@Composable
fun PathPlannerScreen(
    viewModel: PathPlannerViewModel,
    league: League,
    projectPath: String? = null
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.pathName, projectPath) {
        viewModel.onIntent(PathPlannerIntent.LoadPath(projectPath, league))
    }

    LaunchedEffect(projectPath, state.saveStatus) {
        viewModel.onIntent(PathPlannerIntent.FetchAvailablePaths(projectPath, league))
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WaypointEditorPanel(
            state = state,
            projectPath = projectPath,
            league = league,
            onIntent = { viewModel.onIntent(it) }
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        ) {
            FieldCanvas(
                league = league,
                waypoints = state.waypoints,
                actualPath = emptyList(),
                onWaypointsChanged = {
                    viewModel.onIntent(PathPlannerIntent.UpdateWaypoints(it))
                },
                projectPath = projectPath,
                showPathControls = false,
                showObstacleControls = false,
                aprilTags = null,
                onAprilTagsChanged = null,
                eventMarkers = state.eventMarkers,
                onEventMarkersChanged = {
                    viewModel.onIntent(PathPlannerIntent.UpdateEventMarkers(it))
                },
                initialViewRotation = state.viewRotation,
                onViewRotationChanged = { newRot ->
                    viewModel.onIntent(PathPlannerIntent.UpdateViewRotation(newRot))
                },
                rotationTargets = state.rotationTargets,
                constraintZones = state.constraintZones,
                pointTowardsZones = state.pointTowardsZones,
                globalConstraints = state.globalConstraints
            )
        }
    }
}
'''
with open(screen_path, 'w', encoding='utf-8') as f:
    f.write(screen_code)

print('Done')
