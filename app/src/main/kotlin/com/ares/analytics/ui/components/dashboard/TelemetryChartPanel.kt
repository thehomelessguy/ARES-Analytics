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
import androidx.compose.material.icons.automirrored.filled.ArrowRight
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clipToBounds
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun buildSignalTree(keys: List<String>): SignalNode {
    /**
     * root val.
     */
    val root = SignalNode("", "", false)
    for (topic in keys) {
        /**
         * parts val.
         */
        val parts = topic.split("/").filter { it.isNotEmpty() }
        /**
         * current var.
         */
        var current = root
        /**
         * currentPath var.
         */
        var currentPath = ""
        for (i in parts.indices) {
            /**
             * part val.
             */
            val part = parts[i]
            currentPath += "/$part"
            /**
             * isLeaf val.
             */
            val isLeaf = (i == parts.lastIndex)
            current = current.children.getOrPut(part) {
                SignalNode(part, currentPath, isLeaf)
            }
        }
    }
    return root
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class TelemetryPoint(val timestampMs: Long, val value: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun TelemetryChartPanel(
    nt4ClientService: Nt4ClientService,
    properties: Map<String, String>,
    onPropertiesChanged: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    
    /**
     * parentWindowOffset var.
     */
    var parentWindowOffset by remember { mutableStateOf(Offset.Zero) }
    /**
     * draggedKey var.
     */
    var draggedKey by remember { mutableStateOf<String?>(null) }
    /**
     * dragOffset var.
     */
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    /**
     * canvasWindowBounds var.
     */
    var canvasWindowBounds by remember { mutableStateOf<Rect?>(null) }

    /**
     * isTreeVisible var.
     */
    var isTreeVisible by remember { mutableStateOf(true) }
    /**
     * treeWidth val.
     */
    val treeWidth by animateDpAsState(
        targetValue = if (isTreeVisible) 260.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    // Configurable time windows (seconds)
    /**
     * timeWindows val.
     */
    val timeWindows = listOf(10, 30, 60, 120)
    
    /**
     * initialKeys val.
     */
    val initialKeys = remember(properties) {
        properties["selectedKeys"]?.split(",")?.map { it.removePrefix("/") }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    /**
     * initialWindow val.
     */
    val initialWindow = remember(properties) {
        properties["windowSec"]?.toIntOrNull() ?: 30
    }

    /**
     * selectedWindowSec var.
     */
    var selectedWindowSec by remember(initialWindow) { mutableStateOf(initialWindow) }
    /**
     * selectedKeys val.
     */
    val selectedKeys = remember(initialKeys) { mutableStateListOf<String>().apply { addAll(initialKeys) } }

    // In-memory data store for live plotting: key -> ArrayDeque of points (circular buffer)
    /**
     * telemetryData val.
     */
    val telemetryData = remember { ConcurrentHashMap<String, ArrayDeque<TelemetryPoint>>() }
    /**
     * lastUpdateTick var.
     */
    var lastUpdateTick by remember { mutableStateOf(0L) }
    /**
     * serverTimeOffset var.
     */
    var serverTimeOffset by remember { mutableStateOf(0L) }
    /**
     * hasReceivedData var.
     */
    var hasReceivedData by remember { mutableStateOf(false) }
    /**
     * liveTime var.
     */
    var liveTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(selectedKeys.toList(), selectedWindowSec) {
        /**
         * keysList val.
         */
        val keysList = selectedKeys.toList()
        if (keysList != initialKeys || selectedWindowSec != initialWindow) {
            onPropertiesChanged(mapOf(
                "selectedKeys" to keysList.joinToString(","),
                "windowSec" to selectedWindowSec.toString()
            ))
        }

        // Initialize newly added keys with their historical values
        keysList.forEach { key ->
            /**
             * queue val.
             */
            val queue = telemetryData.getOrPut(key) { ArrayDeque() }
            synchronized(queue) {
                if (queue.isEmpty()) {
                    /**
                     * history val.
                     */
                    val history = nt4ClientService.telemetryHistory[key]
                    if (history != null) {
                        synchronized(history) {
                            history.forEach { frame ->
                                queue.add(TelemetryPoint(frame.timestampMs, frame.value))
                            }
                        }
                    } else {
                        /**
                         * latest val.
                         */
                        val latest = nt4ClientService.latestValues[key]
                        if (latest != null) {
                            queue.add(TelemetryPoint(latest.timestampMs, latest.value))
                        }
                    }
                }
            }
        }
    }

    // Live clock ticker to keep the chart scrolling smoothly even when stationary
    LaunchedEffect(Unit) {
        while (true) {
            liveTime = System.currentTimeMillis() + serverTimeOffset
            kotlinx.coroutines.delay(100)
        }
    }

    // Selected target unit for each key
    /**
     * targetUnits val.
     */
    val targetUnits = remember { mutableStateMapOf<String, RobotUnit>() }
    
    // Searchable dropdown state
    /**
     * dropdownExpanded var.
     */
    var dropdownExpanded by remember { mutableStateOf(false) }
    /**
     * searchQuery var.
     */
    var searchQuery by remember { mutableStateOf("") }
    /**
     * activeTopics val.
     */
    val activeTopics = remember { mutableStateListOf<String>() }
    
    // Periodically update active topics from NT4 Service
    LaunchedEffect(Unit) {
        while (true) {
            /**
             * topics val.
             */
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
                /**
                 * queue val.
                 */
                val queue = telemetryData.getOrPut(frame.key) { ArrayDeque() }
                /**
                 * now val.
                 */
                val now = frame.timestampMs
                
                /**
                 * offset val.
                 */
                val offset = now - System.currentTimeMillis()
                if (!hasReceivedData || kotlin.math.abs(serverTimeOffset - offset) > 100) {
                    serverTimeOffset = offset
                    hasReceivedData = true
                }

                /**
                 * maxWindowSec val.
                 */
                val maxWindowSec = timeWindows.maxOrNull() ?: 120
                /**
                 * cutoff val.
                 */
                val cutoff = now - (maxWindowSec * 1000)
                
                synchronized(queue) {
                    queue.add(TelemetryPoint(frame.timestampMs, frame.value))
                    while (queue.size > 1 && queue[1].timestampMs < cutoff) {
                        queue.removeFirst()
                    }
                }
                lastUpdateTick = frame.timestampMs
            }
        }
    }

    // Legend colors for up to 8 channels
    /**
     * channelColors val.
     */
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

        HorizontalDivider(color = AresBorder, thickness = 1.dp)

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

                    /**
                     * filteredTopics val.
                     */
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

            Button(
                onClick = { isTreeVisible = !isTreeVisible },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTreeVisible) AresCyan else AresSurfaceElevated
                ),
                border = ButtonDefaults.outlinedButtonBorder,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isTreeVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                    contentDescription = null,
                    tint = if (isTreeVisible) AresBackground else AresCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isTreeVisible) "Hide Signals" else "Show Signals",
                    color = if (isTreeVisible) AresBackground else AresTextPrimary,
                    fontSize = 12.sp
                )
            }

            // Legend chips
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(selectedKeys.toList()) { key ->
                    /**
                     * index val.
                     */
                    val index = selectedKeys.indexOf(key)
                    /**
                     * color val.
                     */
                    val color = channelColors[index % channelColors.size]
                    /**
                     * detectedUnit val.
                     */
                    val detectedUnit = UnitConversion.detectUnitFromKey(key)
                    /**
                     * targetUnit val.
                     */
                    val targetUnit = targetUnits[key] ?: detectedUnit
                    /**
                     * unitMenuExpanded var.
                     */
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
                            /**
                             * label val.
                             */
                            val label = key.split("/").last()
                            /**
                             * unitSuffix val.
                             */
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

        /**
         * signalTree val.
         */
        val signalTree = remember(activeTopics.toList()) { buildSignalTree(activeTopics.toList()) }

        // Plot Canvas
        /**
         * minY var.
         */
        var minY by remember { mutableStateOf(0.0) }
        /**
         * maxY var.
         */
        var maxY by remember { mutableStateOf(1.0) }
        /**
         * firstKey val.
         */
        val firstKey = selectedKeys.firstOrNull()
        /**
         * currentUnitSymbol val.
         */
        val currentUnitSymbol = firstKey?.let { targetUnits[it] ?: UnitConversion.detectUnitFromKey(it) }?.symbol ?: ""

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AresSurfaceElevated)
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
        ) {
            // Signal Tree Explorer (Slide-out panel)
            Box(
                modifier = Modifier
                    .width(treeWidth)
                    .fillMaxHeight()
                    .background(AresSurface)
                    .clipToBounds()
            ) {
                if (treeWidth > 10.dp) {
                    SignalTreeExplorer(
                        rootNode = signalTree,
                        selectedKeys = selectedKeys,
                        onKeySelected = { key ->
                            /**
                             * cleanKey val.
                             */
                            val cleanKey = key.removePrefix("/")
                            if (selectedKeys.size < 8 && !selectedKeys.contains(cleanKey)) {
                                selectedKeys.add(cleanKey)
                            }
                        },
                        onDragStart = { key, offset ->
                            draggedKey = key.removePrefix("/")
                            dragOffset = offset
                        },
                        onDrag = { offset ->
                            dragOffset += offset
                        },
                        onDragEnd = {
                            /**
                             * finalOffset val.
                             */
                            val finalOffset = dragOffset
                            /**
                             * bounds val.
                             */
                            val bounds = canvasWindowBounds
                            if (bounds != null && bounds.contains(finalOffset)) {
                                draggedKey?.let { key ->
                                    /**
                                     * cleanKey val.
                                     */
                                    val cleanKey = key.removePrefix("/")
                                    if (selectedKeys.size < 8 && !selectedKeys.contains(cleanKey)) {
                                        selectedKeys.add(cleanKey)
                                    }
                                }
                            }
                            draggedKey = null
                        }
                    )
                }
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
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 12.dp).clipToBounds()) {
                        /**
                         * _tick val.
                         */
                        val _tick = liveTime
                        /**
                         * width val.
                         */
                        val width = size.width
                        /**
                         * height val.
                         */
                        val height = size.height
 
                    // 1. Draw Grid Lines
                    /**
                     * gridLinesX val.
                     */
                    val gridLinesX = 5
                    /**
                     * gridLinesY val.
                     */
                    val gridLinesY = 4
                    for (i in 0..gridLinesX) {
                        /**
                         * x val.
                         */
                        val x = width * i / gridLinesX
                        drawLine(color = AresBorder, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                    }
                    for (i in 0..gridLinesY) {
                        /**
                         * y val.
                         */
                        val y = height * i / gridLinesY
                        drawLine(color = AresBorder, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    }
 
                    // 2. Compute Global Bounds (Y-axis auto-scaling)
                    /**
                     * tempMinY var.
                     */
                    var tempMinY = Double.MAX_VALUE
                    /**
                     * tempMaxY var.
                     */
                    var tempMaxY = -Double.MAX_VALUE
                    /**
                     * hasData var.
                     */
                    var hasData = false
 
                    selectedKeys.forEach { key ->
                        /**
                         * deque val.
                         */
                        val deque = telemetryData[key]
                        /**
                         * points val.
                         */
                        val points = if (deque != null) synchronized(deque) { deque.toList() } else emptyList()
                        /**
                         * detectedUnit val.
                         */
                        val detectedUnit = UnitConversion.detectUnitFromKey(key)
                        /**
                         * targetUnit val.
                         */
                        val targetUnit = targetUnits[key] ?: detectedUnit
                        if (points.isNotEmpty()) {
                            hasData = true
                            points.forEach {
                                /**
                                 * value val.
                                 */
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
                    when {
                        !hasData -> {
                            tempMinY = 0.0
                            tempMaxY = 1.0
                        }
                        tempMinY == tempMaxY -> {
                            tempMinY -= 1.0
                            tempMaxY += 1.0
                        }
                        else -> {
                            // Add 10% padding
                            /**
                             * diff val.
                             */
                            val diff = tempMaxY - tempMinY
                            tempMinY -= diff * 0.1
                            tempMaxY += diff * 0.1
                        }
                    }

                    minY = tempMinY
                    maxY = tempMaxY
 
                    // 4. Plot each active channel
                    selectedKeys.forEachIndexed { channelIdx, key ->
                        /**
                         * deque val.
                         */
                        val deque = telemetryData[key]
                        /**
                         * points val.
                         */
                        val points = if (deque != null) synchronized(deque) { deque.toList() } else emptyList()
                        /**
                         * detectedUnit val.
                         */
                        val detectedUnit = UnitConversion.detectUnitFromKey(key)
                        /**
                         * targetUnit val.
                         */
                        val targetUnit = targetUnits[key] ?: detectedUnit
                        if (points.isNotEmpty()) {
                            /**
                             * color val.
                             */
                            val color = channelColors[channelIdx % channelColors.size]
                            /**
                             * now val.
                             */
                            val now = liveTime
                            /**
                             * minX val.
                             */
                            val minX = now - (selectedWindowSec * 1000)
                            /**
                             * maxX val.
                             */
                            val maxX = now
 
                            /**
                             * path val.
                             */
                            val path = Path()

                            /**
                             * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
                             * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
                             * Canvas-to-field coordinate transformation conventions applied where relevant.
                             *
                             * @param args relevant arguments
                             * @return expected results
                             */
                            fun getPy(value: Double): Float {
                                /**
                                 * converted val.
                                 */
                                val converted = if (detectedUnit != null && targetUnit != null) {
                                    UnitConversion.convert(value, detectedUnit, targetUnit)
                                } else {
                                    value
                                }
                                /**
                                 * yPct val.
                                 */
                                val yPct = ((converted - minY) / (maxY - minY)).toFloat()
                                return height - (yPct * height)
                            }

                            /**
                             * isFirst var.
                             */
                            var isFirst = true

                            // Find the first point that is visible (>= minX)
                            /**
                             * visibleStartIndex var.
                             */
                            var visibleStartIndex = points.indexOfFirst { it.timestampMs >= minX }
                            if (visibleStartIndex == -1) visibleStartIndex = points.size

                            // Include one point before minX to connect the line correctly off-screen
                            /**
                             * startIndex val.
                             */
                            val startIndex = kotlin.math.max(0, visibleStartIndex - 1)

                            // 1. Prepend virtual point at minX if the point before minX exists
                            if (startIndex < visibleStartIndex && points[startIndex].timestampMs < minX) {
                                /**
                                 * py val.
                                 */
                                val py = getPy(points[startIndex].value)
                                path.moveTo(0f, py)
                                isFirst = false
                            }

                            // 2. Draw points starting from startIndex
                            for (i in startIndex until points.size) {
                                /**
                                 * pt val.
                                 */
                                val pt = points[i]
                                /**
                                 * xPct val.
                                 */
                                val xPct = (pt.timestampMs - minX).toFloat() / (maxX - minX)
                                /**
                                 * px val.
                                 */
                                val px = xPct * width
                                /**
                                 * py val.
                                 */
                                val py = getPy(pt.value)
                                
                                if (isFirst) {
                                    path.moveTo(px, py)
                                    isFirst = false
                                } else {
                                    path.lineTo(px, py)
                                }
                            }

                            // 3. Append virtual point at maxX if the last point is older than maxX
                            /**
                             * lastPt val.
                             */
                            val lastPt = points.last()
                            if (lastPt.timestampMs < maxX) {
                                /**
                                 * py val.
                                 */
                                val py = getPy(lastPt.value)
                                if (isFirst) {
                                    path.moveTo(width, py)
                                } else {
                                    path.lineTo(width, py)
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
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun SignalTreeExplorer(
    rootNode: SignalNode,
    selectedKeys: List<String>,
    onKeySelected: (String) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    /**
     * expandedStates val.
     */
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        fun renderNode(node: SignalNode, depth: Int, path: String) {
            node.children.values.sortedBy { it.name }.forEach { child ->
                /**
                 * currentPath val.
                 */
                val currentPath = if (path.isEmpty()) child.name else "$path/${child.name}"
                /**
                 * isLeaf val.
                 */
                val isLeaf = child.isLeaf
                /**
                 * cleanPath val.
                 */
                val cleanPath = child.fullPath.removePrefix("/")
                /**
                 * isExpanded val.
                 */
                val isExpanded = expandedStates[currentPath] ?: false

                item(key = currentPath) {
                    /**
                     * nodeOffset var.
                     */
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
                                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
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

