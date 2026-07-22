package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*

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
