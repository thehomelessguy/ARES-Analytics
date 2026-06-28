package com.ares.analytics.service

import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ejml.simple.SimpleMatrix
import kotlin.math.*

data class Pose3d(
    val x: Double, // Left-Right (meters)
    val y: Double, // Up-Down (meters)
    val z: Double, // Depth (meters)
    val roll: Double,
    val pitch: Double,
    val yaw: Double // Heading (radians, CCW-positive)
)

data class CalibrationDiagnostics(
    val pose: Pose3d,
    val standardErrors: DoubleArray,
    val covarianceMatrix: Array<DoubleArray>,
    val reducedChiSquared: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CalibrationDiagnostics
        if (pose != other.pose) return false
        if (!standardErrors.contentEquals(other.standardErrors)) return false
        if (!covarianceMatrix.contentDeepEquals(other.covarianceMatrix)) return false
        if (reducedChiSquared != other.reducedChiSquared) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pose.hashCode()
        result = 31 * result + standardErrors.contentHashCode()
        result = 31 * result + covarianceMatrix.contentDeepHashCode()
        result = 31 * result + reducedChiSquared.hashCode()
        return result
    }
}

data class CalibrationMeasurement(
    val gyroHeading: Double, // radians (CCW-positive)
    val tagId: Int,
    val tagFieldX: Double,
    val tagFieldY: Double,
    val tagFieldZ: Double,
    // Tag relative target space measurements from Limelight
    val targetSpaceX: Double,
    val targetSpaceY: Double,
    val targetSpaceZ: Double,
    val targetSpaceRoll: Double,
    val targetSpacePitch: Double,
    val targetSpaceYaw: Double
)

class CalibrationService(private val databaseService: DatabaseService) {

