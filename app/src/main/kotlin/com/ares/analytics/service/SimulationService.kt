package com.ares.analytics.service

import java.util.Random
import kotlin.math.*

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class SimPoint(val x: Double, val y: Double)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class SimPolygon(val vertices: List<SimPoint>) {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun getEdges(): List<Pair<SimPoint, SimPoint>> {
        /**
         * edges val.
         */
        val edges = mutableListOf<Pair<SimPoint, SimPoint>>()
        for (i in vertices.indices) {
            edges.add(Pair(vertices[i], vertices[(i + 1) % vertices.size]))
        }
        return edges
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
class SimulationService {

    private val random = Random()
    private val obstacles = mutableListOf<SimPolygon>()

    init {
        // Define default FTC center trusses/obstacles (around center of 3.6m x 3.6m field)
        // Center truss pole 1
        obstacles.add(SimPolygon(listOf(
            SimPoint(-0.1, -0.1), SimPoint(0.1, -0.1), SimPoint(0.1, 0.1), SimPoint(-0.1, 0.1)
        )))
        // Backdrop wall in FTC Centerstage
        obstacles.add(SimPolygon(listOf(
            SimPoint(1.6, -0.9), SimPoint(1.8, -0.9), SimPoint(1.8, -0.5), SimPoint(1.6, -0.5)
        )))
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun addObstacle(polygon: SimPolygon) {
        obstacles.add(polygon)
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun clearObstacles() {
        obstacles.clear()
    }

    /**
     * Checks if the line of sight between the camera and the AprilTag is clear.
     */
    fun hasLineOfSight(cam: SimPoint, tag: SimPoint): Boolean {
        for (obstacle in obstacles) {
            for (edge in obstacle.getEdges()) {
                if (segmentsIntersect(cam, tag, edge.first, edge.second)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Generates a synthetic camera packet with Gaussian noise and simulated latency.
     */
    fun generateSyntheticVision(
        robotX: Double,
        robotY: Double,
        robotHeadingRad: Double,
        tagX: Double,
        tagY: Double,
        noiseStdDev: Double = 0.02 // 2 cm standard deviation
    ): SyntheticVisionResult? {
        /**
         * cam val.
         */
        val cam = SimPoint(robotX, robotY)
        /**
         * tag val.
         */
        val tag = SimPoint(tagX, tagY)

        if (!hasLineOfSight(cam, tag)) {
            return null // Tag is occluded
        }

        // Relative distance and angle
        /**
         * dx val.
         */
        val dx = tag.x - robotX
        /**
         * dy val.
         */
        val dy = tag.y - robotY
        /**
         * dist val.
         */
        val dist = sqrt(dx * dx + dy * dy)

        // Only visible if within 3.0 meters and within 60 degrees camera FOV
        if (dist > 3.0) return null

        /**
         * angleToTag val.
         */
        val angleToTag = atan2(dy, dx)
        /**
         * relativeAngle val.
         */
        val relativeAngle = normalizeAngle(angleToTag - robotHeadingRad)

        if (abs(relativeAngle) > Math.toRadians(30.0)) {
            return null // Outside camera FOV
        }

        // Add Gaussian noise
        /**
         * noisyDist val.
         */
        val noisyDist = dist + random.nextGaussian() * noiseStdDev
        /**
         * noisyAngle val.
         */
        val noisyAngle = relativeAngle + random.nextGaussian() * (noiseStdDev * 0.1)

        // Simulate 40ms capture latency
        /**
         * latencyMs val.
         */
        val latencyMs = 40L

        return SyntheticVisionResult(
            distance = noisyDist,
            relativeAngleRad = noisyAngle,
            latencyMs = latencyMs
        )
    }

    private fun segmentsIntersect(a: SimPoint, b: SimPoint, c: SimPoint, d: SimPoint): Boolean {
        /**
         * d1 val.
         */
        val d1 = direction(c, d, a)
        /**
         * d2 val.
         */
        val d2 = direction(c, d, b)
        /**
         * d3 val.
         */
        val d3 = direction(a, b, c)
        /**
         * d4 val.
         */
        val d4 = direction(a, b, d)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true
        }

        // Collinear cases
        if (d1 == 0.0 && onSegment(c, d, a)) return true
        if (d2 == 0.0 && onSegment(c, d, b)) return true
        if (d3 == 0.0 && onSegment(a, b, c)) return true
        if (d4 == 0.0 && onSegment(a, b, d)) return true

        return false
    }

    private fun direction(pi: SimPoint, pj: SimPoint, pk: SimPoint): Double {
        return (pk.x - pi.x) * (pj.y - pi.y) - (pj.x - pi.x) * (pk.y - pi.y)
    }

    private fun onSegment(pi: SimPoint, pj: SimPoint, pk: SimPoint): Boolean {
        return pk.x >= minOf(pi.x, pj.x) && pk.x <= maxOf(pi.x, pj.x) &&
               pk.y >= minOf(pi.y, pj.y) && pk.y <= maxOf(pi.y, pj.y)
    }

    private fun normalizeAngle(angle: Double): Double {
        /**
         * a var.
         */
        var a = angle
        while (a > Math.PI) a -= 2 * Math.PI
        while (a < -Math.PI) a += 2 * Math.PI
        return a
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
data class SyntheticVisionResult(
    /**
     * distance val.
     */
    val distance: Double,
    /**
     * relativeAngleRad val.
     */
    val relativeAngleRad: Double,
    /**
     * latencyMs val.
     */
    val latencyMs: Long
)
