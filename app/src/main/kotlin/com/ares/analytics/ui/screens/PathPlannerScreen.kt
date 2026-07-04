package com.ares.analytics.ui.screens

import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.components.pathplanner.FieldCanvas
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.PathPlannerIntent
import com.ares.analytics.viewmodel.PathPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
                                text = String.format("~%.2fs", state.estimatedDuration),
                                color = AresCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = AresBorder, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Path Name", fontSize = 11.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = state.pathName,
                                onValueChange = { viewModel.onIntent(PathPlannerIntent.UpdatePathName(it)) },
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
                                state.availablePaths.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            viewModel.onIntent(PathPlannerIntent.UpdatePathName(p))
                                            viewModel.onIntent(PathPlannerIntent.LoadPath(projectPath, league))
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
                                onClick = { viewModel.onIntent(PathPlannerIntent.CreateNewPath()) },
                                colors = ButtonDefaults.buttonColors(containerColor = AresSurface, contentColor = AresTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AresBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("New Path", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.onIntent(PathPlannerIntent.SavePath(projectPath, league)) },
                                colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Path", color = AresBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (state.saveStatus.isNotEmpty()) {
                            Text(
                                text = state.saveStatus,
                                color = if (state.saveStatus.contains("failed") || state.saveStatus.contains("No")) AresError else AresGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                    HorizontalDivider(color = AresBorder, modifier = Modifier.padding(vertical = 8.dp))
                }

                // COLLAPSIBLE: WAYPOINTS
                item {
                    CollapsibleSection(title = "Waypoints", badgeCount = state.waypoints.size) {
                        state.waypoints.forEachIndexed { idx, wp ->
                            WaypointCard(
                                idx = idx,
                                wp = wp,
                                onChanged = { updatedWp ->
                                    viewModel.onIntent(PathPlannerIntent.UpdateWaypoint(idx, updatedWp))
                                },
                                onDelete = {
                                    viewModel.onIntent(PathPlannerIntent.DeleteWaypoint(idx))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val last = state.waypoints.lastOrNull() ?: Waypoint(0.0, 0.0)
                                viewModel.onIntent(PathPlannerIntent.AddWaypoint(Waypoint(last.x + 0.3, last.y + 0.3, last.headingRad)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                        ) {
                            Text("Add Waypoint", color = AresBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // COLLAPSIBLE: EVENT MARKERS
                item {
                    CollapsibleSection(title = "Event Markers", badgeCount = state.eventMarkers.size) {
                        val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                        state.eventMarkers.forEachIndexed { idx, marker ->
                            EventMarkerCard(
                                idx = idx,
                                marker = marker,
                                maxPos = maxPos,
                                onChanged = { updatedMarker ->
                                    viewModel.onIntent(PathPlannerIntent.UpdateEventMarker(idx, updatedMarker))
                                },
                                onDelete = {
                                    viewModel.onIntent(PathPlannerIntent.DeleteEventMarker(idx))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(
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

                // COLLAPSIBLE: ROTATION TARGETS
                item {
                    CollapsibleSection(title = "Rotation Targets", badgeCount = state.rotationTargets.size) {
                        val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                        state.rotationTargets.forEachIndexed { idx, target ->
                            RotationTargetCard(
                                idx = idx,
                                target = target,
                                maxPos = maxPos,
                                onChanged = { updatedTarget ->
                                    viewModel.onIntent(PathPlannerIntent.UpdateRotationTarget(idx, updatedTarget))
                                },
                                onDelete = {
                                    viewModel.onIntent(PathPlannerIntent.DeleteRotationTarget(idx))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(PathPlannerIntent.AddRotationTarget(RotationTarget(waypointRelativePos = 0.5, rotationDegrees = 0.0)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AresCyan)
                        ) {
                            Text("Add Rotation Target", color = AresBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // COLLAPSIBLE: POINT TOWARDS ZONES
                item {
                    CollapsibleSection(title = "Point Towards Zones", badgeCount = state.pointTowardsZones.size) {
                        val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                        state.pointTowardsZones.forEachIndexed { idx, zone ->
                            PointTowardsZoneCard(
                                idx = idx,
                                zone = zone,
                                maxPos = maxPos,
                                onChanged = { updatedZone ->
                                    viewModel.onIntent(PathPlannerIntent.UpdatePointTowardsZone(idx, updatedZone))
                                },
                                onDelete = {
                                    viewModel.onIntent(PathPlannerIntent.DeletePointTowardsZone(idx))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(
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

                // COLLAPSIBLE: STARTING STATE
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateStartingState(state.idealStartingState.copy(velocity = it)))
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateStartingState(state.idealStartingState.copy(rotation = it)))
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

                // COLLAPSIBLE: END STATE
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateEndState(state.goalEndState.copy(velocity = it)))
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateEndState(state.goalEndState.copy(rotation = it)))
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

                // COLLAPSIBLE: GLOBAL CONSTRAINTS
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxVelocity = it)))
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAcceleration = it)))
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAngularVelocity = it)))
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
                                        viewModel.onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(maxAngularAcceleration = it)))
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
                                    viewModel.onIntent(PathPlannerIntent.UpdateGlobalConstraints(state.globalConstraints.copy(nominalVoltage = it)))
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

                // COLLAPSIBLE: CONSTRAINT ZONES
                item {
                    CollapsibleSection(title = "Constraint Zones", badgeCount = state.constraintZones.size) {
                        val maxPos = (state.waypoints.size - 1).coerceAtLeast(0).toDouble()
                        state.constraintZones.forEachIndexed { idx, zone ->
                            ConstraintsZoneCard(
                                idx = idx,
                                zone = zone,
                                maxPos = maxPos,
                                onChanged = { updatedZone ->
                                    viewModel.onIntent(PathPlannerIntent.UpdateConstraintZone(idx, updatedZone))
                                },
                                onDelete = {
                                    viewModel.onIntent(PathPlannerIntent.DeleteConstraintZone(idx))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(
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

@Composable
private fun WaypointCard(
    idx: Int,
    wp: Waypoint,
    onChanged: (Waypoint) -> Unit,
    onDelete: () -> Unit
) {
    var xText by remember(wp.x) { mutableStateOf(String.format("%.3f", wp.x)) }
    var yText by remember(wp.y) { mutableStateOf(String.format("%.3f", wp.y)) }
    val headingDeg = Math.toDegrees(wp.headingRad)
    var headingText by remember(wp.headingRad) { mutableStateOf(String.format("%.1f", headingDeg)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Waypoint #${idx + 1}", fontSize = 12.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = xText,
                onValueChange = { newValue ->
                    xText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(wp.copy(x = it))
                    }
                },
                label = { Text("X (m)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )

            OutlinedTextField(
                value = yText,
                onValueChange = { newValue ->
                    yText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(wp.copy(y = it))
                    }
                },
                label = { Text("Y (m)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )

            OutlinedTextField(
                value = headingText,
                onValueChange = { newValue ->
                    headingText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(wp.copy(headingRad = Math.toRadians(it)))
                    }
                },
                label = { Text("Heading (°)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )
        }
    }
}

@Composable
private fun EventMarkerCard(
    idx: Int,
    marker: PathPlannerEventMarker,
    maxPos: Double,
    onChanged: (PathPlannerEventMarker) -> Unit,
    onDelete: () -> Unit
) {
    var nameText by remember(marker.name) { mutableStateOf(marker.name) }
    var posSliderVal by remember(marker.waypointRelativePos) { mutableStateOf(marker.waypointRelativePos.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Marker #${idx + 1}: ${marker.name}", fontSize = 12.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = nameText,
            onValueChange = { newValue ->
                nameText = newValue
                onChanged(marker.copy(name = newValue, command = PathPlannerCommand(name = newValue)))
            },
            label = { Text("Event Name", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Position: ${String.format("%.2f", posSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
                Text("Range: 0.0 to ${String.format("%.1f", maxPos)}", fontSize = 11.sp, color = AresTextSecondary)
            }
            Slider(
                value = posSliderVal,
                onValueChange = { newValue ->
                    posSliderVal = newValue
                    onChanged(marker.copy(waypointRelativePos = newValue.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }
    }
}

@Composable
private fun RotationTargetCard(
    idx: Int,
    target: RotationTarget,
    maxPos: Double,
    onChanged: (RotationTarget) -> Unit,
    onDelete: () -> Unit
) {
    var degText by remember(target.rotationDegrees) { mutableStateOf(String.format("%.1f", target.rotationDegrees)) }
    var posSliderVal by remember(target.waypointRelativePos) { mutableStateOf(target.waypointRelativePos.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Target #${idx + 1}", fontSize = 12.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = degText,
            onValueChange = { newValue ->
                degText = newValue
                newValue.toDoubleOrNull()?.let {
                    onChanged(target.copy(rotationDegrees = it))
                }
            },
            label = { Text("Rotation (Deg)", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Position: ${String.format("%.2f", posSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
                Text("Range: 0.0 to ${String.format("%.1f", maxPos)}", fontSize = 11.sp, color = AresTextSecondary)
            }
            Slider(
                value = posSliderVal,
                onValueChange = { newValue ->
                    posSliderVal = newValue
                    onChanged(target.copy(waypointRelativePos = newValue.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }
    }
}

@Composable
private fun ConstraintsZoneCard(
    idx: Int,
    zone: ConstraintsZone,
    maxPos: Double,
    onChanged: (ConstraintsZone) -> Unit,
    onDelete: () -> Unit
) {
    var nameText by remember(zone.name) { mutableStateOf(zone.name) }
    var minPosSliderVal by remember(zone.minWaypointRelativePos) { mutableStateOf(zone.minWaypointRelativePos.toFloat()) }
    var maxPosSliderVal by remember(zone.maxWaypointRelativePos) { mutableStateOf(zone.maxWaypointRelativePos.toFloat()) }

    var velText by remember(zone.constraints.maxVelocity) { mutableStateOf(String.format("%.2f", zone.constraints.maxVelocity)) }
    var accText by remember(zone.constraints.maxAcceleration) { mutableStateOf(String.format("%.2f", zone.constraints.maxAcceleration)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zone #${idx + 1}: ${zone.name}", fontSize = 12.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = nameText,
            onValueChange = { newValue ->
                nameText = newValue
                onChanged(zone.copy(name = newValue))
            },
            label = { Text("Zone Name", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = velText,
                onValueChange = { newValue ->
                    velText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(zone.copy(constraints = zone.constraints.copy(maxVelocity = it)))
                    }
                },
                label = { Text("Max Vel (m/s)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )

            OutlinedTextField(
                value = accText,
                onValueChange = { newValue ->
                    accText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(zone.copy(constraints = zone.constraints.copy(maxAcceleration = it)))
                    }
                },
                label = { Text("Max Accel (m/s²)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Start Position: ${String.format("%.2f", minPosSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
            Slider(
                value = minPosSliderVal,
                onValueChange = { newValue ->
                    minPosSliderVal = newValue
                    if (newValue > maxPosSliderVal) {
                        maxPosSliderVal = newValue
                    }
                    onChanged(zone.copy(minWaypointRelativePos = newValue.toDouble(), maxWaypointRelativePos = maxPosSliderVal.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("End Position: ${String.format("%.2f", maxPosSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
            Slider(
                value = maxPosSliderVal,
                onValueChange = { newValue ->
                    maxPosSliderVal = newValue
                    if (newValue < minPosSliderVal) {
                        minPosSliderVal = newValue
                    }
                    onChanged(zone.copy(maxWaypointRelativePos = newValue.toDouble(), minWaypointRelativePos = minPosSliderVal.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }
    }
}

@Composable
private fun PointTowardsZoneCard(
    idx: Int,
    zone: PointTowardsZone,
    maxPos: Double,
    onChanged: (PointTowardsZone) -> Unit,
    onDelete: () -> Unit
) {
    var nameText by remember(zone.name) { mutableStateOf(zone.name) }
    var minPosSliderVal by remember(zone.minWaypointRelativePos) { mutableStateOf(zone.minWaypointRelativePos.toFloat()) }
    var maxPosSliderVal by remember(zone.maxWaypointRelativePos) { mutableStateOf(zone.maxWaypointRelativePos.toFloat()) }

    var targetXText by remember(zone.fieldPosition.x) { mutableStateOf(String.format("%.3f", zone.fieldPosition.x)) }
    var targetYText by remember(zone.fieldPosition.y) { mutableStateOf(String.format("%.3f", zone.fieldPosition.y)) }
    var offsetText by remember(zone.rotationOffset) { mutableStateOf(String.format("%.1f", zone.rotationOffset)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Aim Zone #${idx + 1}: ${zone.name}", fontSize = 12.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = nameText,
            onValueChange = { newValue ->
                nameText = newValue
                onChanged(zone.copy(name = newValue))
            },
            label = { Text("Zone Name", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = targetXText,
                onValueChange = { newValue ->
                    targetXText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(zone.copy(fieldPosition = PathPoint(it, zone.fieldPosition.y)))
                    }
                },
                label = { Text("Target X (m)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )

            OutlinedTextField(
                value = targetYText,
                onValueChange = { newValue ->
                    targetYText = newValue
                    newValue.toDoubleOrNull()?.let {
                        onChanged(zone.copy(fieldPosition = PathPoint(zone.fieldPosition.x, it)))
                    }
                },
                label = { Text("Target Y (m)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
            )
        }

        OutlinedTextField(
            value = offsetText,
            onValueChange = { newValue ->
                offsetText = newValue
                newValue.toDoubleOrNull()?.let {
                    onChanged(zone.copy(rotationOffset = it))
                }
            },
            label = { Text("Rotation Offset (Deg)", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Start Position: ${String.format("%.2f", minPosSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
            Slider(
                value = minPosSliderVal,
                onValueChange = { newValue ->
                    minPosSliderVal = newValue
                    if (newValue > maxPosSliderVal) {
                        maxPosSliderVal = newValue
                    }
                    onChanged(zone.copy(minWaypointRelativePos = newValue.toDouble(), maxWaypointRelativePos = maxPosSliderVal.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("End Position: ${String.format("%.2f", maxPosSliderVal)}", fontSize = 11.sp, color = AresTextPrimary)
            Slider(
                value = maxPosSliderVal,
                onValueChange = { newValue ->
                    maxPosSliderVal = newValue
                    if (newValue < minPosSliderVal) {
                        minPosSliderVal = newValue
                    }
                    onChanged(zone.copy(maxWaypointRelativePos = newValue.toDouble(), minWaypointRelativePos = minPosSliderVal.toDouble()))
                },
                valueRange = 0f..maxPos.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan, inactiveTrackColor = AresBorder)
            )
        }
    }
}

@Composable
internal fun CollapsibleSection(
    title: String,
    badgeCount: Int? = null,
    badgeText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AresBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = AresSurfaceElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    if (badgeCount != null) {
                        Surface(
                            shape = CircleShape,
                            color = AresCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(badgeCount.toString(), fontSize = 10.sp, color = AresCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AresCyan.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(badgeText, fontSize = 10.sp, color = AresCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = AresTextSecondary
                )
            }
            if (expanded) {
                HorizontalDivider(color = AresBorder)
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}