    /**
     * Solves for the 6-DOF camera extrinsic calibration offset using a Levenberg-Marquardt approach
     */
    fun solveCameraExtrinsics(measurements: List<CalibrationMeasurement>): Pose3d {
        if (measurements.isEmpty()) {
            return Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // 1. Initial Guess (based on simple averages)
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        var sumRoll = 0.0
        var sumPitch = 0.0
        var sumYaw = 0.0

        for (m in measurements) {
            val cosG = cos(m.gyroHeading)
            val sinG = sin(m.gyroHeading)
            sumX += m.targetSpaceZ * cosG - m.targetSpaceX * sinG
            sumY += m.targetSpaceZ * sinG + m.targetSpaceX * cosG
            sumZ += m.targetSpaceY
            sumRoll += m.targetSpaceRoll
            sumPitch += m.targetSpacePitch
            sumYaw += -m.targetSpaceYaw
        }

        val count = measurements.size.toDouble()
        val initDx = sumX / count
        val initDy = sumY / count
        val initDz = sumZ / count
        val initRoll = sumRoll / count
        val initPitch = sumPitch / count
        val initYaw = sumYaw / count

        // Initialize constant offsets based on initial guess
        var sumCx = 0.0
        var sumCy = 0.0
        var sumCz = 0.0
        var sumCroll = 0.0
        var sumCpitch = 0.0

        for (m in measurements) {
            val cosG = cos(m.gyroHeading)
            val sinG = sin(m.gyroHeading)
            val zPrime = m.targetSpaceZ
            val xPrime = m.targetSpaceX
            val yPrime = m.targetSpaceY
            
            sumCx += (zPrime + initDx) * cosG - (xPrime + initDy) * sinG
            sumCy += (zPrime + initDx) * sinG + (xPrime + initDy) * cosG
            sumCz += yPrime + initDz
            sumCroll += m.targetSpaceRoll - initRoll
            sumCpitch += m.targetSpacePitch - initPitch
        }
        
        val initCx = sumCx / count
        val initCy = sumCy / count
        val initCz = sumCz / count
        val initCroll = sumCroll / count
        val initCpitch = sumCpitch / count
        
        var sumCyawVal = 0.0
        for (m in measurements) {
            sumCyawVal += -m.targetSpaceYaw - (m.gyroHeading + initYaw)
        }
        val initCyaw = sumCyawVal / count

        // Parameters vector: [dx, dy, dz, roll, pitch, yaw, Cx, Cy, Cz, Croll, Cpitch, Cyaw]
        val p = doubleArrayOf(
            initDx, initDy, initDz,
            initRoll, initPitch, initYaw,
            initCx, initCy, initCz,
            initCroll, initCpitch, initCyaw
        )

        val numParams = p.size
        val numResiduals = measurements.size * 6

        var lambda = 0.001
        var cost = computeCost(p, measurements)

        val maxIterations = 200
        val tolerance = 1e-8

        for (iter in 0 until maxIterations) {
            val J = SimpleMatrix(numResiduals, numParams)
            val r = SimpleMatrix(numResiduals, 1)

            val rCurrent = computeResiduals(p, measurements)
            for (i in 0 until numResiduals) {
                r.set(i, 0, rCurrent[i])
            }

            val epsilon = 1e-6
            for (j in 0 until numParams) {
                val pPerturbed = p.clone()
                pPerturbed[j] += epsilon
                val rPerturbed = computeResiduals(pPerturbed, measurements)
                for (i in 0 until numResiduals) {
                    J.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
                }
            }

            val Jt = J.transpose()
            val JtJ = Jt.mult(J)
            val Jtr = Jt.mult(r)

            val identity = SimpleMatrix.identity(numParams)
            val lhs = JtJ.plus(identity.scale(lambda))
            
            val delta: SimpleMatrix
            try {
                delta = lhs.solve(Jtr.scale(-1.0))
            } catch (e: Exception) {
                lambda *= 10.0
                continue
            }

            val pNext = DoubleArray(numParams)
            for (j in 0 until numParams) {
                pNext[j] = p[j] + delta.get(j, 0)
            }

            val nextCost = computeCost(pNext, measurements)
            if (nextCost < cost) {
                val costDiff = cost - nextCost
                cost = nextCost
                System.arraycopy(pNext, 0, p, 0, numParams)
                lambda /= 10.0

                if (costDiff < tolerance) {
                    break
                }
            } else {
                lambda *= 10.0
            }
        }

        return Pose3d(
            x = p[0],
            y = p[1],
            z = p[2],
            roll = p[3],
            pitch = p[4],
            yaw = p[5]
        )
    }

    private fun getRotationMatrix(roll: Double, pitch: Double, yaw: Double): SimpleMatrix {
        val cr = cos(roll)
        val sr = sin(roll)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val cy = cos(yaw)
        val sy = sin(yaw)

        val Rx = SimpleMatrix(arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, cr, -sr),
            doubleArrayOf(0.0, sr, cr)
        ))

        val Ry = SimpleMatrix(arrayOf(
            doubleArrayOf(cy, 0.0, sy),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(-sy, 0.0, cy)
        ))

        val Rz = SimpleMatrix(arrayOf(
            doubleArrayOf(cp, -sp, 0.0),
            doubleArrayOf(sp, cp, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        ))

        return Ry.mult(Rz).mult(Rx)
    }

    private fun computeResiduals(p: DoubleArray, measurements: List<CalibrationMeasurement>): DoubleArray {
        val dx = p[0]
        val dy = p[1]
        val dz = p[2]
        val roll = p[3]
        val pitch = p[4]
        val yaw = p[5]
        val Cx = p[6]
        val Cy = p[7]
        val Cz = p[8]
        val Croll = p[9]
        val Cpitch = p[10]
        val Cyaw = p[11]

        val R_cam = getRotationMatrix(roll, pitch, yaw)
        val residuals = DoubleArray(measurements.size * 6)

        for (i in measurements.indices) {
            val m = measurements[i]
            val cosG = cos(m.gyroHeading)
            val sinG = sin(m.gyroHeading)

            val pMeas = SimpleMatrix(arrayOf(
                doubleArrayOf(m.targetSpaceX),
                doubleArrayOf(m.targetSpaceY),
                doubleArrayOf(m.targetSpaceZ)
            ))

            val pRotated = R_cam.mult(pMeas)
            val xRot = pRotated.get(0, 0)
            val yRot = pRotated.get(1, 0)
            val zRot = pRotated.get(2, 0)

            residuals[i * 6 + 0] = (zRot + dx) * cosG - (xRot + dy) * sinG - Cx
            residuals[i * 6 + 1] = (zRot + dx) * sinG + (xRot + dy) * cosG - Cy
            residuals[i * 6 + 2] = yRot + dz - Cz

            residuals[i * 6 + 3] = m.targetSpaceRoll - roll - Croll
            residuals[i * 6 + 4] = m.targetSpacePitch - pitch - Cpitch
            residuals[i * 6 + 5] = -m.targetSpaceYaw - (m.gyroHeading + yaw) - Cyaw
        }

        return residuals
    }

