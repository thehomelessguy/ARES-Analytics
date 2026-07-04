package com.ares.analytics.ui.screens.fieldeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.League
import com.ares.analytics.shared.Obstacle
import com.ares.analytics.ui.components.forms.AresTextField
import com.ares.analytics.ui.theme.*

@Composable
fun ObstacleRow(
    index: Int,
    obs: Obstacle,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    onUpdate: (Int, Obstacle) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: (Obstacle) -> Unit,
    onMirrorX: (Obstacle, Double, League) -> Obstacle,
    onMirrorY: (Obstacle, Double, League) -> Obstacle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresSurfaceElevated)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AresTextField(
                value = obs.name,
                onValueChange = { newName ->
                    val updated = when (obs) {
                        is Obstacle.Circle -> obs.copy(name = newName)
                        is Obstacle.Rectangle -> obs.copy(name = newName)
                        is Obstacle.Polygon -> obs.copy(name = newName)
                    }
                    onUpdate(index, updated)
                },
                label = "Label",
                labelFontSize = 9.sp,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
            )
            Spacer(modifier = Modifier.height(2.dp))
            val details = when (obs) {
                is Obstacle.Circle -> "Circle | r=${String.format("%.2fm", obs.radius)}"
                is Obstacle.Rectangle -> "Rect | ${String.format("%.2fm", obs.width)}x${String.format("%.2fm", obs.height)} @ ${String.format("%.0f°", obs.rotation)}"
                is Obstacle.Polygon -> "Poly | ${obs.vertices.size} vertices"
            }
            Text(details, fontSize = 10.sp, color = AresTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            when (obs) {
                is Obstacle.Circle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        var cxText by remember(obs.id, obs.centerX) { mutableStateOf(obs.centerX.toString()) }
                        AresTextField(
                            value = cxText,
                            onValueChange = { newVal ->
                                cxText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(centerX = parsed)) }
                            },
                            label = "X (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                        var cyText by remember(obs.id, obs.centerY) { mutableStateOf(obs.centerY.toString()) }
                        AresTextField(
                            value = cyText,
                            onValueChange = { newVal ->
                                cyText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(centerY = parsed)) }
                            },
                            label = "Y (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    var radiusText by remember(obs.id, obs.radius) { mutableStateOf(obs.radius.toString()) }
                    AresTextField(
                        value = radiusText,
                        onValueChange = { newVal ->
                            radiusText = newVal
                            newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(radius = parsed)) }
                        },
                        label = "Radius (m)",
                        labelFontSize = 9.sp,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                    )
                }
                is Obstacle.Rectangle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        var cxText by remember(obs.id, obs.centerX) { mutableStateOf(obs.centerX.toString()) }
                        AresTextField(
                            value = cxText,
                            onValueChange = { newVal ->
                                cxText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(centerX = parsed)) }
                            },
                            label = "X (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                        var cyText by remember(obs.id, obs.centerY) { mutableStateOf(obs.centerY.toString()) }
                        AresTextField(
                            value = cyText,
                            onValueChange = { newVal ->
                                cyText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(centerY = parsed)) }
                            },
                            label = "Y (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    var rectWText by remember(obs.id, obs.width) { mutableStateOf(obs.width.toString()) }
                    var rectHText by remember(obs.id, obs.height) { mutableStateOf(obs.height.toString()) }
                    var rotationText by remember(obs.id, obs.rotation) { mutableStateOf(obs.rotation.toString()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AresTextField(
                            value = rectWText,
                            onValueChange = { newVal ->
                                rectWText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(width = parsed)) }
                            },
                            label = "W (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                        AresTextField(
                            value = rectHText,
                            onValueChange = { newVal ->
                                rectHText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(height = parsed)) }
                            },
                            label = "H (m)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                        AresTextField(
                            value = rotationText,
                            onValueChange = { newVal ->
                                rotationText = newVal
                                newVal.toDoubleOrNull()?.let { parsed -> onUpdate(index, obs.copy(rotation = parsed)) }
                            },
                            label = "Rot (deg)",
                            labelFontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary)
                        )
                    }
                }
                else -> {}
            }
        }
        var flipMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { flipMenuExpanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Flip, contentDescription = "Flip Menu", tint = AresCyan, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = flipMenuExpanded,
                onDismissRequest = { flipMenuExpanded = false },
                modifier = Modifier.background(AresSurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Mirror Horizontally", color = AresTextPrimary) },
                    onClick = {
                        flipMenuExpanded = false
                        val mirrored = onMirrorX(obs, fieldWidthM, league)
                        onUpdate(index, mirrored)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Mirror Vertically", color = AresTextPrimary) },
                    onClick = {
                        flipMenuExpanded = false
                        val mirrored = onMirrorY(obs, fieldHeightM, league)
                        onUpdate(index, mirrored)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate & Mirror X", color = AresTextPrimary) },
                    onClick = {
                        flipMenuExpanded = false
                        val mirrored = onMirrorX(obs, fieldWidthM, league)
                        val copy = when (mirrored) {
                            is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                            is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                            is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                        }
                        onAdd(copy)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate & Mirror Y", color = AresTextPrimary) },
                    onClick = {
                        flipMenuExpanded = false
                        val mirrored = onMirrorY(obs, fieldHeightM, league)
                        val copy = when (mirrored) {
                            is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                            is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                            is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                        }
                        onAdd(copy)
                    }
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = {
                    val updated = when (obs) {
                        is Obstacle.Circle -> obs.copy(locked = !obs.locked)
                        is Obstacle.Rectangle -> obs.copy(locked = !obs.locked)
                        is Obstacle.Polygon -> obs.copy(locked = !obs.locked)
                    }
                    onUpdate(index, updated)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(if (obs.locked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "Lock", tint = if (obs.locked) AresCyan else AresTextTertiary, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { onDelete(index) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
            }
        }
    }
}
