package com.ares.analytics.ui.components.triage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.HardwareTopology
import com.ares.analytics.shared.League
import com.ares.analytics.shared.TopologyNode
import com.ares.analytics.shared.TopologyNodeType
import com.ares.analytics.ui.theme.*
import kotlin.math.*

data class VisualNode(
    val node: TopologyNode,
    val x: Float,
    val y: Float,
    val radius: Float = 30f,
    val isHub: Boolean = false
)

@Composable
fun HardwareTopologyMap(
    league: League,
    topology: HardwareTopology?,
    faultyNodeId: String?,
    cascadingNodes: List<String> = emptyList(),
    onNodeSelected: (TopologyNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (topology == null || topology.nodes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hardware topology available. Connect robot or load session.", color = AresTextTertiary)
        }
        return
    }

    var selectedNode by remember { mutableStateOf<TopologyNode?>(null) }
    val visualNodes = remember(topology) { mutableListOf<VisualNode>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(topology) {
                detectTapGestures { offset ->
                    val clicked = visualNodes.firstOrNull { vn ->
                        val dist = sqrt((offset.x - vn.x).pow(2) + (offset.y - vn.y).pow(2))
                        dist < if (vn.isHub) 60f else vn.radius
                    }
                    if (clicked != null) {
                        selectedNode = clicked.node
                        onNodeSelected(clicked.node)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Background
        drawRect(color = AresSurface)

        // 1. Position Nodes
        val nodes = topology.nodes
        val mutableVisualNodes = mutableListOf<VisualNode>()

        if (league == League.FTC) {
            // FTC: Hub-Port Tree Layout
            // Position Control Hub in center, Expansion Hub to the right.
            val hubs = nodes.filter { it.type == TopologyNodeType.CONTROL_HUB || it.type == TopologyNodeType.EXPANSION_HUB }
            val hubMap = hubs.associateBy { it.id }

            hubs.forEachIndexed { hIdx, hub ->
                val hx = w * (0.3f + hIdx * 0.4f)
                val hy = h * 0.5f
                mutableVisualNodes.add(VisualNode(hub, hx, hy, radius = 50f, isHub = true))

                // Position child devices in a circle around their parent hub
                val children = nodes.filter { it.parentId == hub.id }
                children.forEachIndexed { cIdx, child ->
                    val angle = (2 * Math.PI * cIdx / maxOf(1, children.size))
                    val cx = hx + 180f * cos(angle).toFloat()
                    val cy = hy + 180f * sin(angle).toFloat()
                    mutableVisualNodes.add(VisualNode(child, cx, cy))
                }
            }
        } else {
            // FRC: CAN Bus Chain Layout
            // Position roboRIO on the left, then chain nodes horizontally.
            val rio = nodes.firstOrNull { it.type == TopologyNodeType.ROBORIO }
            val rioX = 100f
            val rioY = h * 0.5f

            if (rio != null) {
                mutableVisualNodes.add(VisualNode(rio, rioX, rioY, radius = 45f, isHub = true))
            }

            // Group by CAN bus name
            val canBuses = nodes.filter { it.canBus != null }.groupBy { it.canBus }
            canBuses.entries.forEachIndexed { bIdx, entry ->
                val busName = entry.key
                val busNodes = entry.value.sortedBy { it.busPosition ?: 0 }

                val startY = h * (0.3f + bIdx * 0.4f)
                busNodes.forEachIndexed { nIdx, node ->
                    val cx = rioX + 180f + nIdx * 150f
                    mutableVisualNodes.add(VisualNode(node, cx, startY))
                }
            }
        }

        visualNodes.clear()
        visualNodes.addAll(mutableVisualNodes)

        // 2. Draw Connection Edges
        visualNodes.forEach { vn ->
            val parentId = vn.node.parentId ?: return@forEach
            val parent = visualNodes.firstOrNull { it.node.id == parentId } ?: return@forEach

            // Check if link is broken/interrupted (e.g. if parent or child is faulty)
            val isLinkBroken = faultyNodeId != null && 
                (vn.node.id == faultyNodeId || parent.node.id == faultyNodeId)

            val edgeColor = if (isLinkBroken) AresError else AresBorderFocused

            drawLine(
                color = edgeColor,
                start = Offset(vn.x, vn.y),
                end = Offset(parent.x, parent.y),
                strokeWidth = 3f
            )

            // Draw port label for FTC port mapping
            if (vn.node.port != null) {
                val labelX = (vn.x + parent.x) / 2
                val labelY = (vn.y + parent.y) / 2
                // We'll let the Canvas render the port label in simple forms, or skip text overlays on Canvas to avoid font complexity
            }
        }

        // 3. Draw Nodes
        visualNodes.forEach { vn ->
            val isSelected = selectedNode?.id == vn.node.id
            val isFaulty = vn.node.id == faultyNodeId
            val isCascading = cascadingNodes.contains(vn.node.id)

            // Check if FRC CAN downstream node affected
            var isDownstreamFault = false
            if (league == League.FRC && faultyNodeId != null) {
                val faultyNode = nodes.firstOrNull { it.id == faultyNodeId }
                if (faultyNode != null && faultyNode.canBus != null && vn.node.canBus == faultyNode.canBus) {
                    val fPos = faultyNode.busPosition ?: 0
                    val currentPos = vn.node.busPosition ?: 0
                    if (currentPos > fPos) {
                        isDownstreamFault = true
                    }
                }
            }

            // Determine border color
            val nodeColor = when {
                isFaulty -> AresError
                isCascading || isDownstreamFault -> AresAmber
                isSelected -> AresCyan
                vn.isHub -> AresCyanGlow
                else -> AresBorderFocused
            }

            // Draw Node shapes
            if (vn.isHub) {
                // Large Rounded rectangle for hubs/controllers
                drawRoundRect(
                    color = AresSurfaceElevated,
                    topLeft = Offset(vn.x - 50f, vn.y - 40f),
                    size = Size(100f, 80f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = nodeColor,
                    topLeft = Offset(vn.x - 50f, vn.y - 40f),
                    size = Size(100f, 80f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 3f)
                )
            } else {
                // Circle for devices
                drawCircle(
                    color = AresSurfaceElevated,
                    radius = vn.radius,
                    center = Offset(vn.x, vn.y)
                )
                drawCircle(
                    color = nodeColor,
                    radius = vn.radius,
                    center = Offset(vn.x, vn.y),
                    style = Stroke(width = 3f)
                )
            }

            // Draw animated pulse overlay for faulty nodes
            if (isFaulty) {
                val pulseRadius = vn.radius + 15f * (1.0f + sin(System.currentTimeMillis() / 200.0).toFloat())
                drawCircle(
                    color = AresRedGlow,
                    radius = if (vn.isHub) pulseRadius + 20f else pulseRadius,
                    center = Offset(vn.x, vn.y),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