    private fun computeCost(p: DoubleArray, measurements: List<CalibrationMeasurement>): Double {
        val r = computeResiduals(p, measurements)
        var sum = 0.0
        for (v in r) {
            sum += v * v
        }
        return sum
    }

    /**
     * Scans NT4 telemetry from a calibration run to pull measurements and run the solver.
     */
    suspend fun runExtrinsicCalibration(
        sessionId: String,
        cameraIndex: Int
    ): Pose3d = withContext(Dispatchers.Default) {
        val gyroFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/GyroHeading" }
        val tagIdFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/TagIndex" }
        val camToTagFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key.startsWith("/Calibration/CameraToTag[$cameraIndex]") }

        if (gyroFrames.isEmpty() || tagIdFrames.isEmpty() || camToTagFrames.isEmpty()) {
            return@withContext Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // Standard AprilTag positions on the field (simplified mapping for FTC field)
        val tagMap = mapOf(
            1 to FieldTag(1.8, 0.0, 0.12),
            2 to FieldTag(1.8, 0.6, 0.12),
            3 to FieldTag(1.8, -0.6, 0.12),
            4 to FieldTag(-1.8, 0.0, 0.12)
        )

        val measurements = mutableListOf<CalibrationMeasurement>()
        val timeMap = gyroFrames.associateBy { it.timestampMs }

        for (tagFrame in tagIdFrames) {
            val t = tagFrame.timestampMs
            val gyro = timeMap[t]?.value ?: continue
            val tagId = tagFrame.value.toInt()
            val tagField = tagMap[tagId] ?: continue

            // Fetch cam translation/rotation from array keys: index 0=x, 1=y, 2=z, 3=roll, 4=pitch, 5=yaw
            val x = getVal(camToTagFrames, t, 0)
            val y = getVal(camToTagFrames, t, 1)
            val z = getVal(camToTagFrames, t, 2)
            val roll = getVal(camToTagFrames, t, 3)
            val pitch = getVal(camToTagFrames, t, 4)
            val yaw = getVal(camToTagFrames, t, 5)

            measurements.add(
                CalibrationMeasurement(
                    gyroHeading = gyro,
                    tagId = tagId,
                    tagFieldX = tagField.x,
                    tagFieldY = tagField.y,
                    tagFieldZ = tagField.z,
                    targetSpaceX = x,
                    targetSpaceY = y,
                    targetSpaceZ = z,
                    targetSpaceRoll = roll,
                    targetSpacePitch = pitch,
                    targetSpaceYaw = yaw
                )
            )
        }

        solveCameraExtrinsics(measurements)
    }

