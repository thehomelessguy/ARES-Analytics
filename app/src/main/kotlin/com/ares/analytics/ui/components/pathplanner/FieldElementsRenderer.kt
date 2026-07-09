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

fun DrawScope.drawFieldBackground(
    activeImage: ImageBitmap?,
    activeConfig: FieldImageConfig,
    w: Float,
    h: Float
) {
    activeImage?.let { img ->
        val cropL = (activeConfig.cropLeft * img.width).toInt().coerceIn(0, img.width)
        val cropR = (activeConfig.cropRight * img.width).toInt().coerceIn(cropL, img.width)
        val cropT = (activeConfig.cropTop * img.height).toInt().coerceIn(0, img.height)
        val cropB = (activeConfig.cropBottom * img.height).toInt().coerceIn(cropT, img.height)
        val srcW = maxOf(1, cropR - cropL)
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

fun DrawScope.drawFieldGrid(
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    val stepX = if (league == League.FTC) fieldWidthM / 6.0 else 1.0
    val stepY = if (league == League.FTC) fieldHeightM / 6.0 else 1.0
    
    var curX = if (league == League.FTC) -fieldWidthM/2 else 0.0
    while (curX <= fieldWidthM/2 + 0.001) {
        val wp = Waypoint(curX, 0.0)
        val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
        drawLine(color = AresBorder, start = Offset(offset.x, 0f), end = Offset(offset.x, h), strokeWidth = 1f)
        curX += stepX
    }
    var curY = if (league == League.FTC) -fieldHeightM/2 else 0.0
    while (curY <= fieldHeightM/2 + 0.001) {
        val wp = Waypoint(0.0, curY)
        val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
        drawLine(color = AresBorder, start = Offset(0f, offset.y), end = Offset(w, offset.y), strokeWidth = 1f)
        curY += stepY
    }
    drawRect(color = AresBorderFocused, style = Stroke(width = 3f))
}

fun DrawScope.drawFtcAllianceStations(
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League,
    activeConfig: FieldImageConfig
) {
    if (league != League.FTC) return
    val strokeW = 12f
    val redColor = AresRed.copy(alpha = 0.7f)
    val blueColor = AresCyan.copy(alpha = 0.7f)
    
    when (activeConfig.ftcCoordinateSystem) {
        com.ares.analytics.shared.FTCCoordinateSystem.SQUARE -> {
            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
            
            val blueStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
        }
        com.ares.analytics.shared.FTCCoordinateSystem.DIAMOND -> {
            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
            
            val blueStart = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
        }
        null -> {}
    }
}

fun DrawScope.drawCustomObstacles(
    activeObstacles: List<Obstacle>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    activeObstacles.forEach { obs ->
        when (obs) {
            is Obstacle.Circle -> {
                val centerWp = Waypoint(obs.centerX, obs.centerY)
                val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                val radiusPx = (obs.radius / fieldWidthM) * w
                drawCircle(color = AresRed.copy(alpha = 0.3f), radius = radiusPx.toFloat(), center = centerOffset)
                drawCircle(color = AresRed, radius = radiusPx.toFloat(), center = centerOffset, style = Stroke(width = 2f))
            }
            is Obstacle.Rectangle -> {
                val centerWp = Waypoint(obs.centerX, obs.centerY)
                val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                
                val rw: Double
                val rh: Double
                if (league == League.FTC) {
                    // FTC: Canvas X corresponds to Field Y, Canvas Y corresponds to Field X
                    rw = (obs.height / fieldWidthM) * w
                    rh = (obs.width / fieldHeightM) * h
                } else {
                    // FRC: Canvas X corresponds to Field X, Canvas Y corresponds to Field Y
                    rw = (obs.width / fieldWidthM) * w
                    rh = (obs.height / fieldHeightM) * h
                }
                
                drawContext.canvas.save()
                
                // Rotation drawing: in FTC, heading 0 is +X (which points UP on screen).
                // A rectangle with 0 rotation should have its length (obs.width) aligned with +X (UP).
                // Since we swapped rw and rh above, rh is the visual size along Canvas Y (UP/DOWN).
                // So rh is already visually the obs.width.
                // When rotation = 0, we just draw it.
                // Note: -obs.rotation is used in hit detection. 
                val drawRot = if (league == League.FTC) -obs.rotation.toFloat() else -obs.rotation.toFloat() 
                drawContext.transform.rotate(drawRot, pivot = centerOffset)
                
                val rectOffset = Offset((centerOffset.x - rw / 2).toFloat(), (centerOffset.y - rh / 2).toFloat())
                drawRect(color = AresRed.copy(alpha = 0.3f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = AresRed, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                drawContext.canvas.restore()
            }
            is Obstacle.Polygon -> {
                if (obs.vertices.isNotEmpty()) {
                    val path = Path()
                    val start = getCanvasOffsetBase(Waypoint(obs.vertices.first().x, obs.vertices.first().y), w, h, fieldWidthM, fieldHeightM, league)
                    path.moveTo(start.x, start.y)
                    obs.vertices.drop(1).forEach { pt ->
                        val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
                        path.lineTo(offset.x, offset.y)
                    }
                    path.close()
                    drawPath(path = path, color = AresRed.copy(alpha = 0.3f))
                    drawPath(path = path, color = AresRed, style = Stroke(width = 2f))
                }
            }
        }
    }
}

fun DrawScope.drawGamePieces(
    activeGamePieces: List<GamePiece>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    activeGamePieces.forEach { gp ->
        val gpOffset = getCanvasOffsetBase(Waypoint(gp.x, gp.y), w, h, fieldWidthM, fieldHeightM, league)
        when (gp.type) {
            "Note", "High Note" -> {
                val outerRadiusPx = (0.175 / fieldWidthM) * w
                val innerRadiusPx = (0.07 / fieldWidthM) * w
                drawCircle(color = AresAmber.copy(alpha = 0.4f), radius = outerRadiusPx.toFloat(), center = gpOffset)
                drawCircle(color = AresAmber, radius = outerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 3f))
                drawCircle(color = AresBackground, radius = innerRadiusPx.toFloat(), center = gpOffset)
                drawCircle(color = AresAmber, radius = innerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 1.5f))
            }
            "Sample (Yellow)" -> {
                val rw = (0.15 / fieldWidthM) * w
                val rh = (0.05 / fieldHeightM) * h
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = Color.Yellow.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = Color.Yellow, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Sample (Red)" -> {
                val rw = (0.15 / fieldWidthM) * w
                val rh = (0.05 / fieldHeightM) * h
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = AresRed.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = AresRed, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Sample (Blue)" -> {
                val rw = (0.15 / fieldWidthM) * w
                val rh = (0.05 / fieldHeightM) * h
                val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                drawRect(color = AresCyan.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                drawRect(color = AresCyan, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
            }
            "Decode (Ball)" -> {
                val radiusPx = (0.0635 / fieldWidthM) * w
                drawCircle(color = Color(0xFF9C27B0).copy(alpha = 0.6f), radius = radiusPx.toFloat(), center = gpOffset)
                drawCircle(color = Color(0xFF9C27B0), radius = radiusPx.toFloat(), center = gpOffset, style = Stroke(width = 2f))
            }
            else -> {
                val sizePx = (0.12 / fieldWidthM) * w
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

fun DrawScope.drawAprilTags(
    activeAprilTags: List<com.ares.analytics.shared.AprilTagPlacement>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    activeAprilTags.forEach { at ->
        val atOffset = getCanvasOffsetBase(Waypoint(at.x, at.y), w, h, fieldWidthM, fieldHeightM, league)
        val rw = (0.15 / fieldWidthM) * w
        val rh = (0.15 / fieldHeightM) * h
        
        drawContext.canvas.save()
        drawContext.transform.rotate(degrees = -at.yawDegrees.toFloat(), pivot = atOffset)
        
        val rectOffset = Offset((atOffset.x - rw/2).toFloat(), (atOffset.y - rh/2).toFloat())
        drawRect(color = Color.White, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
        val innerOffset = Offset((atOffset.x - rw/2 + 2).toFloat(), (atOffset.y - rh/2 + 2).toFloat())
        drawRect(color = Color.Black, topLeft = innerOffset, size = Size((rw - 4).toFloat(), (rh - 4).toFloat()))
        
        drawContext.canvas.restore()
    }
}

fun DrawScope.drawActivePolygonPoints(
    currentPolygonPoints: List<PathPoint>,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    if (currentPolygonPoints.isNotEmpty()) {
        val path = Path()
        val start = getCanvasOffsetBase(Waypoint(currentPolygonPoints.first().x, currentPolygonPoints.first().y), w, h, fieldWidthM, fieldHeightM, league)
        path.moveTo(start.x, start.y)
        currentPolygonPoints.drop(1).forEach { pt ->
            val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
            path.lineTo(offset.x, offset.y)
        }
        drawPath(path = path, color = AresRed.copy(alpha = 0.15f))
        drawPath(path = path, color = AresRed, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
    }
}

fun DrawScope.drawFieldWaypoints(
    fieldWaypoints: List<com.ares.analytics.shared.FieldWaypoint>,
    selectedId: String?,
    w: Float,
    h: Float,
    fieldWidthM: Double,
    fieldHeightM: Double,
    league: League
) {
    fieldWaypoints.forEach { wp ->
        val offset = getCanvasOffsetBase(Waypoint(wp.x, wp.y), w, h, fieldWidthM, fieldHeightM, league)
        val radius = 10.dp.toPx()
        val isSelected = wp.id == selectedId
        val baseColor = if (isSelected) AresCyan else Color(0xFF00E676) // Cyan when selected, neon green when not

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
        val angleRad = Math.toRadians(-wp.headingDegrees - 90.0)
        val cosA = kotlin.math.cos(angleRad).toFloat()
        val sinA = kotlin.math.sin(angleRad).toFloat()
        val pointerLen = 22.dp.toPx()
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
        val arrowSize = 6.dp.toPx()
        val arrowAngle1 = angleRad + Math.toRadians(145.0)
        val arrowAngle2 = angleRad - Math.toRadians(145.0)
        val cosA1 = kotlin.math.cos(arrowAngle1).toFloat()
        val sinA1 = kotlin.math.sin(arrowAngle1).toFloat()
        val cosA2 = kotlin.math.cos(arrowAngle2).toFloat()
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
