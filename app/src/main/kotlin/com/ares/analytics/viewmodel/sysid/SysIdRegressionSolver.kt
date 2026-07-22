package com.ares.analytics.viewmodel.sysid

import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.viewmodel.SysIdState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.ejml.simple.SimpleMatrix

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SysIdRegressionSolver(
    private val nt4ClientService: Nt4ClientService,
    private val _state: MutableStateFlow<SysIdState>
) {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun runCalibrationAnalysis(calibrationType: String, data: List<DoubleArray>) {
        if (data.size < 10) {
            _state.update { it.copy(errorMessage = "Not enough calibration data collected (minimum 10 points)") }
            return
        }
        
        try {
            when (calibrationType) {
                "PINPOINT_SPIN" -> {
                    /**
                     * n val.
                     */
                    val n = data.size
                    /**
                     * A val.
                     */
                    val A = SimpleMatrix(2 * n, 4)
                    /**
                     * b val.
                     */
                    val b = SimpleMatrix(2 * n, 1)
                    
                    for (i in 0 until n) {
                        /**
                         * row val.
                         */
                        val row = data[i]
                        /**
                         * x val.
                         */
                        val x = row[1]
                        /**
                         * y val.
                         */
                        val y = row[2]
                        /**
                         * heading val.
                         */
                        val heading = row[3]
                        /**
                         * cosT val.
                         */
                        val cosT = kotlin.math.cos(heading)
                        /**
                         * sinT val.
                         */
                        val sinT = kotlin.math.sin(heading)
                        
                        A.setRow(2 * i, 0, 1.0, 0.0, cosT, -sinT)
                        A.setRow(2 * i + 1, 0, 0.0, 1.0, sinT, cosT)
                        
                        b.set(2 * i, 0, x)
                        b.set(2 * i + 1, 0, y)
                    }
                    
                    /**
                     * beta val.
                     */
                    val beta = A.solve(b)
                    /**
                     * dxMeters val.
                     */
                    val dxMeters = beta.get(2, 0)
                    /**
                     * dyMeters val.
                     */
                    val dyMeters = beta.get(3, 0)
                    
                    /**
                     * deltaXOffsetMm val.
                     */
                    val deltaXOffsetMm = dxMeters * 1000.0
                    /**
                     * deltaYOffsetMm val.
                     */
                    val deltaYOffsetMm = dyMeters * 1000.0
                    
                    _state.update {
                        it.copy(
                            recommendedPinpointXOffsetMm = deltaXOffsetMm,
                            recommendedPinpointYOffsetMm = deltaYOffsetMm,
                            errorMessage = null
                        )
                    }
                }
                "TRACK_WIDTH_SPIN" -> {
                    /**
                     * n val.
                     */
                    val n = data.size
                    /**
                     * accumHeading var.
                     */
                    var accumHeading = 0.0
                    /**
                     * lastHeading var.
                     */
                    var lastHeading = data[0][5]
                    /**
                     * unwrappedHeadings val.
                     */
                    val unwrappedHeadings = DoubleArray(n)
                    unwrappedHeadings[0] = 0.0
                    
                    for (i in 1 until n) {
                        /**
                         * currentHeading val.
                         */
                        val currentHeading = data[i][5]
                        /**
                         * diff var.
                         */
                        var diff = currentHeading - lastHeading
                        while (diff < -kotlin.math.PI) diff += 2 * kotlin.math.PI
                        while (diff > kotlin.math.PI) diff -= 2 * kotlin.math.PI
                        accumHeading += diff
                        unwrappedHeadings[i] = accumHeading
                        lastHeading = currentHeading
                    }
                    
                    /**
                     * sumXY var.
                     */
                    var sumXY = 0.0
                    /**
                     * sumX2 var.
                     */
                    var sumX2 = 0.0
                    
                    /**
                     * fl0 val.
                     */
                    val fl0 = data[0][1]
                    /**
                     * fr0 val.
                     */
                    val fr0 = data[0][2]
                    /**
                     * rl0 val.
                     */
                    val rl0 = data[0][3]
                    /**
                     * rr0 val.
                     */
                    val rr0 = data[0][4]
                    
                    for (i in 0 until n) {
                        /**
                         * fl val.
                         */
                        val fl = data[i][1] - fl0
                        /**
                         * fr val.
                         */
                        val fr = data[i][2] - fr0
                        /**
                         * rl val.
                         */
                        val rl = data[i][3] - rl0
                        /**
                         * rr val.
                         */
                        val rr = data[i][4] - rr0
                        
                        /**
                         * y val.
                         */
                        val y = -fl + fr - rl + rr
                        /**
                         * x val.
                         */
                        val x = 4.0 * unwrappedHeadings[i]
                        
                        sumXY += x * y
                        sumX2 += x * x
                    }
                    
                    /**
                     * k val.
                     */
                    val k = if (sumX2 > 1e-6) sumXY / sumX2 else 0.45
                    
                    /**
                     * wheelBase val.
                     */
                    val wheelBase = nt4ClientService.latestValues["Tuning/wheelBaseMeters"]?.value ?: 0.45
                    /**
                     * recTrackWidth val.
                     */
                    val recTrackWidth = 2.0 * k - wheelBase
                    
                    _state.update {
                        it.copy(
                            recommendedTrackWidthMeters = recTrackWidth,
                            errorMessage = null
                        )
                    }
                }
                "VISION_CALIBRATION" -> {
                    /**
                     * n val.
                     */
                    val n = data.size
                    /**
                     * meanX val.
                     */
                    val meanX = data.map { it[1] }.average()
                    /**
                     * meanY val.
                     */
                    val meanY = data.map { it[2] }.average()
                    /**
                     * meanHeading val.
                     */
                    val meanHeading = data.map { it[3] }.average()
                    
                    /**
                     * varX var.
                     */
                    var varX = 0.0
                    /**
                     * varY var.
                     */
                    var varY = 0.0
                    /**
                     * varHeading var.
                     */
                    var varHeading = 0.0
                    
                    for (row in data) {
                        /**
                         * dx val.
                         */
                        val dx = row[1] - meanX
                        /**
                         * dy val.
                         */
                        val dy = row[2] - meanY
                        
                        /**
                         * dHeading var.
                         */
                        var dHeading = row[3] - meanHeading
                        while (dHeading < -kotlin.math.PI) dHeading += 2 * kotlin.math.PI
                        while (dHeading > kotlin.math.PI) dHeading -= 2 * kotlin.math.PI
                        
                        varX += dx * dx
                        varY += dy * dy
                        varHeading += dHeading * dHeading
                    }
                    
                    /**
                     * stdX val.
                     */
                    val stdX = kotlin.math.sqrt(varX / (n - 1))
                    /**
                     * stdY val.
                     */
                    val stdY = kotlin.math.sqrt(varY / (n - 1))
                    /**
                     * stdHeading val.
                     */
                    val stdHeading = kotlin.math.sqrt(varHeading / (n - 1))
                    
                    _state.update {
                        it.copy(
                            recommendedVisionStdDevsX = stdX,
                            recommendedVisionStdDevsY = stdY,
                            recommendedVisionStdDevsHeading = stdHeading,
                            errorMessage = null
                        )
                    }
                }
                "LINEAR_DRIVE" -> {
                    /**
                     * firstDisplacement val.
                     */
                    val firstDisplacement = data.firstOrNull()?.get(1) ?: 0.0
                    /**
                     * lastDisplacement val.
                     */
                    val lastDisplacement = data.lastOrNull()?.get(1) ?: 0.0
                    /**
                     * reportedDisplacement val.
                     */
                    val reportedDisplacement = lastDisplacement - firstDisplacement
                    
                    /**
                     * actualDistance val.
                     */
                    val actualDistance = _state.value.linearDriveActualDistanceMeters
                    
                    if (actualDistance > 0.1 && reportedDisplacement > 0.05) {
                        /**
                         * currentTicks val.
                         */
                        val currentTicks = nt4ClientService.latestValues["Tuning/ticksPerMeter"]?.value ?: 2000.0
                        /**
                         * recTicks val.
                         */
                        val recTicks = currentTicks * (reportedDisplacement / actualDistance)
                        
                        _state.update {
                            it.copy(
                                recommendedTicksPerMeter = recTicks,
                                errorMessage = null
                            )
                        }
                    } else {
                        _state.update { it.copy(errorMessage = "Insufficient linear displacement or invalid physical distance input.") }
                    }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = "Calibration analysis failed: ${e.message}") }
        }
    }
}
