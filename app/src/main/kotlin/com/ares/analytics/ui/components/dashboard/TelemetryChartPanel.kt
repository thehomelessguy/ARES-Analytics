package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.shared.RobotUnit
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.shared.UnitCategory
import com.ares.analytics.shared.UnitConversion
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

fun buildSignalTree(keys: List<String>): SignalNode {
    val root = SignalNode("", "", false)
    for (topic in keys) {
        val parts = topic.split("/").filter { it.isNotEmpty() }
        var current = root
        var currentPath = ""
        for (i in parts.indices) {
            val part = parts[i]
            currentPath += "/$part"
            val isLeaf = (i == parts.lastIndex)
            current = current.children.getOrPut(part) {
                SignalNode(part, currentPath, isLeaf)
            }
        }
    }
    return root
}

data class TelemetryPoint(val timestampMs: Long, val value: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryChartPanel(
    nt4ClientService: Nt4ClientService,
    properties: Map<String, String>,
    onPropertiesChanged: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    var parentWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasWindowBounds by remember { mutableStateOf<Rect?>(null) }

    // Configurable time windows (seconds)
    val timeWindows = listOf(10, 30, 60, 120)
    
    val initialKeys = remember(properties) {
        properties["selectedKeys"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }
    val initialWindow = remember(properties) {
        properties["windowSec"]?.toIntOrNull() ?: 30
    }

    var selectedWindowSec by remember(initialWindow) { mutableStateOf(initialWindow) }
    val selectedKeys = remember(initialKeys) { mutableStateListOf<String>().apply { addAll(initialKeys) } }

    LaunchedEffect(selectedKeys.toList(), selectedWindowSec) {
        val keysList = selectedKeys.toList()
        if (keysList != initialKeys || selectedWindowSec != initialWindow) {
            onPropertiesChanged(mapOf(
                "selectedKeys" to keysList.joinToString(","),
                "windowSec" to selectedWindowSec.toString()
            ))
        }
    }
    
    // In-memory data store for live plotting: key -> list of points
    val telemetryData = remember { mutableStateMapOf<String, List<TelemetryPoint>>() }

    // Selected target unit for each key
    val targetUnits = remember { mutableStateMapOf<String, RobotUnit>() }
    
    // Searchable dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val activeTopics = remember { mutableStateListOf<String>() }
    
    // Periodically update active topics from NT4 Service
    LaunchedEffect(Unit) {
        while (true) {
            val topics = nt4ClientService.getActiveTopics()
            activeTopics.clear()
            activeTopics.addAll(topics)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Subscribe to telemetry Flow
    LaunchedEffect(Unit) {
        nt4ClientService.telemetryFlow.collect { frame ->
            if (selectedKeys.contains(frame.key)) {
                val currentPoints = telemetryData[frame.key] ?: emptyList()
                val now = System.currentTimeMillis()
                val cutoff = now - (selectedWindowSec * 1000)
                
                // Add new point, filter out old points beyond window size
                val updatedPoints = (currentPoints + TelemetryPoint(frame.timestampMs, frame.value))
                    .filter { it.timestampMs >= cutoff }
                    
                telemetryData[frame.key] = updatedPoints
            }
        }
    }

    // Clean up data if keys are removed
    LaunchedEffect(selectedKeys.size) {
        val keysToRemove = telemetryData.keys.filter { !selectedKeys.contains(it) }
        keysToRemove.forEach { telemetryData.remove(it) }
    }

    // Legend colors for up to 8 channels
    val channelColors = listOf(
        AresCyan, AresRed, AresGreen, AresAmber,
        Color(0xFFFF00FF), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFFFFFFFF)
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                parentWindowOffset = coords.positionInWindow()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(AresSurface)
                .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Live Telemetry Viewer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AresTextPrimary
                )
                Text(
                    "Real-time streaming multi-channel scope",
                    style = MaterialTheme.typography.bodySmall,
                    color = AresTextTertiary
                )
            }
            
            // Window size selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                timeWindows.forEach { sec ->
                    FilterChip(
                        selected = selectedWindowSec == sec,
                        onClick = { selectedWindowSec = sec },
                        label = { Text("${sec}s", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AresCyan,
                            selectedLabelColor = AresBackground,
                            containerColor = AresSurfaceElevated,
                            labelColor = AresTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedWindowSec == sec,
                            borderColor = AresBorder
                        )
                    )
                }
            }
        }

        Divider(color = AresBorder, thickness = 1.dp)

        // Dropdown Search / Add Channel controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Button(
                    onClick = { dropdownExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AresSurfaceElevated),
                    border = ButtonDefaults.outlinedButtonBorder,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AresCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Channel", color = AresTextPrimary, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .width(300.dp)
                        .background(AresSurfaceElevated)
                        .border(1.dp, AresBorder)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search NT4 topics...", color = AresTextTertiary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AresCyan,
                            unfocusedBorderColor = AresBorder,
                            focusedTextColor = AresTextPrimary,
                            unfocusedTextColor = AresTextPrimary
                        )
                    )

                    val filteredTopics = activeTopics.filter {
                        it.contains(searchQuery, ignoreCase = true) && !selectedKeys.contains(it)
                    }

                    if (filteredTopics.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No matching topics found", color = AresTextTertiary, fontSize = 12.sp) },
                            onClick = {}
                        )
                    } else {
                        filteredTopics.take(10).forEach { topic ->
                            DropdownMenuItem(
                                text = { Text(topic, color = AresTextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    if (selectedKeys.size < 8) {
                                        selectedKeys.add(topic)
                                    }
                                    dropdownExpanded = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
            }

            // Legend chips
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(selectedKeys.toList()) { key ->
                    val index = selectedKeys.indexOf(key)
                    val color = channelColors[index % channelColors.size]
                    val detectedUnit = UnitConversion.detectUnitFromKey(key)
                    val targetUnit = targetUnits[key] ?: detectedUnit
                    var unitMenuExpanded by remember { mutableStateOf(false) }
                    
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AresSurfaceElevated)
                                .border(1.dp, AresBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (detectedUnit != null && detectedUnit.category != UnitCategory.NONE) {
                                        unitMenuExpanded = true
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            val label = key.split("/").last()
                            val unitSuffix = targetUnit?.let { " (${it.symbol})" } ?: ""
                            Text(
                                "$label$unitSuffix",
                                color = AresTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = AresTextTertiary,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { selectedKeys.remove(key) }
                            )
                        }

                        if (detectedUnit != null && detectedUnit.category != UnitCategory.NONE) {
                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false },
                                modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder)
                            ) {
                                RobotUnit.entries.filter { it.category == detectedUnit.category }.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.name + " (${unit.symbol})", color = AresTextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            targetUnits[key] = unit
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val signalTree = remember(activeTopics.toList()) { buildSignalTree(activeTopics.toList()) }

        // Plot Canvas
        var minY by remember { mutableStateOf(0.0) }
        var maxY by remember { mutableStateOf(1.0) }
        val firstKey = selectedKeys.firstOrNull()
        val currentUnitSymbol = firstKey?.let { targetUnits[it] ?: UnitConversion.detectUnitFromKey(it) }?.symbol ?: ""

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AresSurfaceElevated)
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
        ) {
            // Signal Tree Explorer
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(AresSurface)
            ) {
                SignalTreeExplorer(
                    rootNode = signalTree,
                    selectedKeys = selectedKeys,
                    onKeySelected = { key ->
                        if (selectedKeys.size < 8 && !selectedKeys.contains(key)) {
                            selectedKeys.add(key)
                        }
                    },
                    onDragStart = { key, offset ->
                        draggedKey = key
                        dragOffset = offset
                    },
                    onDrag = { offset ->
                        dragOffset += offset
                    },
                    onDragEnd = {
                        val finalOffset = dragOffset
                        val bounds = canvasWindowBounds
                        if (bounds != null && bounds.contains(finalOffset)) {
                            draggedKey?.let { key ->
                                if (selectedKeys.size < 8 && !selectedKeys.contains(key)) {
                                    selectedKeys.add(key)
                                }
                            }
                        }
                        draggedKey = null
                    }
                )
            }

            VerticalDivider(color = AresBorder, modifier = Modifier.fillMaxHeight())

            // Plot Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { coordinates ->
                        canvasWindowBounds = coordinates.boundsInWindow()
                    }
            ) {
                if (selectedKeys.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Drag & drop channels here or use tree (+) to plot.", color = AresTextTertiary, fontSize = 12.sp)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 12.dp)) {
                    val width = size.width
                    val height = size.height
 
                    // 1. Draw Grid Lines
                    val gridLinesX = 5
                    val gridLinesY = 4
                    for (i in 0..gridLinesX) {
                        val x = width * i / gridLinesX
                        drawLine(color = AresBorder, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                    }
                    for (i in 0..gridLinesY) {
                        val y = height * i / gridLinesY
                        drawLine(color = AresBorder, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    }
 
                    // 2. Compute Global Bounds (Y-axis auto-scaling)
                    var tempMinY = Double.MAX_VALUE
                    var tempMaxY = -Double.MAX_VALUE
                    var hasData = false
 
                    selectedKeys.forEach { key ->
                        val points = telemetryData[key] ?: emptyList()
                        val detectedUnit = UnitConversion.detectUnitFromKey(key)
                        val targetUnit = targetUnits[key] ?: detectedUnit
                        if (points.isNotEmpty()) {
                            hasData = true
                            points.forEach {
                                val value = if (detectedUnit != null && targetUnit != null) {
                                    UnitConversion.convert(it.value, detectedUnit, targetUnit)
                                } else {
                                    it.value
                                }
                                if (value < tempMinY) tempMinY = value
                                if (value > tempMaxY) tempMaxY = value
                            }
                        }
                    }
 
                    // Handle edge cases
                    if (!hasData) {
                        tempMinY = 0.0
                        tempMaxY = 1.0
                    } else if (tempMinY == tempMaxY) {
                        tempMinY -= 1.0
                        tempMaxY += 1.0
                    } else {
                        // Add 10% padding
                        val diff = tempMaxY - tempMinY
                        tempMinY -= diff * 0.1
                        tempMaxY += diff * 0.1
                    }

                    minY = tempMinY
                    maxY = tempMaxY
 
                    // 4. Plot each active channel
                    selectedKeys.forEachIndexed { channelIdx, key ->
                        val points = telemetryData[key] ?: emptyList()
                        val detectedUnit = UnitConversion.detectUnitFromKey(key)
                        val targetUnit = targetUnits[key] ?: detectedUnit
                        if (points.size >= 2) {
                            val color = channelColors[channelIdx % channelColors.size]
                            val now = System.currentTimeMillis()
                            val minX = now - (selectedWindowSec * 1000)
                            val maxX = now
 
                            val path = Path()
                            points.forEachIndexed { ptIdx, pt ->
                                val xPct = (pt.timestampMs - minX).toFloat() / (maxX - minX)
                                val value = if (detectedUnit != null && targetUnit != null) {
                                    UnitConversion.convert(pt.value, detectedUnit, targetUnit)
                                } else {
                                    pt.value
                                }
                                val yPct = ((value - minY) / (maxY - minY)).toFloat()
                                 
                                val px = xPct * width
                                val py = height - (yPct * height) // Invert Y-axis
 
                                if (ptIdx == 0) {
                                    path.moveTo(px, py)
                                } else {
                                    path.lineTo(px, py)
                                }
                            }
 
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Overlay Y-axis labels
                Column(
                    modifier = Modifier.fillMaxHeight().padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%.2f %s", maxY, currentUnitSymbol),
                        color = AresTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f %s", minY, currentUnitSymbol),
                        color = AresTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
        
        if (draggedKey != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((dragOffset.x - parentWindowOffset.x).toInt() - 20, (dragOffset.y - parentWindowOffset.y).toInt() - 20) }
                    .background(AresCyan.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = draggedKey!!.split("/").last(),
                    color = AresBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SignalTreeExplorer(
    rootNode: SignalNode,
    selectedKeys: List<String>,
    onKeySelected: (String) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        fun renderNode(node: SignalNode, depth: Int, path: String) {
            node.children.values.sortedBy { it.name }.forEach { child ->
                val currentPath = if (path.isEmpty()) child.name else "$path/${child.name}"
                val isLeaf = child.isLeaf
                val cleanPath = child.fullPath.removePrefix("/")
                val isExpanded = expandedStates[currentPath] ?: false

                item(key = currentPath) {
                    var nodeOffset by remember { mutableStateOf(Offset.Zero) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (depth * 8).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isLeaf && selectedKeys.contains(cleanPath)) AresCyan.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .onGloballyPositioned { coords ->
                                nodeOffset = coords.positionInWindow()
                            }
                            .pointerInput(isLeaf) {
                                if (isLeaf) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            onDragStart(cleanPath, nodeOffset + offset)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount)
                                        }
                                    )
                                }
                            }
                            .clickable {
                                if (isLeaf) {
                                    onKeySelected(cleanPath)
                                } else {
                                    expandedStates[currentPath] = !isExpanded
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!isLeaf) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                contentDescription = null,
                                tint = AresTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AresCyan.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = AresAmber.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Text(
                            text = child.name,
                            color = if (isLeaf && selectedKeys.contains(cleanPath)) AresCyan else AresTextPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isLeaf) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = AresTextTertiary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onKeySelected(cleanPath) }
                            )
                        }
                    }
                }

                if (!isLeaf && isExpanded) {
                    renderNode(child, depth + 1, currentPath)
                }
            }
        }

        renderNode(rootNode, 0, "")
    }
}

