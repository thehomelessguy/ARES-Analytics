package com.ares.analytics.ui.components.pathplanner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.League
import com.ares.analytics.ui.theme.*

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class EditorMode {
    SELECT, ADD_WAYPOINT, DRAW_POLYGON, DRAW_CIRCLE, DRAW_RECTANGLE, PLACE_GAME_PIECE, PLACE_APRILTAG, PLACE_FIELD_WAYPOINT, ERASER
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PointTowardsZoneRenderData(
    /**
     * target val.
     */
    val target: Waypoint,
    /**
     * splinePoints val.
     */
    val splinePoints: List<Waypoint>
)

// Precomputed Cache Data Structures for zero-allocation rendering performance
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class PathCacheHolder {
    /**
     * splinePoints var.
     */
    var splinePoints: List<Waypoint> = emptyList()
    /**
     * actualPoints var.
     */
    var actualPoints: List<Waypoint> = emptyList()
    /**
     * constraintSplines var.
     */
    var constraintSplines: List<List<Waypoint>> = emptyList()
    /**
     * w var.
     */
    var w: Float = 0f
    /**
     * h var.
     */
    var h: Float = 0f
    /**
     * splinePath var.
     */
    var splinePath: Path? = null
    /**
     * actualPath var.
     */
    var actualPath: Path? = null
    /**
     * constraintPaths var.
     */
    var constraintPaths: List<Path> = emptyList()

    /**
     * reusableArrowPath val.
     */
    val reusableArrowPath = Path()
    /**
     * reusableDiamondPath val.
     */
    val reusableDiamondPath = Path()
    /**
     * reusableXAxisPath val.
     */
    val reusableXAxisPath = Path()
    /**
     * reusableYAxisPath val.
     */
    val reusableYAxisPath = Path()
    /**
     * reusablePath val.
     */
    val reusablePath = Path()

    /**
     * dashEffect10 val.
     */
    val dashEffect10 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    /**
     * dashEffect8 val.
     */
    val dashEffect8 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    /**
     * dashEffect5 val.
     */
    val dashEffect5 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
    /**
     * dashEffect4 val.
     */
    val dashEffect4 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    /**
     * dashEffect6_4 val.
     */
    val dashEffect6_4 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
    /**
     * dashEffect4_3 val.
     */
    val dashEffect4_3 = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
}

// Convert coordinates base models
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun getCanvasOffsetBase(
    wp: Waypoint,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Offset {
    return if (league == League.FTC) {
        /**
         * x val.
         */
        val x = (-wp.y / fieldW + 0.5) * canvasW
        /**
         * y val.
         */
        val y = (-wp.x / fieldH + 0.5) * canvasH
        Offset(x.toFloat(), y.toFloat())
    } else {
        /**
         * x val.
         */
        val x = (wp.x / fieldW) * canvasW
        /**
         * y val.
         */
        val y = ((fieldH - wp.y) / fieldH) * canvasH
        Offset(x.toFloat(), y.toFloat())
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
fun getRobotCoordBase(
    offset: Offset,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Waypoint {
    return if (league == League.FTC) {
        /**
         * rx val.
         */
        val rx = -(offset.y / canvasH - 0.5) * fieldH
        /**
         * ry val.
         */
        val ry = -(offset.x / canvasW - 0.5) * fieldW
        Waypoint(rx, ry)
    } else {
        /**
         * rx val.
         */
        val rx = (offset.x / canvasW) * fieldW
        /**
         * ry val.
         */
        val ry = fieldH - (offset.y / canvasH) * fieldH
        Waypoint(rx, ry)
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
fun getTransformedCanvasOffset(
    wp: Waypoint, 
    w: Float, 
    h: Float, 
    fieldWidthM: Double, 
    fieldHeightM: Double, 
    league: League, 
    zoomScale: Float, 
    panOffset: Offset,
    viewRotationDeg: Float = 0f
): Offset {
    /**
     * base val.
     */
    val base = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
    // Apply zoom scale and pan offset
    /**
     * x var.
     */
    var x = base.x * zoomScale + panOffset.x
    /**
     * y var.
     */
    var y = base.y * zoomScale + panOffset.y
    // Apply view rotation around canvas center (matching the draw transform)
    if (viewRotationDeg != 0f) {
        /**
         * cx val.
         */
        val cx = w / 2f; val cy = h / 2f
        /**
         * rad val.
         */
        val rad = Math.toRadians(viewRotationDeg.toDouble())
        /**
         * cosR val.
         */
        val cosR = kotlin.math.cos(rad).toFloat()
        /**
         * sinR val.
         */
        val sinR = kotlin.math.sin(rad).toFloat()
        /**
         * dx val.
         */
        val dx = x - cx; val dy = y - cy
        x = cx + dx * cosR - dy * sinR
        y = cy + dx * sinR + dy * cosR
    }
    return Offset(x, y)
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun getRobotCoordFromScreen(
    screenOffset: Offset, 
    w: Float, 
    h: Float, 
    fieldWidthM: Double, 
    fieldHeightM: Double, 
    league: League, 
    zoomScale: Float, 
    panOffset: Offset,
    viewRotationDeg: Float = 0f
): Waypoint {
    /**
     * sx var.
     */
    var sx = screenOffset.x; var sy = screenOffset.y
    // Reverse view rotation around canvas center
    if (viewRotationDeg != 0f) {
        /**
         * cx val.
         */
        val cx = w / 2f; val cy = h / 2f
        /**
         * rad val.
         */
        val rad = Math.toRadians(-viewRotationDeg.toDouble()) // negative to reverse
        /**
         * cosR val.
         */
        val cosR = kotlin.math.cos(rad).toFloat()
        /**
         * sinR val.
         */
        val sinR = kotlin.math.sin(rad).toFloat()
        /**
         * dx val.
         */
        val dx = sx - cx; val dy = sy - cy
        sx = cx + dx * cosR - dy * sinR
        sy = cy + dx * sinR + dy * cosR
    }
    // Reverse pan & zoom
    /**
     * baseOffset val.
     */
    val baseOffset = Offset(
        x = (sx - panOffset.x) / zoomScale,
        y = (sy - panOffset.y) / zoomScale
    )
    return getRobotCoordBase(baseOffset, w, h, fieldWidthM, fieldHeightM, league)
}

/**
 * Reverse-transforms screen pointer coordinates to base (untransformed) canvas coordinates.
 * This allows hit-testing using getCanvasOffsetBase positions directly.
 */
fun getBaseCanvasFromScreen(
    screenOffset: Offset,
    w: Float,
    h: Float,
    zoomScale: Float,
    panOffset: Offset,
    viewRotationDeg: Float = 0f
): Offset {
    /**
     * sx var.
     */
    var sx = screenOffset.x; var sy = screenOffset.y
    if (viewRotationDeg != 0f) {
        /**
         * cx val.
         */
        val cx = w / 2f; val cy = h / 2f
        /**
         * rad val.
         */
        val rad = Math.toRadians(-viewRotationDeg.toDouble())
        /**
         * cosR val.
         */
        val cosR = kotlin.math.cos(rad).toFloat()
        /**
         * sinR val.
         */
        val sinR = kotlin.math.sin(rad).toFloat()
        /**
         * dx val.
         */
        val dx = sx - cx; val dy = sy - cy
        sx = cx + dx * cosR - dy * sinR
        sy = cy + dx * sinR + dy * cosR
    }
    return Offset(
        x = (sx - panOffset.x) / zoomScale,
        y = (sy - panOffset.y) / zoomScale
    )
}

/**
 * Converts a pixel-space drag delta directly to a field-coordinate delta.
 * Uses the `dragAmount` vector from Compose's `detectDragGestures` to avoid
 * absolute coordinate conversion issues (DPI scaling, rotate modifier, etc.).
 *
 * For FTC:
 *   canvasX = (-fieldY / fieldW + 0.5) * canvasW  ⇒  deltaFieldY = -deltaCanvasX / canvasW * fieldW
 *   canvasY = (-fieldX / fieldH + 0.5) * canvasH  ⇒  deltaFieldX = -deltaCanvasY / canvasH * fieldH
 */
fun getDragDeltaInFieldCoords(
    dragAmount: Offset,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League,
    zoomScale: Float,
    viewRotationDeg: Float = 0f
): Waypoint {
    /**
     * dxPx var.
     */
    var dxPx = dragAmount.x
    /**
     * dyPx var.
     */
    var dyPx = dragAmount.y
    if (viewRotationDeg != 0f) {
        /**
         * rad val.
         */
        val rad = Math.toRadians(-viewRotationDeg.toDouble())
        /**
         * cosR val.
         */
        val cosR = kotlin.math.cos(rad).toFloat()
        /**
         * sinR val.
         */
        val sinR = kotlin.math.sin(rad).toFloat()
        /**
         * rotatedX val.
         */
        val rotatedX = dxPx * cosR - dyPx * sinR
        /**
         * rotatedY val.
         */
        val rotatedY = dxPx * sinR + dyPx * cosR
        dxPx = rotatedX
        dyPx = rotatedY
    }
    
    return if (league == League.FTC) {
        /**
         * dx val.
         */
        val dx = -(dyPx / canvasH) * fieldH / zoomScale
        /**
         * dy val.
         */
        val dy = -(dxPx / canvasW) * fieldW / zoomScale
        Waypoint(dx, dy)
    } else {
        /**
         * dx val.
         */
        val dx = (dxPx / canvasW) * fieldW / zoomScale
        /**
         * dy val.
         */
        val dy = -(dyPx / canvasH) * fieldH / zoomScale
        Waypoint(dx, dy)
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
fun cubicHermite(p0: Double, v0: Double, p1: Double, v1: Double, t: Double): Double {
    /**
     * t2 val.
     */
    val t2 = t * t
    /**
     * t3 val.
     */
    val t3 = t2 * t
    /**
     * h00 val.
     */
    val h00 = 2 * t3 - 3 * t2 + 1
    /**
     * h10 val.
     */
    val h10 = t3 - 2 * t2 + t
    /**
     * h01 val.
     */
    val h01 = -2 * t3 + 3 * t2
    /**
     * h11 val.
     */
    val h11 = t3 - t2
    return h00 * p0 + h10 * v0 + h01 * p1 + h11 * v1
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun getPositionOnSpline(pos: Double, waypoints: List<Waypoint>): Waypoint {
    if (waypoints.isEmpty()) return Waypoint(0.0, 0.0)
    if (waypoints.size == 1) return waypoints.first()
    /**
     * maxIdx val.
     */
    val maxIdx = waypoints.size - 1
    /**
     * i val.
     */
    val i = pos.toInt().coerceIn(0, maxIdx - 1)
    /**
     * t val.
     */
    val t = (pos - i).coerceIn(0.0, 1.0)
    
    /**
     * p0 val.
     */
    val p0 = waypoints[i]
    /**
     * p1 val.
     */
    val p1 = waypoints[i + 1]
    
    /**
     * h0 val.
     */
    val h0 = resolveHeading(waypoints, i)
    /**
     * h1 val.
     */
    val h1 = resolveHeading(waypoints, i + 1)
    /**
     * v0x val.
     */
    val v0x = kotlin.math.cos(h0) * p0.nextControlLength
    /**
     * v0y val.
     */
    val v0y = kotlin.math.sin(h0) * p0.nextControlLength
    /**
     * v1x val.
     */
    val v1x = kotlin.math.cos(h1) * p1.prevControlLength
    /**
     * v1y val.
     */
    val v1y = kotlin.math.sin(h1) * p1.prevControlLength
    
    /**
     * px val.
     */
    val px = cubicHermite(p0.x, v0x, p1.x, v1x, t)
    /**
     * py val.
     */
    val py = cubicHermite(p0.y, v0y, p1.y, v1y, t)
    return Waypoint(px, py)
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun getClosestSplinePosition(
    mouseOffset: Offset,
    waypoints: List<Waypoint>,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Double {
    if (waypoints.size < 2) return 0.0
    /**
     * bestPos var.
     */
    var bestPos = 0.0
    /**
     * minDist var.
     */
    var minDist = Float.MAX_VALUE
    /**
     * maxPos val.
     */
    val maxPos = (waypoints.size - 1).toDouble()
    
    // Sample the spline densely to find closest projection
    /**
     * steps val.
     */
    val steps = (waypoints.size - 1) * 40
    for (k in 0..steps) {
        /**
         * pos val.
         */
        val pos = maxPos * k / steps
        /**
         * wp val.
         */
        val wp = getPositionOnSpline(pos, waypoints)
        /**
         * offset val.
         */
        val offset = getCanvasOffsetBase(wp, canvasW, canvasH, fieldW, fieldH, league)
        /**
         * dx val.
         */
        val dx = mouseOffset.x - offset.x
        /**
         * dy val.
         */
        val dy = mouseOffset.y - offset.y
        /**
         * dist val.
         */
        val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist < minDist) {
            minDist = dist
            bestPos = pos
        }
    }
    return bestPos
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun DrawScope.drawCoordinateAxes(
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League,
    textMeasurer: TextMeasurer
) {
    /**
     * originOffset val.
     */
    val originOffset = getCanvasOffsetBase(Waypoint(0.0, 0.0), canvasW, canvasH, fieldW, fieldH, league)
    /**
     * xAxisOffset val.
     */
    val xAxisOffset = getCanvasOffsetBase(Waypoint(0.8, 0.0), canvasW, canvasH, fieldW, fieldH, league)
    /**
     * yAxisOffset val.
     */
    val yAxisOffset = getCanvasOffsetBase(Waypoint(0.0, 0.8), canvasW, canvasH, fieldW, fieldH, league)

    /**
     * strokeW val.
     */
    val strokeW = 3.dp.toPx()
    /**
     * arrowSize val.
     */
    val arrowSize = 10.dp.toPx()

    // X Axis (Red)
    drawLine(color = AresRed, start = originOffset, end = xAxisOffset, strokeWidth = strokeW)
    // X Axis Arrow Head
    /**
     * xAngle val.
     */
    val xAngle = kotlin.math.atan2((xAxisOffset.y - originOffset.y).toDouble(), (xAxisOffset.x - originOffset.x).toDouble())
    /**
     * xArrowPath val.
     */
    val xArrowPath = Path().apply {
        moveTo(xAxisOffset.x, xAxisOffset.y)
        lineTo(
            xAxisOffset.x - arrowSize * kotlin.math.cos(xAngle - Math.PI / 6).toFloat(),
            xAxisOffset.y - arrowSize * kotlin.math.sin(xAngle - Math.PI / 6).toFloat()
        )
        lineTo(
            xAxisOffset.x - arrowSize * kotlin.math.cos(xAngle + Math.PI / 6).toFloat(),
            xAxisOffset.y - arrowSize * kotlin.math.sin(xAngle + Math.PI / 6).toFloat()
        )
        close()
    }
    drawPath(path = xArrowPath, color = AresRed)

    // Y Axis (Green)
    drawLine(color = AresGreen, start = originOffset, end = yAxisOffset, strokeWidth = strokeW)
    // Y Axis Arrow Head
    /**
     * yAngle val.
     */
    val yAngle = kotlin.math.atan2((yAxisOffset.y - originOffset.y).toDouble(), (yAxisOffset.x - originOffset.x).toDouble())
    /**
     * yArrowPath val.
     */
    val yArrowPath = Path().apply {
        moveTo(yAxisOffset.x, yAxisOffset.y)
        lineTo(
            yAxisOffset.x - arrowSize * kotlin.math.cos(yAngle - Math.PI / 6).toFloat(),
            yAxisOffset.y - arrowSize * kotlin.math.sin(yAngle - Math.PI / 6).toFloat()
        )
        lineTo(
            yAxisOffset.x - arrowSize * kotlin.math.cos(yAngle + Math.PI / 6).toFloat(),
            yAxisOffset.y - arrowSize * kotlin.math.sin(yAngle + Math.PI / 6).toFloat()
        )
        close()
    }
    drawPath(path = yArrowPath, color = AresGreen)

    // Axis Labels
    /**
     * textStyle val.
     */
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    /**
     * textLayoutX val.
     */
    val textLayoutX = textMeasurer.measure("X", textStyle)
    /**
     * textLayoutY val.
     */
    val textLayoutY = textMeasurer.measure("Y", textStyle)

    drawText(
        textMeasurer = textMeasurer,
        text = "X",
        style = textStyle,
        topLeft = Offset(
            xAxisOffset.x + arrowSize * kotlin.math.cos(xAngle).toFloat(),
            xAxisOffset.y + arrowSize * kotlin.math.sin(xAngle).toFloat() - textLayoutX.size.height / 2f
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "Y",
        style = textStyle,
        topLeft = Offset(
            yAxisOffset.x + arrowSize * kotlin.math.cos(yAngle).toFloat() - textLayoutY.size.width / 2f,
            yAxisOffset.y + arrowSize * kotlin.math.sin(yAngle).toFloat()
        )
    )
}
