package com.ares.analytics.ui.components.pathplanner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ares.analytics.shared.League

object HeatmapOverlay {
    fun drawHeatmap(
        drawScope: DrawScope,
        actualPath: List<Waypoint>,
        fieldWidthM: Double,
        fieldHeightM: Double,
        league: League,
        gridSize: Int = 40,
        opacity: Float = 0.6f
    ) {
        if (actualPath.isEmpty()) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Compute grid bounds and counts
        val counts = Array(gridSize) { IntArray(gridSize) }
        var maxCount = 0

        actualPath.forEach { wp ->
            // Map robot coordinates to grid index
            val pctX = if (league == League.FTC) {
                ((wp.x + fieldWidthM / 2.0) / fieldWidthM).coerceIn(0.0, 1.0)
            } else {
                (wp.x / fieldWidthM).coerceIn(0.0, 1.0)
            }

            val pctY = if (league == League.FTC) {
                ((wp.y + fieldHeightM / 2.0) / fieldHeightM).coerceIn(0.0, 1.0)
            } else {
                ((fieldHeightM - wp.y) / fieldHeightM).coerceIn(0.0, 1.0)
            }

            val col = (pctX * (gridSize - 1)).toInt()
            val row = (pctY * (gridSize - 1)).toInt()

            counts[row][col]++
            if (counts[row][col] > maxCount) {
                maxCount = counts[row][col]
            }
        }

        if (maxCount == 0) return

        // 2. Draw density rectangles
        val cellW = width / gridSize
        val cellH = height / gridSize

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val count = counts[row][col]
                if (count > 0) {
                    val density = count.toDouble() / maxCount
                    val color = when {
                        density < 0.25 -> {
                            // Cool: Blue to Cyan
                            val t = (density / 0.25).toFloat()
                            Color(0f, t, 1f, opacity)
                        }
                        density < 0.5 -> {
                            // Mid-Cool: Cyan to Green
                            val t = ((density - 0.25) / 0.25).toFloat()
                            Color(0f, 1f, 1f - t, opacity)
                        }
                        density < 0.75 -> {
                            // Warm: Green to Yellow/Orange
                            val t = ((density - 0.5) / 0.25).toFloat()
                            Color(t, 1f, 0f, opacity)
                        }
                        else -> {
                            // Hot: Orange to Red
                            val t = ((density - 0.75) / 0.25).toFloat()
                            Color(1f, 1f - t, 0f, opacity)
                        }
                    }

                    drawScope.drawRect(
                        color = color,
                        topLeft = Offset(col * cellW, row * cellH),
                        size = Size(cellW, cellH)
                    )
                }
            }
        }
    }
}
