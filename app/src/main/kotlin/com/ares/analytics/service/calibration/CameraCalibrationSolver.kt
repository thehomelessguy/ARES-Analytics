package com.ares.analytics.service.calibration

import com.ares.analytics.service.CalibrationDiagnostics
import com.ares.analytics.service.CalibrationMeasurement
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.FieldTag
import com.ares.analytics.service.Pose3d
import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ejml.simple.SimpleMatrix
import kotlin.math.*

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class CameraCalibrationSolver(private val databaseService: DatabaseService) {

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun solveCameraExtrinsics(measurements: List<CalibrationMeasurement>): Pose3d {
        if (measurements.isEmpty()) {
            return Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        /**
         * sumX var.
         */
        var sumX = 0.0
        /**
         * sumY var.
         */
        var sumY = 0.0
        /**
         * sumZ var.
         */
        var sumZ = 0.0
        /**
         * sumRoll var.
         */
        var sumRoll = 0.0
        /**
         * sumPitch var.
         */
        var sumPitch = 0.0
        /**
         * sumYaw var.
         */
        var sumYaw = 0.0

        for (m in measurements) {
            /**
             * cosG val.
             */
            val cosG = cos(m.gyroHeading)
            /**
             * sinG val.
             */
            val sinG = sin(m.gyroHeading)
            sumX += m.targetSpaceZ * cosG - m.targetSpaceX * sinG
            sumY += m.targetSpaceZ * sinG + m.targetSpaceX * cosG
            sumZ += m.targetSpaceY
            sumRoll += m.targetSpaceRoll
            sumPitch += m.targetSpacePitch
            sumYaw += -m.targetSpaceYaw
        }

        /**
         * count val.
         */
        val count = measurements.size.toDouble()
        /**
         * initDx val.
         */
        val initDx = sumX / count
        /**
         * initDy val.
         */
        val initDy = sumY / count
        /**
         * initDz val.
         */
        val initDz = sumZ / count
        /**
         * initRoll val.
         */
        val initRoll = sumRoll / count
        /**
         * initPitch val.
         */
        val initPitch = sumPitch / count
        /**
         * initYaw val.
         */
        val initYaw = sumYaw / count

        /**
         * sumCx var.
         */
        var sumCx = 0.0
        /**
         * sumCy var.
         */
        var sumCy = 0.0
        /**
         * sumCz var.
         */
        var sumCz = 0.0
        /**
         * sumCroll var.
         */
        var sumCroll = 0.0
        /**
         * sumCpitch var.
         */
        var sumCpitch = 0.0

        for (m in measurements) {
            /**
             * cosG val.
             */
            val cosG = cos(m.gyroHeading)
            /**
             * sinG val.
             */
            val sinG = sin(m.gyroHeading)
            /**
             * zPrime val.
             */
            val zPrime = m.targetSpaceZ
            /**
             * xPrime val.
             */
            val xPrime = m.targetSpaceX
            /**
             * yPrime val.
             */
            val yPrime = m.targetSpaceY
            
            sumCx += (zPrime + initDx) * cosG - (xPrime + initDy) * sinG
            sumCy += (zPrime + initDx) * sinG + (xPrime + initDy) * cosG
            sumCz += yPrime + initDz
            sumCroll += m.targetSpaceRoll - initRoll
            sumCpitch += m.targetSpacePitch - initPitch
        }
        
        /**
         * initCx val.
         */
        val initCx = sumCx / count
        /**
         * initCy val.
         */
        val initCy = sumCy / count
        /**
         * initCz val.
         */
        val initCz = sumCz / count
        /**
         * initCroll val.
         */
        val initCroll = sumCroll / count
        /**
         * initCpitch val.
         */
        val initCpitch = sumCpitch / count
        
        /**
         * sumCyawVal var.
         */
        var sumCyawVal = 0.0
        for (m in measurements) {
            sumCyawVal += -m.targetSpaceYaw - (m.gyroHeading + initYaw)
        }
        /**
         * initCyaw val.
         */
        val initCyaw = sumCyawVal / count

        /**
         * p val.
         */
        val p = doubleArrayOf(
            initDx, initDy, initDz,
            initRoll, initPitch, initYaw,
            initCx, initCy, initCz,
            initCroll, initCpitch, initCyaw
        )

        /**
         * numParams val.
         */
        val numParams = p.size
        /**
         * numResiduals val.
         */
        val numResiduals = measurements.size * 6

        /**
         * lambda var.
         */
        var lambda = 0.001
        /**
         * cost var.
         */
        var cost = computeCost(p, measurements)

        /**
         * maxIterations val.
         */
        val maxIterations = 200
        /**
         * tolerance val.
         */
        val tolerance = 1e-8

        for (iter in 0 until maxIterations) {
            /**
             * J val.
             */
            val J = SimpleMatrix(numResiduals, numParams)
            /**
             * r val.
             */
            val r = SimpleMatrix(numResiduals, 1)

            /**
             * rCurrent val.
             */
            val rCurrent = computeResiduals(p, measurements)
            for (i in 0 until numResiduals) {
                r.set(i, 0, rCurrent[i])
            }

            /**
             * epsilon val.
             */
            val epsilon = 1e-6
            for (j in 0 until numParams) {
                /**
                 * pPerturbed val.
                 */
                val pPerturbed = p.clone()
                pPerturbed[j] += epsilon
                /**
                 * rPerturbed val.
                 */
                val rPerturbed = computeResiduals(pPerturbed, measurements)
                for (i in 0 until numResiduals) {
                    J.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
                }
            }

            /**
             * Jt val.
             */
            val Jt = J.transpose()
            /**
             * JtJ val.
             */
            val JtJ = Jt.mult(J)
            /**
             * Jtr val.
             */
            val Jtr = Jt.mult(r)

            /**
             * identity val.
             */
            val identity = SimpleMatrix.identity(numParams)
            /**
             * lhs val.
             */
            val lhs = JtJ.plus(identity.scale(lambda))
            
            /**
             * delta val.
             */
            val delta: SimpleMatrix
            try {
                delta = lhs.solve(Jtr.scale(-1.0))
            } catch (e: Exception) {
                lambda *= 10.0
                continue
            }

            /**
             * pNext val.
             */
            val pNext = DoubleArray(numParams)
            for (j in 0 until numParams) {
                pNext[j] = p[j] + delta.get(j, 0)
            }

            /**
             * nextCost val.
             */
            val nextCost = computeCost(pNext, measurements)
            if (nextCost < cost) {
                /**
                 * costDiff val.
                 */
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
        /**
         * cr val.
         */
        val cr = cos(roll)
        /**
         * sr val.
         */
        val sr = sin(roll)
        /**
         * cp val.
         */
        val cp = cos(pitch)
        /**
         * sp val.
         */
        val sp = sin(pitch)
        /**
         * cy val.
         */
        val cy = cos(yaw)
        /**
         * sy val.
         */
        val sy = sin(yaw)

        /**
         * Rx val.
         */
        val Rx = SimpleMatrix(arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, cr, -sr),
            doubleArrayOf(0.0, sr, cr)
        ))

        /**
         * Ry val.
         */
        val Ry = SimpleMatrix(arrayOf(
            doubleArrayOf(cy, 0.0, sy),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(-sy, 0.0, cy)
        ))

        /**
         * Rz val.
         */
        val Rz = SimpleMatrix(arrayOf(
            doubleArrayOf(cp, -sp, 0.0),
            doubleArrayOf(sp, cp, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        ))

        return Ry.mult(Rz).mult(Rx)
    }

    private fun computeResiduals(p: DoubleArray, measurements: List<CalibrationMeasurement>): DoubleArray {
        /**
         * dx val.
         */
        val dx = p[0]
        /**
         * dy val.
         */
        val dy = p[1]
        /**
         * dz val.
         */
        val dz = p[2]
        /**
         * roll val.
         */
        val roll = p[3]
        /**
         * pitch val.
         */
        val pitch = p[4]
        /**
         * yaw val.
         */
        val yaw = p[5]
        /**
         * Cx val.
         */
        val Cx = p[6]
        /**
         * Cy val.
         */
        val Cy = p[7]
        /**
         * Cz val.
         */
        val Cz = p[8]
        /**
         * Croll val.
         */
        val Croll = p[9]
        /**
         * Cpitch val.
         */
        val Cpitch = p[10]
        /**
         * Cyaw val.
         */
        val Cyaw = p[11]

        /**
         * R_cam val.
         */
        val R_cam = getRotationMatrix(roll, pitch, yaw)
        /**
         * residuals val.
         */
        val residuals = DoubleArray(measurements.size * 6)

        for (i in measurements.indices) {
            /**
             * m val.
             */
            val m = measurements[i]
            /**
             * cosG val.
             */
            val cosG = cos(m.gyroHeading)
            /**
             * sinG val.
             */
            val sinG = sin(m.gyroHeading)

            /**
             * pMeas val.
             */
            val pMeas = SimpleMatrix(arrayOf(
                doubleArrayOf(m.targetSpaceX),
                doubleArrayOf(m.targetSpaceY),
                doubleArrayOf(m.targetSpaceZ)
            ))

            /**
             * pRotated val.
             */
            val pRotated = R_cam.mult(pMeas)
            /**
             * xRot val.
             */
            val xRot = pRotated.get(0, 0)
            /**
             * yRot val.
             */
            val yRot = pRotated.get(1, 0)
            /**
             * zRot val.
             */
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
        /**
         * r val.
         */
        val r = computeResiduals(p, measurements)
        /**
         * sum var.
         */
        var sum = 0.0
        for (v in r) {
            sum += v * v
        }
        return sum
    }

    suspend fun runExtrinsicCalibration(
        sessionId: String,
        cameraIndex: Int
    ): Pose3d = withContext(Dispatchers.Default) {
        /**
         * gyroFrames val.
         */
        val gyroFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/GyroHeading" }
        /**
         * tagIdFrames val.
         */
        val tagIdFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/TagIndex" }
        /**
         * camToTagFrames val.
         */
        val camToTagFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key.startsWith("/Calibration/CameraToTag[$cameraIndex]") }

        if (gyroFrames.isEmpty() || tagIdFrames.isEmpty() || camToTagFrames.isEmpty()) {
            return@withContext Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        /**
         * tagMap val.
         */
        val tagMap = mapOf(
            1 to FieldTag(1.8, 0.0, 0.12),
            2 to FieldTag(1.8, 0.6, 0.12),
            3 to FieldTag(1.8, -0.6, 0.12),
            4 to FieldTag(-1.8, 0.0, 0.12)
        )

        /**
         * measurements val.
         */
        val measurements = mutableListOf<CalibrationMeasurement>()
        /**
         * timeMap val.
         */
        val timeMap = gyroFrames.associateBy { it.timestampMs }

        for (tagFrame in tagIdFrames) {
            /**
             * t val.
             */
            val t = tagFrame.timestampMs
            /**
             * gyro val.
             */
            val gyro = timeMap[t]?.value ?: continue
            /**
             * tagId val.
             */
            val tagId = tagFrame.value.toInt()
            /**
             * tagField val.
             */
            val tagField = tagMap[tagId] ?: continue

            /**
             * x val.
             */
            val x = getVal(camToTagFrames, t, 0)
            /**
             * y val.
             */
            val y = getVal(camToTagFrames, t, 1)
            /**
             * z val.
             */
            val z = getVal(camToTagFrames, t, 2)
            /**
             * roll val.
             */
            val roll = getVal(camToTagFrames, t, 3)
            /**
             * pitch val.
             */
            val pitch = getVal(camToTagFrames, t, 4)
            /**
             * yaw val.
             */
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

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun solveCameraExtrinsicsWithDiagnostics(measurements: List<CalibrationMeasurement>): CalibrationDiagnostics {
        if (measurements.isEmpty()) {
            return CalibrationDiagnostics(
                pose = Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                standardErrors = DoubleArray(6),
                covarianceMatrix = Array(6) { DoubleArray(6) },
                reducedChiSquared = 0.0
            )
        }

        /**
         * sumX var.
         */
        var sumX = 0.0
        /**
         * sumY var.
         */
        var sumY = 0.0
        /**
         * sumZ var.
         */
        var sumZ = 0.0
        /**
         * sumRoll var.
         */
        var sumRoll = 0.0
        /**
         * sumPitch var.
         */
        var sumPitch = 0.0
        /**
         * sumYaw var.
         */
        var sumYaw = 0.0

        for (m in measurements) {
            /**
             * cosG val.
             */
            val cosG = cos(m.gyroHeading)
            /**
             * sinG val.
             */
            val sinG = sin(m.gyroHeading)
            sumX += m.targetSpaceZ * cosG - m.targetSpaceX * sinG
            sumY += m.targetSpaceZ * sinG + m.targetSpaceX * cosG
            sumZ += m.targetSpaceY
            sumRoll += m.targetSpaceRoll
            sumPitch += m.targetSpacePitch
            sumYaw += -m.targetSpaceYaw
        }

        /**
         * count val.
         */
        val count = measurements.size.toDouble()
        /**
         * initDx val.
         */
        val initDx = sumX / count
        /**
         * initDy val.
         */
        val initDy = sumY / count
        /**
         * initDz val.
         */
        val initDz = sumZ / count
        /**
         * initRoll val.
         */
        val initRoll = sumRoll / count
        /**
         * initPitch val.
         */
        val initPitch = sumPitch / count
        /**
         * initYaw val.
         */
        val initYaw = sumYaw / count

        /**
         * sumCx var.
         */
        var sumCx = 0.0
        /**
         * sumCy var.
         */
        var sumCy = 0.0
        /**
         * sumCz var.
         */
        var sumCz = 0.0
        /**
         * sumCroll var.
         */
        var sumCroll = 0.0
        /**
         * sumCpitch var.
         */
        var sumCpitch = 0.0

        for (m in measurements) {
            /**
             * cosG val.
             */
            val cosG = cos(m.gyroHeading)
            /**
             * sinG val.
             */
            val sinG = sin(m.gyroHeading)
            /**
             * zPrime val.
             */
            val zPrime = m.targetSpaceZ
            /**
             * xPrime val.
             */
            val xPrime = m.targetSpaceX
            /**
             * yPrime val.
             */
            val yPrime = m.targetSpaceY
            
            sumCx += (zPrime + initDx) * cosG - (xPrime + initDy) * sinG
            sumCy += (zPrime + initDx) * sinG + (xPrime + initDy) * cosG
            sumCz += yPrime + initDz
            sumCroll += m.targetSpaceRoll - initRoll
            sumCpitch += m.targetSpacePitch - initPitch
        }
        
        /**
         * initCx val.
         */
        val initCx = sumCx / count
        /**
         * initCy val.
         */
        val initCy = sumCy / count
        /**
         * initCz val.
         */
        val initCz = sumCz / count
        /**
         * initCroll val.
         */
        val initCroll = sumCroll / count
        /**
         * initCpitch val.
         */
        val initCpitch = sumCpitch / count
        
        /**
         * sumCyawVal var.
         */
        var sumCyawVal = 0.0
        for (m in measurements) {
            sumCyawVal += -m.targetSpaceYaw - (m.gyroHeading + initYaw)
        }
        /**
         * initCyaw val.
         */
        val initCyaw = sumCyawVal / count

        /**
         * p val.
         */
        val p = doubleArrayOf(
            initDx, initDy, initDz,
            initRoll, initPitch, initYaw,
            initCx, initCy, initCz,
            initCroll, initCpitch, initCyaw
        )

        /**
         * numParams val.
         */
        val numParams = p.size
        /**
         * numResiduals val.
         */
        val numResiduals = measurements.size * 6

        /**
         * lambda var.
         */
        var lambda = 0.001
        /**
         * cost var.
         */
        var cost = computeCost(p, measurements)

        /**
         * maxIterations val.
         */
        val maxIterations = 200
        /**
         * tolerance val.
         */
        val tolerance = 1e-8

        for (iter in 0 until maxIterations) {
            /**
             * J val.
             */
            val J = SimpleMatrix(numResiduals, numParams)
            /**
             * r val.
             */
            val r = SimpleMatrix(numResiduals, 1)

            /**
             * rCurrent val.
             */
            val rCurrent = computeResiduals(p, measurements)
            for (i in 0 until numResiduals) {
                r.set(i, 0, rCurrent[i])
            }

            /**
             * epsilon val.
             */
            val epsilon = 1e-6
            for (j in 0 until numParams) {
                /**
                 * pPerturbed val.
                 */
                val pPerturbed = p.clone()
                pPerturbed[j] += epsilon
                /**
                 * rPerturbed val.
                 */
                val rPerturbed = computeResiduals(pPerturbed, measurements)
                for (i in 0 until numResiduals) {
                    J.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
                }
            }

            /**
             * Jt val.
             */
            val Jt = J.transpose()
            /**
             * JtJ val.
             */
            val JtJ = Jt.mult(J)
            /**
             * Jtr val.
             */
            val Jtr = Jt.mult(r)

            /**
             * identity val.
             */
            val identity = SimpleMatrix.identity(numParams)
            /**
             * lhs val.
             */
            val lhs = JtJ.plus(identity.scale(lambda))
            
            /**
             * delta val.
             */
            val delta: SimpleMatrix
            try {
                delta = lhs.solve(Jtr.scale(-1.0))
            } catch (e: Exception) {
                lambda *= 10.0
                continue
            }

            /**
             * pNext val.
             */
            val pNext = DoubleArray(numParams)
            for (j in 0 until numParams) {
                pNext[j] = p[j] + delta.get(j, 0)
            }

            /**
             * nextCost val.
             */
            val nextCost = computeCost(pNext, measurements)
            if (nextCost < cost) {
                /**
                 * costDiff val.
                 */
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

        /**
         * finalJ val.
         */
        val finalJ = SimpleMatrix(numResiduals, numParams)
        /**
         * rCurrent val.
         */
        val rCurrent = computeResiduals(p, measurements)
        /**
         * epsilon val.
         */
        val epsilon = 1e-6
        for (j in 0 until numParams) {
            /**
             * pPerturbed val.
             */
            val pPerturbed = p.clone()
            pPerturbed[j] += epsilon
            /**
             * rPerturbed val.
             */
            val rPerturbed = computeResiduals(pPerturbed, measurements)
            for (i in 0 until numResiduals) {
                finalJ.set(i, j, (rPerturbed[i] - rCurrent[i]) / epsilon)
            }
        }

        /**
         * Jt val.
         */
        val Jt = finalJ.transpose()
        /**
         * JtJ val.
         */
        val JtJ = Jt.mult(finalJ)

        /**
         * covMatrix val.
         */
        val covMatrix = try {
            JtJ.invert()
        } catch (e: Exception) {
            /**
             * regularizedJtJ val.
             */
            val regularizedJtJ = JtJ.plus(SimpleMatrix.identity(numParams).scale(1e-6))
            try {
                regularizedJtJ.invert()
            } catch (e2: Exception) {
                SimpleMatrix(numParams, numParams)
            }
        }

        /**
         * sumSquaredResiduals val.
         */
        val sumSquaredResiduals = computeCost(p, measurements)
        /**
         * degreesOfFreedom val.
         */
        val degreesOfFreedom = (numResiduals - numParams).coerceAtLeast(1)
        /**
         * chiSquaredReduced val.
         */
        val chiSquaredReduced = sumSquaredResiduals / degreesOfFreedom

        /**
         * standardErrors val.
         */
        val standardErrors = DoubleArray(6)
        for (j in 0 until 6) {
            /**
             * variance val.
             */
            val variance = covMatrix.get(j, j)
            standardErrors[j] = sqrt(max(0.0, variance) * chiSquaredReduced)
        }

        /**
         * covariance6x6 val.
         */
        val covariance6x6 = Array(6) { r ->
            DoubleArray(6) { c ->
                covMatrix.get(r, c) * chiSquaredReduced
            }
        }

        /**
         * solvedPose val.
         */
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
        /**
         * gyroFrames val.
         */
        val gyroFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/GyroHeading" }
        /**
         * tagIdFrames val.
         */
        val tagIdFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key == "/Calibration/TagIndex" }
        /**
         * camToTagFrames val.
         */
        val camToTagFrames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE).filter { it.key.startsWith("/Calibration/CameraToTag[$cameraIndex]") }

        if (gyroFrames.isEmpty() || tagIdFrames.isEmpty() || camToTagFrames.isEmpty()) {
            return@withContext CalibrationDiagnostics(
                pose = Pose3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                standardErrors = DoubleArray(6),
                covarianceMatrix = Array(6) { DoubleArray(6) },
                reducedChiSquared = 0.0
            )
        }

        /**
         * tagMap val.
         */
        val tagMap = mapOf(
            1 to FieldTag(1.8, 0.0, 0.12),
            2 to FieldTag(1.8, 0.6, 0.12),
            3 to FieldTag(1.8, -0.6, 0.12),
            4 to FieldTag(-1.8, 0.0, 0.12)
        )

        /**
         * groupedFrames val.
         */
        val groupedFrames = camToTagFrames.groupBy { frame ->
            /**
             * key val.
             */
            val key = frame.key
            /**
             * lastOpenBracket val.
             */
            val lastOpenBracket = key.lastIndexOf('[')
            /**
             * lastCloseBracket val.
             */
            val lastCloseBracket = key.lastIndexOf(']')
            if (lastOpenBracket != -1 && lastCloseBracket != -1 && lastCloseBracket > lastOpenBracket) {
                key.substring(lastOpenBracket + 1, lastCloseBracket).toIntOrNull() ?: -1
            } else {
                -1
            }
        }

        /**
         * measurements val.
         */
        val measurements = mutableListOf<CalibrationMeasurement>()
        /**
         * timeMap val.
         */
        val timeMap = gyroFrames.associateBy { it.timestampMs }

        /**
         * list0 val.
         */
        val list0 = groupedFrames[0] ?: emptyList()
        /**
         * list1 val.
         */
        val list1 = groupedFrames[1] ?: emptyList()
        /**
         * list2 val.
         */
        val list2 = groupedFrames[2] ?: emptyList()
        /**
         * list3 val.
         */
        val list3 = groupedFrames[3] ?: emptyList()
        /**
         * list4 val.
         */
        val list4 = groupedFrames[4] ?: emptyList()
        /**
         * list5 val.
         */
        val list5 = groupedFrames[5] ?: emptyList()

        for (tagFrame in tagIdFrames) {
            /**
             * t val.
             */
            val t = tagFrame.timestampMs
            /**
             * gyro val.
             */
            val gyro = timeMap[t]?.value ?: continue
            /**
             * tagId val.
             */
            val tagId = tagFrame.value.toInt()
            /**
             * tagField val.
             */
            val tagField = tagMap[tagId] ?: continue

            /**
             * x val.
             */
            val x = getVal(list0, t, 0)
            /**
             * y val.
             */
            val y = getVal(list1, t, 1)
            /**
             * z val.
             */
            val z = getVal(list2, t, 2)
            /**
             * roll val.
             */
            val roll = getVal(list3, t, 3)
            /**
             * pitch val.
             */
            val pitch = getVal(list4, t, 4)
            /**
             * yaw val.
             */
            val yaw = getVal(list5, t, 5)

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

    private fun getVal(targetList: List<TelemetryFrame>, timestampMs: Long, index: Int): Double {
        if (targetList.isEmpty()) return 0.0

        /**
         * targetTime val.
         */
        val targetTime = timestampMs + index
        /**
         * low var.
         */
        var low = 0
        /**
         * high var.
         */
        var high = targetList.size - 1
        /**
         * bestFrame var.
         */
        var bestFrame = targetList[0]
        /**
         * minDiff var.
         */
        var minDiff = abs(bestFrame.timestampMs - targetTime)

        while (low <= high) {
            /**
             * mid val.
             */
            val mid = (low + high) ushr 1
            /**
             * midFrame val.
             */
            val midFrame = targetList[mid]
            /**
             * diff val.
             */
            val diff = abs(midFrame.timestampMs - targetTime)
            if (diff < minDiff) {
                minDiff = diff
                bestFrame = midFrame
            }
            if (midFrame.timestampMs == targetTime) {
                return midFrame.value
            } else if (midFrame.timestampMs < targetTime) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return bestFrame.value
    }
}
