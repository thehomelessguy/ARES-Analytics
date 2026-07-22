package com.ares.analytics.service

import com.ares.analytics.service.calibration.CameraCalibrationSolver
import com.ares.analytics.service.calibration.OdometryCalibrationSolver

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class Pose3d(
    /**
     * x val.
     */
    val x: Double, // Left-Right (meters)
    /**
     * y val.
     */
    val y: Double, // Up-Down (meters)
    /**
     * z val.
     */
    val z: Double, // Depth (meters)
    /**
     * roll val.
     */
    val roll: Double,
    /**
     * pitch val.
     */
    val pitch: Double,
    /**
     * yaw val.
     */
    val yaw: Double // Heading (radians, CCW-positive)
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class CalibrationDiagnostics(
    /**
     * pose val.
     */
    val pose: Pose3d,
    /**
     * standardErrors val.
     */
    val standardErrors: DoubleArray,
    /**
     * covarianceMatrix val.
     */
    val covarianceMatrix: Array<DoubleArray>,
    /**
     * reducedChiSquared val.
     */
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
        /**
         * result var.
         */
        var result = pose.hashCode()
        result = 31 * result + standardErrors.contentHashCode()
        result = 31 * result + covarianceMatrix.contentDeepHashCode()
        result = 31 * result + reducedChiSquared.hashCode()
        return result
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
data class CalibrationMeasurement(
    /**
     * gyroHeading val.
     */
    val gyroHeading: Double, // radians (CCW-positive)
    /**
     * tagId val.
     */
    val tagId: Int,
    /**
     * tagFieldX val.
     */
    val tagFieldX: Double,
    /**
     * tagFieldY val.
     */
    val tagFieldY: Double,
    /**
     * tagFieldZ val.
     */
    val tagFieldZ: Double,
    // Tag relative target space measurements from Limelight
    /**
     * targetSpaceX val.
     */
    val targetSpaceX: Double,
    /**
     * targetSpaceY val.
     */
    val targetSpaceY: Double,
    /**
     * targetSpaceZ val.
     */
    val targetSpaceZ: Double,
    /**
     * targetSpaceRoll val.
     */
    val targetSpaceRoll: Double,
    /**
     * targetSpacePitch val.
     */
    val targetSpacePitch: Double,
    /**
     * targetSpaceYaw val.
     */
    val targetSpaceYaw: Double
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class CalibrationService(private val databaseService: DatabaseService) {

    private val cameraSolver = CameraCalibrationSolver(databaseService)
    private val odometrySolver = OdometryCalibrationSolver(databaseService)

    /**
     * Solves for the 6-DOF camera extrinsic calibration offset using a Levenberg-Marquardt approach
     */
    fun solveCameraExtrinsics(measurements: List<CalibrationMeasurement>): Pose3d {
        return cameraSolver.solveCameraExtrinsics(measurements)
    }

    /**
     * Scans NT4 telemetry from a calibration run to pull measurements and run the solver.
     */
    suspend fun runExtrinsicCalibration(
        sessionId: String,
        cameraIndex: Int
    ): Pose3d {
        return cameraSolver.runExtrinsicCalibration(sessionId, cameraIndex)
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
        return cameraSolver.solveCameraExtrinsicsWithDiagnostics(measurements)
    }

    suspend fun runExtrinsicCalibrationWithDiagnostics(
        sessionId: String,
        cameraIndex: Int
    ): CalibrationDiagnostics {
        return cameraSolver.runExtrinsicCalibrationWithDiagnostics(sessionId, cameraIndex)
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
data class FieldTag(val x: Double, val y: Double, val z: Double)
