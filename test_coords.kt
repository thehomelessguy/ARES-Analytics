import kotlin.math.*

enum class League { FTC, FRC }

class Waypoint(val x: Double, val y: Double)
class Offset(val x: Float, val y: Float)

fun getCanvasOffsetBase(
    wp: Waypoint,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Offset {
    return if (league == League.FTC) {
        val x = (-wp.y / fieldW + 0.5) * canvasW
        val y = (-wp.x / fieldH + 0.5) * canvasH
        Offset(x.toFloat(), y.toFloat())
    } else {
        val x = (wp.x / fieldW) * canvasW
        val y = ((fieldH - wp.y) / fieldH) * canvasH
        Offset(x.toFloat(), y.toFloat())
    }
}

fun getRobotCoordBase(
    offset: Offset,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Waypoint {
    return if (league == League.FTC) {
        val rx = -(offset.y / canvasH - 0.5) * fieldH
        val ry = -(offset.x / canvasW - 0.5) * fieldW
        Waypoint(rx, ry)
    } else {
        val rx = (offset.x / canvasW) * fieldW
        val ry = fieldH - (offset.y / canvasH) * fieldH
        Waypoint(rx, ry)
    }
}

fun main() {
    val canvasW = 1584f
    val canvasH = 1030f
    val fieldW = 3.65
    val fieldH = 3.65
    val league = League.FTC
    
    val wp = Waypoint(1.0, 1.0)
    val offset = getCanvasOffsetBase(wp, canvasW, canvasH, fieldW, fieldH, league)
    val back = getRobotCoordBase(offset, canvasW, canvasH, fieldW, fieldH, league)
    
    println("Original WP: ${wp.x}, ${wp.y}")
    println("Offset: ${offset.x}, ${offset.y}")
    println("Back to WP: ${back.x}, ${back.y}")
    
    // Test drag
    val dx_mouse = 100f
    val offset2 = Offset(offset.x + dx_mouse, offset.y)
    val back2 = getRobotCoordBase(offset2, canvasW, canvasH, fieldW, fieldH, league)
    println("Drag X by 100px:")
    println("New WP: ${back2.x}, ${back2.y}")
    println("Field delta: ${back2.x - back.x}, ${back2.y - back.y}")
    
    val offset3 = getCanvasOffsetBase(back2, canvasW, canvasH, fieldW, fieldH, league)
    println("Redrawn offset: ${offset3.x}, ${offset3.y}")
    println("Canvas delta: ${offset3.x - offset.x}, ${offset3.y - offset.y}")
}
