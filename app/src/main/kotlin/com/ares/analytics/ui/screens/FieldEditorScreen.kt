package com.ares.analytics.ui.screens

import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.components.pathplanner.FieldCanvas
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.FieldEditorIntent
import com.ares.analytics.viewmodel.FieldEditorViewModel

@Composable
fun FieldEditorScreen(
    viewModel: FieldEditorViewModel,
    league: League,
    projectPath: String? = null
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(projectPath) {
        viewModel.onIntent(FieldEditorIntent.LoadConfig(projectPath, league))
    }

    var showCropBoundaries by remember { mutableStateOf(false) }

    val fieldWidthM = if (state.fieldImageConfig.widthMeters > 0.0) state.fieldImageConfig.widthMeters else (if (league == League.FTC) 3.65 else 16.5)
    val fieldHeightM = if (state.fieldImageConfig.heightMeters > 0.0) state.fieldImageConfig.heightMeters else (if (league == League.FTC) 3.65 else 8.2)

    fun mirrorObstacleX(obs: Obstacle, fieldWidth: Double, league: League): Obstacle {
        return when (obs) {
            is Obstacle.Circle -> {
                val newX = if (league == League.FTC) -obs.centerX else fieldWidth - obs.centerX
                obs.copy(centerX = newX)
            }
            is Obstacle.Rectangle -> {
                val newX = if (league == League.FTC) -obs.centerX else fieldWidth - obs.centerX
                obs.copy(centerX = newX, rotation = -obs.rotation)
            }
            is Obstacle.Polygon -> {
                obs.copy(vertices = obs.vertices.map { v ->
                    val newX = if (league == League.FTC) -v.x else fieldWidth - v.x
                    PathPoint(newX, v.y)
                })
            }
        }
    }

    fun mirrorObstacleY(obs: Obstacle, fieldHeight: Double, league: League): Obstacle {
        return when (obs) {
            is Obstacle.Circle -> {
                val newY = if (league == League.FTC) -obs.centerY else fieldHeight - obs.centerY
                obs.copy(centerY = newY)
            }
            is Obstacle.Rectangle -> {
                val newY = if (league == League.FTC) -obs.centerY else fieldHeight - obs.centerY
                obs.copy(centerY = newY, rotation = -obs.rotation)
            }
            is Obstacle.Polygon -> {
                obs.copy(vertices = obs.vertices.map { v ->
                    val newY = if (league == League.FTC) -v.y else fieldHeight - v.y
                    PathPoint(v.x, newY)
                })
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side controls
        Surface(
            modifier = Modifier.width(320.dp).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Field Background Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                HorizontalDivider(color = AresBorder)

                Button(
                    onClick = {
                        if (!projectPath.isNullOrEmpty()) {
                            SwingUtilities.invokeLater {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "Select Field Image (PNG/JPG)"
                                    fileFilter = FileNameExtensionFilter("Images", "png", "jpg", "jpeg")
                                }
                                val result = chooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    val selectedFile = chooser.selectedFile
                                    if (selectedFile != null && selectedFile.exists()) {
                                        viewModel.onIntent(FieldEditorIntent.ImportFieldImage(selectedFile, projectPath, league))
                                    }
                                }
                            }
                        } else {
                            viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig, projectPath, league))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = AresBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Field Image", color = AresBackground, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = AresBorder)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCropBoundaries = !showCropBoundaries }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Image Adjustments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    Icon(
                        imageVector = if (showCropBoundaries) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = "Expand/Collapse",
                        tint = AresTextSecondary
                    )
                }

                AnimatedVisibility(visible = showCropBoundaries) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Orientation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0.0, 90.0, 180.0, 270.0).forEach { angle ->
                                    val isSelected = state.fieldImageConfig.rotationDegrees == angle
                                    Button(
                                        onClick = {
                                            viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(rotationDegrees = angle), projectPath, league))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AresCyan else AresSurfaceElevated,
                                            contentColor = if (isSelected) AresBackground else AresTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        Text("${angle.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Crop Boundaries", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                            Text("Left Crop: ${String.format("%.2f", state.fieldImageConfig.cropLeft)}", fontSize = 11.sp, color = AresTextSecondary)
                            Slider(
                                value = state.fieldImageConfig.cropLeft.toFloat(),
                                onValueChange = {
                                    viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(cropLeft = it.toDouble().coerceIn(0.0, state.fieldImageConfig.cropRight)), projectPath, league))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan)
                            )

                            Text("Right Crop: ${String.format("%.2f", state.fieldImageConfig.cropRight)}", fontSize = 11.sp, color = AresTextSecondary)
                            Slider(
                                value = state.fieldImageConfig.cropRight.toFloat(),
                                onValueChange = {
                                    viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(cropRight = it.toDouble().coerceIn(state.fieldImageConfig.cropLeft, 1.0)), projectPath, league))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan)
                            )

                            Text("Top Crop: ${String.format("%.2f", state.fieldImageConfig.cropTop)}", fontSize = 11.sp, color = AresTextSecondary)
                            Slider(
                                value = state.fieldImageConfig.cropTop.toFloat(),
                                onValueChange = {
                                    viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(cropTop = it.toDouble().coerceIn(0.0, state.fieldImageConfig.cropBottom)), projectPath, league))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan)
                            )

                            Text("Bottom Crop: ${String.format("%.2f", state.fieldImageConfig.cropBottom)}", fontSize = 11.sp, color = AresTextSecondary)
                            Slider(
                                value = state.fieldImageConfig.cropBottom.toFloat(),
                                onValueChange = {
                                    viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(cropBottom = it.toDouble().coerceIn(state.fieldImageConfig.cropTop, 1.0)), projectPath, league))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AresCyan, activeTrackColor = AresCyan)
                            )
                        }
                    }
                }

                if (league == League.FTC) {
                    HorizontalDivider(color = AresBorder)
                    
                    Text("FTC Coordinate System", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FTCCoordinateSystem.entries.forEach { coord ->
                            val isSelected = state.fieldImageConfig.ftcCoordinateSystem == coord
                            Button(
                                onClick = {
                                    viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig.copy(ftcCoordinateSystem = coord), projectPath, league))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) AresCyan else AresSurfaceElevated,
                                    contentColor = if (isSelected) AresBackground else AresTextPrimary
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = when (coord) {
                                        FTCCoordinateSystem.DIAMOND -> "Diamond"
                                        FTCCoordinateSystem.SQUARE -> "Square"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = AresBorder)

                Button(
                    onClick = {
                        viewModel.onIntent(FieldEditorIntent.UpdateFieldImageConfig(state.fieldImageConfig, projectPath, league))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Configuration", color = AresBackground, fontWeight = FontWeight.Bold)
                }

                if (state.saveStatus.isNotEmpty()) {
                    Text(
                        text = state.saveStatus,
                        color = if (state.saveStatus.contains("failed") || state.saveStatus.contains("error")) AresError else AresGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LaunchedEffect(state.saveStatus) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.onIntent(FieldEditorIntent.ClearSaveStatus)
                    }
                }

                // Drawn Obstacles Section
                if (state.obstacles.isNotEmpty()) {
                    HorizontalDivider(color = AresBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.selectedElement != null) "Selected Item Properties" else "Drawn Obstacles (${state.obstacles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AresTextPrimary
                        )
                        if (state.selectedElement != null) {
                            TextButton(
                                onClick = { viewModel.onIntent(FieldEditorIntent.SelectElement(null)) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Selection", fontSize = 10.sp, color = AresCyan)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val copies = state.obstacles.map { obs ->
                                            val mirrored = mirrorObstacleX(obs, fieldWidthM, league)
                                            when (mirrored) {
                                                is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored X")
                                                is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored X")
                                                is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored X")
                                            }
                                        }
                                        viewModel.onIntent(FieldEditorIntent.SetObstacles(state.obstacles + copies))
                                        viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp), tint = AresCyan)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Copy X", fontSize = 10.sp, color = AresCyan)
                                }
                                TextButton(
                                    onClick = {
                                        val copies = state.obstacles.map { obs ->
                                            val mirrored = mirrorObstacleY(obs, fieldHeightM, league)
                                            when (mirrored) {
                                                is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored Y")
                                                is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored Y")
                                                is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}_${obs.id.hashCode()}", name = "${obs.name} Mirrored Y")
                                            }
                                        }
                                        viewModel.onIntent(FieldEditorIntent.SetObstacles(state.obstacles + copies))
                                        viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(12.dp), tint = AresCyan)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Copy Y", fontSize = 10.sp, color = AresCyan)
                                }
                            }
                        }
                    }
                    
                    state.obstacles.forEachIndexed { index, obs ->
                        if (state.selectedElement == null || state.selectedElement == obs.id) {
                            key(obs.id) {
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
                                        OutlinedTextField(
                                            value = obs.name,
                                            onValueChange = { newName ->
                                                val updated = when (obs) {
                                                    is Obstacle.Circle -> obs.copy(name = newName)
                                                    is Obstacle.Rectangle -> obs.copy(name = newName)
                                                    is Obstacle.Polygon -> obs.copy(name = newName)
                                                }
                                                viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, updated))
                                                viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                            },
                                            label = { Text("Label", fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
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
                                                var radiusText by remember(obs.id, obs.radius) { mutableStateOf(obs.radius.toString()) }
                                                OutlinedTextField(
                                                    value = radiusText,
                                                    onValueChange = { newVal ->
                                                        radiusText = newVal
                                                        newVal.toDoubleOrNull()?.let { parsed ->
                                                            viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, obs.copy(radius = parsed)))
                                                            viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                        }
                                                    },
                                                    label = { Text("Radius (m)", fontSize = 9.sp) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                                )
                                            }
                                            is Obstacle.Rectangle -> {
                                                var rectWText by remember(obs.id, obs.width) { mutableStateOf(obs.width.toString()) }
                                                var rectHText by remember(obs.id, obs.height) { mutableStateOf(obs.height.toString()) }
                                                var rotationText by remember(obs.id, obs.rotation) { mutableStateOf(obs.rotation.toString()) }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = rectWText,
                                                        onValueChange = { newVal ->
                                                            rectWText = newVal
                                                            newVal.toDoubleOrNull()?.let { parsed ->
                                                                viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, obs.copy(width = parsed)))
                                                                viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                            }
                                                        },
                                                        label = { Text("W (m)", fontSize = 9.sp) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                                    )
                                                    OutlinedTextField(
                                                        value = rectHText,
                                                        onValueChange = { newVal ->
                                                            rectHText = newVal
                                                            newVal.toDoubleOrNull()?.let { parsed ->
                                                                viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, obs.copy(height = parsed)))
                                                                viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                            }
                                                        },
                                                        label = { Text("H (m)", fontSize = 9.sp) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                                    )
                                                    OutlinedTextField(
                                                        value = rotationText,
                                                        onValueChange = { newVal ->
                                                            rotationText = newVal
                                                            newVal.toDoubleOrNull()?.let { parsed ->
                                                                viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, obs.copy(rotation = parsed)))
                                                                viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                            }
                                                        },
                                                        label = { Text("Rot (deg)", fontSize = 9.sp) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
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
                                                    val mirrored = mirrorObstacleX(obs, fieldWidthM, league)
                                                    viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, mirrored))
                                                    viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Mirror Vertically", color = AresTextPrimary) },
                                                onClick = {
                                                    flipMenuExpanded = false
                                                    val mirrored = mirrorObstacleY(obs, fieldHeightM, league)
                                                    viewModel.onIntent(FieldEditorIntent.UpdateObstacle(index, mirrored))
                                                    viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicate & Mirror X", color = AresTextPrimary) },
                                                onClick = {
                                                    flipMenuExpanded = false
                                                    val mirrored = mirrorObstacleX(obs, fieldWidthM, league)
                                                    val copy = when (mirrored) {
                                                        is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                                                        is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                                                        is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}", name = "${obs.name} Mirrored X")
                                                    }
                                                    viewModel.onIntent(FieldEditorIntent.AddObstacle(copy))
                                                    viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicate & Mirror Y", color = AresTextPrimary) },
                                                onClick = {
                                                    flipMenuExpanded = false
                                                    val mirrored = mirrorObstacleY(obs, fieldHeightM, league)
                                                    val copy = when (mirrored) {
                                                        is Obstacle.Circle -> mirrored.copy(id = "circle_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                                                        is Obstacle.Rectangle -> mirrored.copy(id = "rect_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                                                        is Obstacle.Polygon -> mirrored.copy(id = "poly_${System.currentTimeMillis()}", name = "${obs.name} Mirrored Y")
                                                    }
                                                    viewModel.onIntent(FieldEditorIntent.AddObstacle(copy))
                                                    viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                                }
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.onIntent(FieldEditorIntent.DeleteObstacle(index))
                                            viewModel.onIntent(FieldEditorIntent.SaveObstacles(projectPath, league))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Placed Game Pieces Section
                if (state.gamePieces.isNotEmpty()) {
                    HorizontalDivider(color = AresBorder)
                    Text("Placed Game Pieces (${state.gamePieces.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    state.gamePieces.forEachIndexed { index, gp ->
                        key(gp.id) {
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
                                    OutlinedTextField(
                                        value = gp.name,
                                        onValueChange = { newName ->
                                            viewModel.onIntent(FieldEditorIntent.UpdateGamePiece(index, gp.copy(name = newName)))
                                            viewModel.onIntent(FieldEditorIntent.SaveGamePieces(projectPath, league))
                                        },
                                        label = { Text("Label", fontSize = 9.sp) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val types = if (league == League.FTC) {
                                        listOf("Sample (Yellow)", "Sample (Red)", "Sample (Blue)", "Specimen")
                                    } else {
                                        listOf("Note", "High Note")
                                    }
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        Text(
                                            text = "Type: ${gp.type} ▾",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AresCyan,
                                            modifier = Modifier.clickable { expanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            types.forEach { t ->
                                                DropdownMenuItem(
                                                    text = { Text(t, fontSize = 12.sp) },
                                                    onClick = {
                                                        expanded = false
                                                        viewModel.onIntent(FieldEditorIntent.UpdateGamePiece(index, gp.copy(type = t)))
                                                        viewModel.onIntent(FieldEditorIntent.SaveGamePieces(projectPath, league))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Pos: (${String.format("%.2f", gp.x)}, ${String.format("%.2f", gp.y)})", fontSize = 10.sp, color = AresTextSecondary)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.onIntent(FieldEditorIntent.DeleteGamePiece(index))
                                        viewModel.onIntent(FieldEditorIntent.SaveGamePieces(projectPath, league))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Placed AprilTags Section
                if (state.aprilTags.isNotEmpty()) {
                    HorizontalDivider(color = AresBorder)
                    Text("Placed AprilTags (${state.aprilTags.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    state.aprilTags.forEachIndexed { index, at ->
                        key(at.id) {
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        var tagIdText by remember(at.id, at.tagId) { mutableStateOf(at.tagId.toString()) }
                                        OutlinedTextField(
                                            value = tagIdText,
                                            onValueChange = { newVal ->
                                                tagIdText = newVal
                                                newVal.toIntOrNull()?.let { parsed ->
                                                    viewModel.onIntent(FieldEditorIntent.UpdateAprilTag(index, at.copy(tagId = parsed)))
                                                    viewModel.onIntent(FieldEditorIntent.SaveAprilTags(projectPath, league))
                                                }
                                            },
                                            label = { Text("Tag ID", fontSize = 9.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                        )

                                        var tagZText by remember(at.id, at.z) { mutableStateOf(at.z.toString()) }
                                        OutlinedTextField(
                                            value = tagZText,
                                            onValueChange = { newVal ->
                                                tagZText = newVal
                                                newVal.toDoubleOrNull()?.let { parsed ->
                                                    viewModel.onIntent(FieldEditorIntent.UpdateAprilTag(index, at.copy(z = parsed)))
                                                    viewModel.onIntent(FieldEditorIntent.SaveAprilTags(projectPath, league))
                                                }
                                            },
                                            label = { Text("Z (m)", fontSize = 9.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                        )

                                        var tagYawText by remember(at.id, at.yawDegrees) { mutableStateOf(at.yawDegrees.toString()) }
                                        OutlinedTextField(
                                            value = tagYawText,
                                            onValueChange = { newVal ->
                                                tagYawText = newVal
                                                newVal.toDoubleOrNull()?.let { parsed ->
                                                    viewModel.onIntent(FieldEditorIntent.UpdateAprilTag(index, at.copy(yawDegrees = parsed)))
                                                    viewModel.onIntent(FieldEditorIntent.SaveAprilTags(projectPath, league))
                                                }
                                            },
                                            label = { Text("Yaw (°)", fontSize = 9.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = AresTextPrimary),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Pos: (${String.format("%.2f", at.x)}, ${String.format("%.2f", at.y)})", fontSize = 10.sp, color = AresTextSecondary)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.onIntent(FieldEditorIntent.DeleteAprilTag(index))
                                        viewModel.onIntent(FieldEditorIntent.SaveAprilTags(projectPath, league))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AresError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right side canvas
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        ) {
            FieldCanvas(
                league = league,
                waypoints = emptyList(),
                actualPath = emptyList(),
                onWaypointsChanged = {},
                projectPath = projectPath,
                showPathControls = false,
                showObstacleControls = true,
                fieldImage = state.fieldImage,
                fieldImageConfig = state.fieldImageConfig,
                obstacles = state.obstacles,
                onObstaclesChanged = {
                    viewModel.onIntent(FieldEditorIntent.SetObstacles(it))
                },
                gamePieces = state.gamePieces,
                onGamePiecesChanged = {
                    viewModel.onIntent(FieldEditorIntent.SetGamePieces(it))
                },
                aprilTags = state.aprilTags,
                onAprilTagsChanged = {
                    viewModel.onIntent(FieldEditorIntent.SetAprilTags(it))
                }
            )
        }
    }
}