    fun solveCameraExtrinsicsWithDiagnostics(measurements: List<CalibrationMeasurement>): CalibrationDiagnostics {
        if (measurements.isEmpty()) {
            return CalibrationDiagnostics(
                pose = Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                standardErrors = DoubleArray(6),
                covarianceMatrix = Array(6) { DoubleArray(6) },
                reducedChiSquared = 0.0
            )
        }

        // 1. Initial Guess (based on simple averages)
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        var sumRoll = 0.0
        var sumPitch = 0.0
        var sumYaw = 0.0

        for (m in measurements) {
            val cosG = cos(m.gyroHeading)
            val sinG = sin(m.gyroHeading)
            sumX += m.targetSpaceZ * cosG - m.targetSpaceX * sinG
            sumY += m.targetSpaceZ * sinG + m.targetSpaceX * cosG
            sumZ += m.targetSpaceY
            sumRoll += m.targetSpaceRoll
            sumPitch += m.targetSpacePitch
            sumYaw += -m.targetSpaceYaw
        }

        val count = measurements.size.toDouble()
        val initDx = sumX / count
        val initDy = sumY / count
        val initDz = sumZ / count
        val initRoll = sumRoll / count
        val initPitch = sumPitch / count
        val initYaw = sumYaw / count

        // Initialize constant offsets based on initial guess
        var sumCx = 0.0
        var sumCy = 0.0
        var sumCz = 0.0
        var sumCroll = 0.0
        var sumCpitch = 0.0

        for (m in measurements) {
            val cosG = cos(m.gyroHeading)
            val sinG = sin(m.gyroHeading)
            val zPrime = m.targetSpaceZ
            val xPrime = m.targetSpaceX
            val yPrime = m.targetSpaceY
            
            sumCx += (zPrime + initDx) * cosG - (xPrime + initDy) * sinG
            sumCy += (zPrime + initDx) * sinG + (xPrime + initDy) * cosG
            sumCz += yPrime + initDz
            sumCroll += m.targetSpaceRoll - initRoll
            sumCpitch += m.targetSpacePitch - initPitch
        }
        
        val initCx = sumCx / count
        val initCy = sumCy / count
        val initCz = sumCz / count
        val initCroll = sumCroll / count
        val initCpitch = sumCpitch / count
        
        var sumCyawVal = 0.0
        for (m in measurements) {
            sumCyawVal += -m.targetSpaceYaw - (m.gyroHeading + initYaw)
        }
        val initCyaw = sumCyawVal / count

        // Parameters vector: [dx, dy, dz, roll, pitch, yaw, Cx, Cy, Cz, Croll, Cpitch, Cyaw]
        val p = doubleArrayOf(
            initDx, initDy, initDz,
            initRoll, initPitch, initYaw,
            initCx, initCy, initCz,
            initCroll, initCpitch, initCyaw
        )

        val numParams = p.size
        val numResiduals = measurements.size * 6

        var lambda = 0.001
        var cost = computeCost(p, measurements)

        val maxIterations = 200
        val tolerance = 1e-8

        for (iter in 0 until maxIterations) {
            val J = SimpleMatrix(numResiduals, numParams)
            val r = SimpleMatrix(numResiduals, 1)

            val rCurrent = computeResiduals(p, measurements)
            for (i in 0 until numResiduals) {
                r.set(i, 0, rCurrent[i])
            }

            val epsilon = 1e-6
            for (j in 0 until numParams) {
                val pPerturbed = p.clone()
                pPerturbed[j] += epsilon
                val rPerturbed = computeResiduals(pPerturbed, measurements)
                for (i in 0 until numResiduals) {
                    J.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
                }
            }

            val Jt = J.transpose()
            val JtJ = Jt.mult(J)
            val Jtr = Jt.mult(r)

            val identity = SimpleMatrix.identity(numParams)
            val lhs = JtJ.plus(identity.scale(lambda))
            
            val delta: SimpleMatrix
            try {
                delta = lhs.solve(Jtr.scale(-1.0))
            } catch (e: Exception) {
                lambda *= 10.0
                continue
            }

            val pNext = DoubleArray(numParams)
            for (j in 0 until numParams) {
                pNext[j] = p[j] + delta.get(j, 0)
            }

            val nextCost = computeCost(pNext, measurements)
            if (nextCost < cost) {
                val costDiff = cost - nextCost
                cost = nextCost
                System.arraycopy(pNext, 0, p, 0, numParams)
                lambda /= 10.0

                if (costDiff < tolerance) {
                    break
                }
            } else {
                lambda *= 10.0
            }
        }

        // --- Compute Diagnostics ---
        val finalJ = SimpleMatrix(numResiduals, numParams)
        val rCurrent = computeResiduals(p, measurements)
        val epsilon = 1e-6
        for (j in 0 until numParams) {
            val pPerturbed = p.clone()
            pPerturbed[j] += epsilon
            val rPerturbed = computeResiduals(pPerturbed, measurements)
            for (i in 0 until numResiduals) {
                finalJ.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
            }
        }

        val Jt = finalJ.transpose()
        val JtJ = Jt.mult(finalJ)

        val covMatrix = try {
            JtJ.invert()
        } catch (e: Exception) {
            val regularizedJtJ = JtJ.plus(SimpleMatrix.identity(numParams).scale(1e-6))
            try {
                regularizedJtJ.invert()
            } catch (e2: Exception) {
                SimpleMatrix(numParams, numParams)
            }
        }

        val sumSquaredResiduals = computeCost(p, measurements)
        val degreesOfFreedom = (numResiduals - numParams).coerceAtLeast(1)
        val chiSquaredReduced = sumSquaredResiduals / degreesOfFreedom

        val standardErrors = DoubleArray(6)
        for (j in 0 until 6) {
            val variance = covMatrix.get(j, j)
            standardErrors[j] = sqrt(max(0.0, variance) * chiSquaredReduced)
        }

        val covariance6x6 = Array(6) { r ->
            DoubleArray(6) { c ->
                covMatrix.get(r, c) * chiSquaredReduced
            }
        }

        val solvedPose = Pose3d(
            x = p[0],
            y = p[1],
            z = p[2],
            roll = p[3],
            pitch = p[4],
            yaw = p[5]
        )

        return CalibrationDiagnostics(
            pose = solvedPose,
            standardErrors = standardErrors,
            covarianceMatrix = covariance6x6,
            reducedChiSquared = chiSquaredReduced
        )
    }

