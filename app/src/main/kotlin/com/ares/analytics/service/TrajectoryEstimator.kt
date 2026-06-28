package com.ares.analytics.service

import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.shared.PathConstraints
import com.ares.analytics.shared.ConstraintsZone
import com.ares.analytics.shared.GoalEndState
import com.ares.analytics.shared.IdealStartingState
import kotlin.math.*

object TrajectoryEstimator {

    private data class SampledPoint(
        val x: Double,
        val y: Double,
        val s: Double,
        val relativePos: Double,
        var maxV: Double = 0.0,
        var v: Double = 0.0
    )

    fun estimateDuration(
        waypoints: List<Waypoint>,
        globalConstraints: PathConstraints,
        constraintZones: List<ConstraintsZone>,
        idealStartingState: IdealStartingState,
        goalEndState: GoalEndState
    ): Double {
        if (waypoints.size < 2) return 0.0

        val sampledPoints = mutableListOf<SampledPoint>()
        // Start with the first waypoint
        sampledPoints.add(SampledPoint(waypoints[0].x, waypoints[0].y, 0.0, 0.0))

        val maxIdx = waypoints.size - 1
        var totalDist = 0.0

        // Step 1: Sample points along Catmull-Rom spline
        for (i in 0 until maxIdx) {
            val p0 = waypoints[maxOf(0, i - 1)]
            val p1 = waypoints[i]
            val p2 = waypoints[i + 1]
            val p3 = waypoints[minOf(maxIdx, i + 2)]

            val steps = 100
            for (j in 1..steps) {
                val t = j.toDouble() / steps
                val px = catmullRom(p0.x, p1.x, p2.x, p3.x, t)
                val py = catmullRom(p0.y, p1.y, p2.y, p3.y, t)
                val prev = sampledPoints.last()
                val ds = sqrt((px - prev.x).pow(2) + (py - prev.y).pow(2))
                if (ds >= 0.05 || (i == maxIdx - 1 && j == steps)) {
                    totalDist += ds
                    val relativePos = i.toDouble() + t
                    sampledPoints.add(SampledPoint(px, py, totalDist, relativePos))
                }
            }
        }

        // Step 2: Calculate max velocity limits at each point based on local constraints and curvature
        for (idx in sampledPoints.indices) {
            val pt = sampledPoints[idx]
            val constraints = getConstraintsAt(pt.relativePos, globalConstraints, constraintZones)

            val maxAcc = constraints.maxAcceleration
            val maxVel = constraints.maxVelocity

            // Curvature radius calculation
            val r = if (idx > 0 && idx < sampledPoints.size - 1) {
                val prev = sampledPoints[idx - 1]
                val curr = sampledPoints[idx]
                val next = sampledPoints[idx + 1]
                calculateRadius(prev.x, prev.y, curr.x, curr.y, next.x, next.y)
            } else {
                Double.POSITIVE_INFINITY
            }

            val centripetalV = if (r.isInfinite() || r.isNaN()) {
                maxVel
            } else {
                sqrt(maxAcc * r)
            }

            sampledPoints[idx].maxV = minOf(maxVel, centripetalV)
        }

        // Apply start/end velocity limits
        sampledPoints[0].maxV = minOf(sampledPoints[0].maxV, idealStartingState.velocity)
        sampledPoints[sampledPoints.size - 1].maxV = minOf(sampledPoints[sampledPoints.size - 1].maxV, goalEndState.velocity)

        // Step 3: Forward Pass (acceleration profile)
        sampledPoints[0].v = sampledPoints[0].maxV
        for (i in 1 until sampledPoints.size) {
            val ds = sampledPoints[i].s - sampledPoints[i - 1].s
            val constraints = getConstraintsAt(sampledPoints[i].relativePos, globalConstraints, constraintZones)
            val maxAcc = constraints.maxAcceleration
            val maxReachable = sqrt(sampledPoints[i - 1].v.pow(2) + 2.0 * maxAcc * ds)
            sampledPoints[i].v = minOf(sampledPoints[i].maxV, maxReachable)
        }

        // Step 4: Backward Pass (deceleration profile)
        sampledPoints[sampledPoints.size - 1].v = sampledPoints[sampledPoints.size - 1].maxV
        for (i in (sampledPoints.size - 2) downTo 0) {
            val ds = sampledPoints[i + 1].s - sampledPoints[i].s
            val constraints = getConstraintsAt(sampledPoints[i].relativePos, globalConstraints, constraintZones)
            val maxAcc = constraints.maxAcceleration
            val maxReachable = sqrt(sampledPoints[i + 1].v.pow(2) + 2.0 * maxAcc * ds)
            sampledPoints[i].v = minOf(sampledPoints[i].v, maxReachable)
        }

        // Step 5: Integrate time to compute estimated duration
        var duration = 0.0
        for (i in 1 until sampledPoints.size) {
            val ds = sampledPoints[i].s - sampledPoints[i - 1].s
            val avgV = (sampledPoints[i].v + sampledPoints[i - 1].v) / 2.0
            duration += if (avgV > 1e-4) ds / avgV else 0.0
        }

        return duration
    }

    private fun catmullRom(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
        return 0.5 * (
            (2.0 * p1) +
            (-p0 + p2) * t +
            (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t * t +
            (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t * t * t
        )
    }

    private fun calculateRadius(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double {
        val ab = sqrt((ax - bx).pow(2) + (ay - by).pow(2))
        val bc = sqrt((bx - cx).pow(2) + (by - cy).pow(2))
        val ca = sqrt((cx - ax).pow(2) + (cy - ay).pow(2))
        val area = 0.5 * abs(ax * (by - cy) + bx * (cy - ay) + cx * (ay - by))
        if (area < 1e-6) return Double.POSITIVE_INFINITY
        return (ab * bc * ca) / (4.0 * area)
    }

    private fun getConstraintsAt(
        pos: Double,
        globalConstraints: PathConstraints,
        constraintZones: List<ConstraintsZone>
    ): PathConstraints {
        for (zone in constraintZones) {
            if (pos >= zone.minWaypointRelativePos && pos <= zone.maxWaypointRelativePos) {
                return zone.constraints
            }
        }
        return globalConstraints
    }
}
