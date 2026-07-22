package com.ares.analytics.ui.components.pathplanner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ares.analytics.shared.League

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
object HeatmapOverlay {
    // Stateful grid for accumulation
    private val globalCounts = Array(100) { IntArray(100) }
    private var globalMaxCount = 0
    private var lastPathSize = 0
    private var lastFirstPoint: Waypoint? = null

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun resetHeatmap() {
        for (i in globalCounts.indices) {
            for (j in globalCounts[i].indices) {
                globalCounts[i][j] = 0
            }
        }
        globalMaxCount = 0
        lastPathSize = 0
        lastFirstPoint = null
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun drawHeatmap(
        drawScope: DrawScope,
        actualPath: List<Waypoint>,
        fieldWidthM: Double,
        fieldHeightM: Double,
        league: League,
        gridSize: Int = 100,
        opacity: Float = 0.6f
    ) {
        // Check if path was reset or if we have new points
        if (actualPath.isEmpty()) {
            resetHeatmap()
            return
        }

        /**
         * width val.
         */
        val width = drawScope.size.width
        /**
         * height val.
         */
        val height = drawScope.size.height
        
        // If the path seems completely new (e.g. simulation reset), clear the heatmap
        if (lastPathSize > actualPath.size || (actualPath.isNotEmpty() && lastFirstPoint != actualPath.first())) {
            resetHeatmap()
        }

        // Add only the NEW points to the heatmap
        /**
         * startIndex val.
         */
        val startIndex = if (lastPathSize <= actualPath.size && lastFirstPoint == actualPath.firstOrNull()) lastPathSize else 0
        
        for (i in startIndex until actualPath.size) {
            /**
             * wp val.
             */
            val wp = actualPath[i]
            /**
             * pctX val.
             */
            val pctX = if (league == League.FTC) {
                ((wp.x + fieldWidthM / 2.0) / fieldWidthM).coerceIn(0.0, 1.0)
            } else {
                (wp.x / fieldWidthM).coerceIn(0.0, 1.0)
            }

            /**
             * pctY val.
             */
            val pctY = if (league == League.FTC) {
                ((fieldHeightM / 2.0 - wp.y) / fieldHeightM).coerceIn(0.0, 1.0)
            } else {
                ((fieldHeightM - wp.y) / fieldHeightM).coerceIn(0.0, 1.0)
            }

            /**
             * col val.
             */
            val col = (pctX * (gridSize - 1)).toInt()
            /**
             * row val.
             */
            val row = (pctY * (gridSize - 1)).toInt()

            globalCounts[row][col]++
            if (globalCounts[row][col] > globalMaxCount) {
                globalMaxCount = globalCounts[row][col]
            }
        }

        lastPathSize = actualPath.size
        lastFirstPoint = actualPath.firstOrNull()

        if (globalMaxCount == 0) return

        // 2. Draw density rectangles
        /**
         * cellW val.
         */
        val cellW = width / gridSize
        /**
         * cellH val.
         */
        val cellH = height / gridSize

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                /**
                 * count val.
                 */
                val count = globalCounts[row][col]
                if (count > 0) {
                    /**
                     * density val.
                     */
                    val density = count.toDouble() / globalMaxCount
                    /**
                     * color val.
                     */
                    val color = when {
                        density < 0.25 -> {
                            /**
                             * t val.
                             */
                            val t = (density / 0.25).toFloat()
                            Color(0f, t, 1f, opacity)
                        }
                        density < 0.5 -> {
                            /**
                             * t val.
                             */
                            val t = ((density - 0.25) / 0.25).toFloat()
                            Color(0f, 1f, 1f - t, opacity)
                        }
                        density < 0.75 -> {
                            /**
                             * t val.
                             */
                            val t = ((density - 0.5) / 0.25).toFloat()
                            Color(t, 1f, 0f, opacity)
                        }
                        else -> {
                            /**
                             * t val.
                             */
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