    suspend fun runExtrinsicCalibrationWithDiagnostics(
        sessionId: String,
        cameraIndex: Int
    ): CalibrationDiagnostics = withContext(Dispatchers.Default) {
        val gyroFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/GyroHeading" }
        val tagIdFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/TagIndex" }
        val camToTagFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key.startsWith("/Calibration/CameraToTag[$cameraIndex]") }

        if (gyroFrames.isEmpty() || tagIdFrames.isEmpty() || camToTagFrames.isEmpty()) {
            return@withContext CalibrationDiagnostics(
                pose = Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                standardErrors = DoubleArray(6),
                covarianceMatrix = Array(6) { DoubleArray(6) },
                reducedChiSquared = 0.0
            )
        }

        val tagMap = mapOf(
            1 to FieldTag(1.8, 0.0, 0.12),
            2 to FieldTag(1.8, 0.6, 0.12),
            3 to FieldTag(1.8, -0.6, 0.12),
            4 to FieldTag(-1.8, 0.0, 0.12)
        )

        val measurements = mutableListOf<CalibrationMeasurement>()
        val timeMap = gyroFrames.associateBy { it.timestampMs }

        for (tagFrame in tagIdFrames) {
            val t = tagFrame.timestampMs
            val gyro = timeMap[t]?.value ?: continue
            val tagId = tagFrame.value.toInt()
            val tagField = tagMap[tagId] ?: continue

            val x = getVal(camToTagFrames, t, 0)
            val y = getVal(camToTagFrames, t, 1)
            val z = getVal(camToTagFrames, t, 2)
            val roll = getVal(camToTagFrames, t, 3)
            val pitch = getVal(camToTagFrames, t, 4)
            val yaw = getVal(camToTagFrames, t, 5)

            measurements.add(
                CalibrationMeasurement(
                    gyroHeading = gyro,
                    tagId = tagId,
                    tagFieldX = tagField.x,
                    tagFieldY = tagField.y,
                    tagFieldZ = tagField.z,
                    targetSpaceX = x,
                    targetSpaceY = y,
                    targetSpaceZ = z,
                    targetSpaceRoll = roll,
                    targetSpacePitch = pitch,
                    targetSpaceYaw = yaw
                )
            )
        }

        solveCameraExtrinsicsWithDiagnostics(measurements)
    }

    private fun getVal(frames: List<TelemetryFrame>, timestampMs: Long, index: Int): Double {
        val indexKeySuffix = "[$index]"
        val matchingKeys = frames.filter { it.key.endsWith(indexKeySuffix) }
        val targetList = if (matchingKeys.isNotEmpty()) matchingKeys else frames
        return targetList.minByOrNull { abs(it.timestampMs - (timestampMs + index)) }?.value ?: 0.0
    }
}

data class FieldTag(val x: Double, val y: Double, val z: Double)
