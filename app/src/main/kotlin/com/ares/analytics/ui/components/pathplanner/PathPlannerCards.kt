package com.ares.analytics.ui.components.pathplanner

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

@Composable
fun WaypointCard(
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
fun EventMarkerCard(
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
fun RotationTargetCard(
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
fun ConstraintsZoneCard(
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
fun PointTowardsZoneCard(
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
fun CollapsibleSection(
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
