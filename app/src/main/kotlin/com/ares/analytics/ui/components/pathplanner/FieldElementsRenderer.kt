package com.ares.analytics.ui.components.pathplanner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ares.analytics.shared.FieldImageConfig
import com.ares.analytics.shared.League
import com.ares.analytics.shared.Obstacle
import com.ares.analytics.shared.GamePiece
import com.ares.analytics.shared.PathPoint
import com.ares.analytics.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawFieldBackground(
    activeImage: ImageBitmap?,
    activeConfig: FieldImageConfig,
    w: Float,
    h: Float
) {
    activeImage?.let { img ->
        /**
         * cropL val.
         */
        val cropL = (activeConfig.cropLeft * img.width).toInt().coerceIn(0, img.width)
        /**
         * cropR val.
         */
        val cropR = (activeConfig.cropRight * img.width).toInt().coerceIn(cropL, img.width)
        /**
         * cropT val.
         */
        val cropT = (activeConfig.cropTop * img.height).toInt().coerceIn(0, img.height)
        /**
         * cropB val.
         */
        val cropB = (activeConfig.cropBottom * img.height).toInt().coerceIn(cropT, img.height)
        /**
         * srcW val.
         */
        val srcW = maxOf(1, cropR - cropL)
        /**
         * srcH val.
         */
        val srcH = maxOf(1, cropB - cropT)

        if (activeConfig.rotationDegrees != 0.0) {
            drawContext.canvas.save()
            drawContext.transform.rotate(activeConfig.rotationDegrees.toFloat(), Offset(w / 2f, h / 2f))
            drawImage(
                image = img,
                srcOffset = IntOffset(cropL, cropT),
                srcSize = IntSize(srcW, srcH),
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(w.toInt(), h.toInt())
            )
            drawContext.canvas.restore()
        } else {
            drawImage(
                image = img,
                srcOffset = IntOffset(cropL, cropT),
                srcSize = IntSize(srcW, srcH),
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(w.toInt(), h.toInt())
            )
        }
    } ?: run {
        drawRect(color = AresSurfaceElevated, size = size)
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawFieldGrid(
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    showCostmap: Boolean = false
) {
    /**
     * stepX val.
     */
    val stepX = if (league == League.FTC) fieldWidthM / 6.0 else 1.0
    /**
     * stepY val.
     */
    val stepY = if (league == League.FTC) fieldHeightM / 6.0 else 1.0
    
    /**
     * curX var.
     */
    var curX = if (league == League.FTC) -fieldWidthM/2 else 0.0
    while (curX <= fieldWidthM/2 + 0.001) {
        /**
         * wp val.
         */
        val wp = Waypoint(curX, 0.0)
        /**
         * offset val.
         */
        val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
        drawLine(color = AresBorder, start = Offset(offset.x, 0f), end = Offset(offset.x, h), strokeWidth = 1f)
        curX += stepX
    }
    /**
     * curY var.
     */
    var curY = if (league == League.FTC) -fieldHeightM/2 else 0.0
    while (curY <= fieldHeightM/2 + 0.001) {
        /**
         * wp val.
         */
        val wp = Waypoint(0.0, curY)
        /**
         * offset val.
         */
        val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
        drawLine(color = AresBorder, start = Offset(0f, offset.y), end = Offset(w, offset.y), strokeWidth = 1f)
        curY += stepY
    }
    drawRect(color = AresBorderFocused, style = Stroke(width = 3f))

    if (showCostmap) {
        /**
         * robotRadius val.
         */
        val robotRadius = if (league == League.FTC) 0.25 else 0.45
        /**
         * robotRadiusPxX val.
         */
        val robotRadiusPxX = (robotRadius / fieldWidthM) * w
        /**
         * robotRadiusPxY val.
         */
        val robotRadiusPxY = (robotRadius / fieldHeightM) * h
        /**
         * insetOffset val.
         */
        val insetOffset = Offset(robotRadiusPxX.toFloat(), robotRadiusPxY.toFloat())
        /**
         * insetSize val.
         */
        val insetSize = Size((w - 2.0 * robotRadiusPxX).toFloat(), (h - 2.0 * robotRadiusPxY).toFloat())
        
        drawRect(
            color = AresRed.copy(alpha = 0.25f),
            topLeft = insetOffset,
            size = insetSize,
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawFtcAllianceStations(
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    activeConfig: FieldImageConfig
) {
    if (league != League.FTC) return
    /**
     * strokeW val.
     */
    val strokeW = 12f
    /**
     * redColor val.
     */
    val redColor = AresRed.copy(alpha = 0.7f)
    /**
     * blueColor val.
     */
    val blueColor = AresCyan.copy(alpha = 0.7f)
    
    when (activeConfig.ftcCoordinateSystem) {
        com.ares.analytics.shared.FTCCoordinateSystem.SQUARE -> {
            /**
             * redStart val.
             */
            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            /**
             * redEnd val.
             */
            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
            
            /**
             * blueStart val.
             */
            val blueStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            /**
             * blueEnd val.
             */
            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
        }
        com.ares.analytics.shared.FTCCoordinateSystem.DIAMOND -> {
            /**
             * redStart val.
             */
            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            /**
             * redEnd val.
             */
            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
            
            /**
             * blueStart val.
             */
            val blueStart = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            /**
             * blueEnd val.
             */
            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
        }
        null -> {}
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawCustomObstacles(
    activeObstacles: List<Obstacle>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    showCostmap: Boolean = false
) {
    /**
     * robotRadius val.
     */
    val robotRadius = if (league == League.FTC) 0.25 else 0.45
    /**
     * robotRadiusPx val.
     */
    val robotRadiusPx = (robotRadius / fieldWidthM) * w

    activeObstacles.forEach { obs ->
        /**
         * baseColor val.
         */
        val baseColor = try {
            /**
             * clean val.
             */
            val clean = obs.colorHex.removePrefix("#")
            /**
             * colorInt val.
             */
            val colorInt = clean.toLong(16)
            if (clean.length == 6) {
                Color(0xFF000000 or colorInt)
            } else {
                Color(colorInt)
            }
        } catch (e: Exception) {
            AresRed
        }
        /**
         * fillColor val.
         */
        val fillColor = baseColor.copy(alpha = 0.3f)
        /**
         * costmapFillColor val.
         */
        val costmapFillColor = baseColor.copy(alpha = 0.08f)
        /**
         * costmapStrokeColor val.
         */
        val costmapStrokeColor = baseColor.copy(alpha = 0.25f)

        when (obs) {
            is Obstacle.Circle -> {
                /**
                 * centerWp val.
                 */
                val centerWp = Waypoint(obs.centerX, obs.centerY)
                /**
                 * centerOffset val.
                 */
                val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                /**
                 * radiusPx val.
                 */
                val radiusPx = (obs.radius / fieldWidthM) * w
                
                if (showCostmap) {
                    /**
                     * inflatedRadius val.
                     */
                    val inflatedRadius = radiusPx + robotRadiusPx
                    drawCircle(color = costmapFillColor, radius = inflatedRadius.toFloat(), center = centerOffset)
                    drawCircle(
                        color = costmapStrokeColor,
                        radius = inflatedRadius.toFloat(),
                        center = centerOffset,
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }

                drawCircle(color = fillColor, radius = radiusPx.toFloat(), center = centerOffset)
                drawCircle(color = baseColor, radius = radiusPx.toFloat(), center = centerOffset, style = Stroke(width = 2f))
            }
            is Obstacle.Rectangle -> {
                /**
                 * centerWp val.
                 */
                val centerWp = Waypoint(obs.centerX, obs.centerY)
                /**
                 * centerOffset val.
                 */
                val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                
                /**
                 * rw val.
                 */
                val rw: Double
                /**
                 * rh val.
                 */
                val rh: Double
                if (league == League.FTC) {
                    rw = (obs.height / fieldWidthM) * w
                    rh = (obs.width / fieldHeightM) * h
                } else {
                    rw = (obs.width / fieldWidthM) * w
                    rh = (obs.height / fieldHeightM) * h
                }
                
                if (showCostmap) {
                    /**
                     * inflatedRw val.
                     */
                    val inflatedRw = rw + 2.0 * robotRadiusPx
                    /**
                     * inflatedRh val.
                     */
                    val inflatedRh = rh + 2.0 * robotRadiusPx
                    
                    drawContext.canvas.save()
                    /**
                     * drawRot val.
                     */
                    val drawRot = -obs.rotation.toFloat() 
                    drawContext.transform.rotate(drawRot, pivot = centerOffset)
                    
                    /**
                     * rectOffset val.
                     */
                    val rectOffset = Offset((centerOffset.x - inflatedRw / 2).toFloat(), (centerOffset.y - inflatedRh / 2).toFloat())
                    /**
                     * roundedCorner val.
                     */
                    val roundedCorner = androidx.compose.ui.geometry.CornerRadius(robotRadiusPx.toFloat(), robotRadiusPx.toFloat())
                    
                    drawRoundRect(
                        color = costmapFillColor,
                        topLeft = rectOffset,
                        size = Size(inflatedRw.toFloat(), inflatedRh.toFloat()),
                        cornerRadius = roundedCorner
                    )
                    drawRoundRect(
                        color = costmapStrokeColor,
                        topLeft = rectOffset,
                        size = Size(inflatedRw.toFloat(), inflatedRh.toFloat()),
                        cornerRadius = roundedCorner,
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                    drawContext.canvas.restore()
                }

                drawContext.canvas.save()
                /**
                 * drawRot val.
                 */
                val drawRot = -obs.rotation.toFloat() 
                drawContext.transform.rotate(drawRot, pivot = centerOffset)
                
                /**
                 * rectOffset val.
                 */
                val rectOffset = Offset((centerOffset.x - rw / 2).toFloat(), (centerOffset.y - rh / 2).toFloat())
                drawRect(color = fillColor, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = baseColor, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                drawContext.canvas.restore()
            }
            is Obstacle.Polygon -> {
                if (obs.vertices.isNotEmpty()) {
                    /**
                     * path val.
                     */
                    val path = Path()
                    /**
                     * start val.
                     */
                    val start = getCanvasOffsetBase(Waypoint(obs.vertices.first().x, obs.vertices.first().y), w, h, fieldWidthM, fieldHeightM, league)
                    path.moveTo(start.x, start.y)
                    obs.vertices.drop(1).forEach { pt ->
                        /**
                         * offset val.
                         */
                        val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
                        path.lineTo(offset.x, offset.y)
                    }
                    path.close()
                    
                    if (showCostmap) {
                        drawPath(
                            path = path,
                            color = costmapFillColor,
                            style = Stroke(
                                width = (robotRadiusPx * 2).toFloat(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                        drawPath(
                            path = path,
                            color = costmapStrokeColor,
                            style = Stroke(
                                width = (robotRadiusPx * 2).toFloat(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }

                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = baseColor, style = Stroke(width = 2f))
                }
            }
        }
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawGamePieces(
    activeGamePieces: List<GamePiece>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    activeGamePieces.forEach { gp ->
        /**
         * gpOffset val.
         */
        val gpOffset = getCanvasOffsetBase(Waypoint(gp.x, gp.y), w, h, fieldWidthM, fieldHeightM, league)
        when (gp.type) {
            "Note", "High Note" -> {
                /**
                 * outerRadiusPx val.
                 */
                val outerRadiusPx = (0.175 / fieldWidthM) * w
                /**
                 * innerRadiusPx val.
                 */
                val innerRadiusPx = (0.07 / fieldWidthM) * w
                drawCircle(color = AresAmber.copy(alpha = 0.4f), radius = outerRadiusPx.toFloat(), center = gpOffset)
                drawCircle(color = AresAmber, radius = outerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 3f))
                drawCircle(color = AresBackground, radius = innerRadiusPx.toFloat(), center = gpOffset)
                drawCircle(color = AresAmber, radius = innerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 1.5f))
            }
            "Sample (Yellow)" -> {
                /**
                 * rw val.
                 */
                val rw = (0.15 / fieldWidthM) * w
                /**
                 * rh val.
                 */
                val rh = (0.05 / fieldHeightM) * h
                /**
                 * rectOffset val.
                 */
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = Color.Yellow.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = Color.Yellow, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Sample (Red)" -> {
                /**
                 * rw val.
                 */
                val rw = (0.15 / fieldWidthM) * w
                /**
                 * rh val.
                 */
                val rh = (0.05 / fieldHeightM) * h
                /**
                 * rectOffset val.
                 */
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = AresRed.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = AresRed, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Sample (Blue)" -> {
                /**
                 * rw val.
                 */
                val rw = (0.15 / fieldWidthM) * w
                /**
                 * rh val.
                 */
                val rh = (0.05 / fieldHeightM) * h
                /**
                 * rectOffset val.
                 */
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = AresCyan.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = AresCyan, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Decode (Ball)" -> {
                /**
                 * radiusPx val.
                 */
                val radiusPx = (0.0635 / fieldWidthM) * w
                drawCircle(color = Color(0xFF9C27B0).copy(alpha = 0.6f), radius = radiusPx.toFloat(), center = gpOffset)
                drawCircle(color = Color(0xFF9C27B0), radius = radiusPx.toFloat(), center = gpOffset, style = Stroke(width = 2f))
            }
            else -> {
                /**
                 * sizePx val.
                 */
                val sizePx = (0.12 / fieldWidthM) * w
                /**
                 * path val.
                 */
                val path = Path().apply {
                    moveTo(gpOffset.x, (gpOffset.y - sizePx).toFloat())
                    lineTo((gpOffset.x + sizePx).toFloat(), gpOffset.y)
                    lineTo(gpOffset.x, (gpOffset.y + sizePx).toFloat())
                    lineTo((gpOffset.x - sizePx).toFloat(), gpOffset.y)
                    close()
                }
                drawPath(path = path, color = Color(0xFF9C27B0).copy(alpha = 0.6f))
                drawPath(path = path, color = Color(0xFF9C27B0), style = Stroke(width = 2f))
            }
        }
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawAprilTags(
    activeAprilTags: List<com.ares.analytics.shared.AprilTagPlacement>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    textMeasurer: TextMeasurer
) {
    activeAprilTags.forEach { at ->
        /**
         * atOffset val.
         */
        val atOffset = getCanvasOffsetBase(Waypoint(at.x, at.y), w, h, fieldWidthM, fieldHeightM, league)
        /**
         * rw val.
         */
        val rw = (0.15 / fieldWidthM) * w
        /**
         * rh val.
         */
        val rh = (0.15 / fieldHeightM) * h
        
        drawContext.canvas.save()
        drawContext.transform.rotate(degrees = -at.yawDegrees.toFloat(), pivot = atOffset)
        
        /**
         * rectOffset val.
         */
        val rectOffset = Offset((atOffset.x - rw/2).toFloat(), (atOffset.y - rh/2).toFloat())
        drawRect(color = Color.White, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
        
        /**
         * innerOffset val.
         */
        val innerOffset = Offset((atOffset.x - rw/2 + 2).toFloat(), (atOffset.y - rh/2 + 2).toFloat())
        /**
         * innerW val.
         */
        val innerW = rw - 4
        /**
         * innerH val.
         */
        val innerH = rh - 4
        drawRect(color = Color.Black, topLeft = innerOffset, size = Size(innerW.toFloat(), innerH.toFloat()))
        
        // Stylized AprilTag inner pattern
        /**
         * blockW val.
         */
        val blockW = innerW / 6.0
        /**
         * blockH val.
         */
        val blockH = innerH / 6.0
        /**
         * activeBlocks val.
         */
        val activeBlocks = listOf(
            Pair(1, 1), Pair(2, 1), Pair(4, 1),
            Pair(1, 2), Pair(3, 2),
            Pair(2, 3), Pair(4, 3),
            Pair(1, 4), Pair(3, 4), Pair(4, 4)
        )
        activeBlocks.forEach { (bx, by) ->
            drawRect(
                color = Color.White,
                topLeft = Offset(
                    (innerOffset.x + bx * blockW).toFloat(),
                    (innerOffset.y + by * blockH).toFloat()
                ),
                size = Size(blockW.toFloat(), blockH.toFloat())
            )
        }
        
        drawContext.canvas.restore()
        
        // Display Tag ID Label
        /**
         * textStyle val.
         */
        val textStyle = TextStyle(
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        /**
         * labelText val.
         */
        val labelText = "#${at.tagId}"
        /**
         * textLayout val.
         */
        val textLayout = textMeasurer.measure(labelText, textStyle)
        /**
         * labelOffset val.
         */
        val labelOffset = Offset(
            (atOffset.x - textLayout.size.width / 2f).toFloat(),
            (atOffset.y - rh/2 - textLayout.size.height - 4).toFloat()
        )
        
        drawRoundRect(
            color = Color(0xCC212121),
            topLeft = Offset(labelOffset.x - 4, labelOffset.y - 2),
            size = Size((textLayout.size.width + 8).toFloat(), (textLayout.size.height + 4).toFloat()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        
        drawText(
            textMeasurer = textMeasurer,
            text = labelText,
            style = textStyle,
            topLeft = labelOffset
        )
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawActivePolygonPoints(
    currentPolygonPoints: List<PathPoint>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    if (currentPolygonPoints.isNotEmpty()) {
        /**
         * path val.
         */
        val path = Path()
        /**
         * start val.
         */
        val start = getCanvasOffsetBase(Waypoint(currentPolygonPoints.first().x, currentPolygonPoints.first().y), w, h, fieldWidthM, fieldHeightM, league)
        path.moveTo(start.x, start.y)
        currentPolygonPoints.drop(1).forEach { pt ->
            /**
             * offset val.
             */
            val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
            path.lineTo(offset.x, offset.y)
        }
        drawPath(path = path, color = AresRed.copy(alpha = 0.15f))
        drawPath(path = path, color = AresRed, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawFieldWaypoints(
    fieldWaypoints: List<com.ares.analytics.shared.FieldWaypoint>,
    selectedId: String?,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    textMeasurer: TextMeasurer
) {
    fieldWaypoints.forEach { wp ->
        /**
         * offset val.
         */
        val offset = getCanvasOffsetBase(Waypoint(wp.x, wp.y), w, h, fieldWidthM, fieldHeightM, league)
        /**
         * radius val.
         */
        val radius = 10.dp.toPx()
        /**
         * isSelected val.
         */
        val isSelected = wp.id == selectedId
        /**
         * baseColor val.
         */
        val baseColor = if (isSelected) AresCyan else Color(0xFF00E676) // Cyan when selected, neon green when not

        // Waypoint name label
        /**
         * textStyle val.
         */
        val textStyle = TextStyle(
            color = baseColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        /**
         * textLayout val.
         */
        val textLayout = textMeasurer.measure(wp.name, textStyle)
        /**
         * textOffset val.
         */
        val textOffset = Offset(
            (offset.x + radius + 6).toFloat(),
            (offset.y - textLayout.size.height / 2f).toFloat()
        )
        
        drawRoundRect(
            color = Color(0xCC212121),
            topLeft = Offset(textOffset.x - 3, textOffset.y - 1),
            size = Size((textLayout.size.width + 6).toFloat(), (textLayout.size.height + 2).toFloat()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        
        drawText(
            textMeasurer = textMeasurer,
            text = wp.name,
            style = textStyle,
            topLeft = textOffset
        )

        // Draw reticle circle
        drawCircle(
            color = baseColor.copy(alpha = 0.2f),
            center = offset,
            radius = radius
        )
        drawCircle(
            color = baseColor,
            center = offset,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = baseColor,
            center = offset,
            radius = 3.dp.toPx()
        )

        // Draw heading line and arrow pointer
        /**
         * angleRad val.
         */
        val angleRad = Math.toRadians(-wp.headingDegrees - 90.0)
        /**
         * cosA val.
         */
        val cosA = kotlin.math.cos(angleRad).toFloat()
        /**
         * sinA val.
         */
        val sinA = kotlin.math.sin(angleRad).toFloat()
        /**
         * pointerLen val.
         */
        val pointerLen = 22.dp.toPx()
        /**
         * pointerEnd val.
         */
        val pointerEnd = Offset(
            offset.x + pointerLen * cosA,
            offset.y + pointerLen * sinA
        )
        drawLine(
            color = baseColor,
            start = offset,
            end = pointerEnd,
            strokeWidth = 2.dp.toPx()
        )
        // Draw small arrowhead
        /**
         * arrowSize val.
         */
        val arrowSize = 6.dp.toPx()
        /**
         * arrowAngle1 val.
         */
        val arrowAngle1 = angleRad + Math.toRadians(145.0)
        /**
         * arrowAngle2 val.
         */
        val arrowAngle2 = angleRad - Math.toRadians(145.0)
        /**
         * cosA1 val.
         */
        val cosA1 = kotlin.math.cos(arrowAngle1).toFloat()
        /**
         * sinA1 val.
         */
        val sinA1 = kotlin.math.sin(arrowAngle1).toFloat()
        /**
         * cosA2 val.
         */
        val cosA2 = kotlin.math.cos(arrowAngle2).toFloat()
        /**
         * sinA2 val.
         */
        val sinA2 = kotlin.math.sin(arrowAngle2).toFloat()
        drawLine(
            color = baseColor,
            start = pointerEnd,
            end = Offset(
                pointerEnd.x + arrowSize * cosA1,
                pointerEnd.y + arrowSize * sinA1
            ),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = baseColor,
            start = pointerEnd,
            end = Offset(
                pointerEnd.x + arrowSize * cosA2,
                pointerEnd.y + arrowSize * sinA2
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}
