package com.ares.analytics.service

import com.ares.analytics.shared.CalculatedSummary
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.shared.TransientClassification
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import org.ejml.simple.SimpleMatrix
import kotlin.math.abs
import kotlin.math.sign

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SysIdService(private val databaseService: DatabaseService) {

    suspend fun analyzeMotorData(
        sessionId: String,
        voltageKey: String,
        velocityKey: String,
        accelerationKey: String
    ): CalculatedSummary {
        /**
         * start val.
         */
        val start = 0L
        /**
         * end val.
         */
        val end = Long.MAX_VALUE

        /**
         * voltages val.
         */
        val voltages = databaseService.getTelemetryRange(sessionId, start, end).filter { it.key == voltageKey }
        /**
         * velocities val.
         */
        val velocities = databaseService.getTelemetryRange(sessionId, start, end).filter { it.key == velocityKey }
        /**
         * accelerations val.
         */
        val accelerations = databaseService.getTelemetryRange(sessionId, start, end).filter { it.key == accelerationKey }

        if (voltages.isEmpty() || velocities.isEmpty() || accelerations.isEmpty()) {
            return CalculatedSummary()
        }

        // Align data by matching timestamps (nearest neighbor or exact matching)
        /**
         * timeMap val.
         */
        val timeMap = voltages.associateBy { it.timestampMs }
        /**
         * alignedData val.
         */
        val alignedData = mutableListOf<AlignedDataRow>()

        // Identify direction change timestamps (sign of velocity changes)
        /**
         * directionChanges val.
         */
        val directionChanges = mutableListOf<Long>()
        /**
         * lastSign var.
         */
        var lastSign = 0.0
        /**
         * sortedVelocities val.
         */
        val sortedVelocities = velocities.sortedBy { it.timestampMs }
        for (v in sortedVelocities) {
            /**
             * currentSign val.
             */
            val currentSign = sign(v.value)
            if (currentSign != 0.0 && currentSign != lastSign) {
                directionChanges.add(v.timestampMs)
                lastSign = currentSign
            }
        }

        /**
         * sortedAccels val.
         */
        val sortedAccels = accelerations.sortedBy { it.timestampMs }
        /**
         * accelIdx var.
         */
        var accelIdx = 0

        for (v in sortedVelocities) {
            /**
             * t val.
             */
            val t = v.timestampMs

            // Apply direction change cleansing: skip data points within ±50ms of a sign change
            /**
             * isNearDirectionChange val.
             */
            val isNearDirectionChange = directionChanges.any { abs(it - t) <= 50 }
            if (isNearDirectionChange) continue

            /**
             * volt val.
             */
            val volt = timeMap[t]?.value ?: continue

            // Move accelIdx forward to find nearest neighbor in O(N + M)
            while (accelIdx < sortedAccels.size - 1 &&
                abs(sortedAccels[accelIdx + 1].timestampMs - t) <= abs(sortedAccels[accelIdx].timestampMs - t)
            ) {
                accelIdx++
            }
            if (sortedAccels.isEmpty()) continue
            /**
             * accel val.
             */
            val accel = sortedAccels[accelIdx].value

            alignedData.add(AlignedDataRow(t, volt, v.value, accel))
        }

        return analyzeRawData(alignedData)
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun analyzeRawData(alignedData: List<AlignedDataRow>): CalculatedSummary {
        if (alignedData.size < 10) {
            return CalculatedSummary()
        }

        // Solve OLS: V = kS * sgn(v) + kV * v + kA * a
        // Construct matrices
        /**
         * n val.
         */
        val n = alignedData.size
        /**
         * X val.
         */
        val X = SimpleMatrix(n, 3)
        /**
         * y val.
         */
        val y = SimpleMatrix(n, 1)

        for (i in 0 until n) {
            /**
             * row val.
             */
            val row = alignedData[i]
            X.setRow(i, 0, sign(row.velocity), row.velocity, row.accel)
            y.set(i, 0, row.voltage)
        }

        return try {
            /**
             * Xt val.
             */
            val Xt = X.transpose()
            /**
             * XtX val.
             */
            val XtX = Xt.mult(X)
            /**
             * XtX_inv val.
             */
            val XtX_inv = XtX.invert()
            /**
             * beta val.
             */
            val beta = XtX_inv.mult(Xt).mult(y)

            /**
             * kS val.
             */
            val kS = beta.get(0, 0)
            /**
             * kV val.
             */
            val kV = beta.get(1, 0)
            /**
             * kA val.
             */
            val kA = beta.get(2, 0)

            // Compute R-squared
            /**
             * yMean val.
             */
            val yMean = alignedData.map { it.voltage }.average()
            /**
             * ssTot var.
             */
            var ssTot = 0.0
            /**
             * ssRes var.
             */
            var ssRes = 0.0
            for (i in 0 until n) {
                /**
                 * actual val.
                 */
                val actual = y.get(i, 0)
                /**
                 * predicted val.
                 */
                val predicted = kS * sign(alignedData[i].velocity) + kV * alignedData[i].velocity + kA * alignedData[i].accel
                ssTot += (actual - yMean) * (actual - yMean)
                ssRes += (actual - predicted) * (actual - predicted)
            }
            /**
             * rSquared val.
             */
            val rSquared = if (ssTot > 0) 1.0 - (ssRes / ssTot) else 0.0

            // Classify transient response
            /**
             * transientClassification val.
             */
            val transientClassification = classifyTransient(alignedData)

            CalculatedSummary(
                kS = kS,
                kV = kV,
                kA = kA,
                rSquared = rSquared,
                transientClassification = transientClassification
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CalculatedSummary()
        }
    }

    private fun classifyTransient(data: List<AlignedDataRow>): TransientClassification {
        // Find a step-like voltage increase (e.g. from < 1.0 to > 6.0)
        /**
         * stepStartIdx var.
         */
        var stepStartIdx = -1
        for (i in 1 until data.size) {
            if (data[i - 1].voltage < 1.0 && data[i].voltage > 6.0) {
                stepStartIdx = i
                break
            }
        }
        if (stepStartIdx == -1) return TransientClassification.UNKNOWN

        // Trace velocity after step
        /**
         * transientPoints val.
         */
        val transientPoints = data.subList(stepStartIdx, minOf(stepStartIdx + 30, data.size))
        if (transientPoints.isEmpty()) return TransientClassification.UNKNOWN

        /**
         * maxVel val.
         */
        val maxVel = transientPoints.maxOf { it.velocity }
        // Find steady state velocity (average of last 10 points)
        /**
         * steadyStateVel val.
         */
        val steadyStateVel = if (transientPoints.size > 10) {
            transientPoints.takeLast(10).map { it.velocity }.average()
        } else {
            transientPoints.last().velocity
        }

        if (steadyStateVel <= 0.0) return TransientClassification.UNKNOWN

        /**
         * overshoot val.
         */
        val overshoot = (maxVel - steadyStateVel) / steadyStateVel
        return when {
            overshoot > 0.05 -> TransientClassification.UNDERDAMPED
            overshoot < -0.05 -> TransientClassification.OVERDAMPED // very sluggish
            else -> TransientClassification.CRITICALLY_DAMPED
        }
    }

    /**
     * FFT analysis of a telemetry signal to find dominant frequencies (e.g. vibrations or oscillations).
     */
    fun performFftAnalysis(values: DoubleArray, sampleRateHz: Double): FftResult {
        if (values.size < 4) return FftResult(emptyDoubleArray(), emptyDoubleArray(), 0.0)

        // FFT size must be power of two
        /**
         * n val.
         */
        val n = values.size
        /**
         * nextPow2 val.
         */
        val nextPow2 = nextPowerOfTwo(n)
        /**
         * padded val.
         */
        val padded = DoubleArray(nextPow2)
        System.arraycopy(values, 0, padded, 0, n)

        /**
         * transformer val.
         */
        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        /**
         * complex val.
         */
        val complex = transformer.transform(padded, TransformType.FORWARD)

        // Magnitudes of first half
        /**
         * half val.
         */
        val half = nextPow2 / 2
        /**
         * frequencies val.
         */
        val frequencies = DoubleArray(half)
        /**
         * magnitudes val.
         */
        val magnitudes = DoubleArray(half)

        for (i in 0 until half) {
            frequencies[i] = i * sampleRateHz / nextPow2
            magnitudes[i] = complex[i].abs()
        }

        // Find dominant frequency (excluding DC component at index 0)
        /**
         * maxMag var.
         */
        var maxMag = 0.0
        /**
         * dominantFreq var.
         */
        var dominantFreq = 0.0
        for (i in 1 until half) {
            if (magnitudes[i] > maxMag) {
                maxMag = magnitudes[i]
                dominantFreq = frequencies[i]
            }
        }

        return FftResult(frequencies, magnitudes, dominantFreq)
    }

    private fun nextPowerOfTwo(n: Int): Int {
        /**
         * k var.
         */
        var k = 1
        while (k < n) k = k shl 1
        return k
    }

    private fun emptyDoubleArray() = DoubleArray(0)
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AlignedDataRow(
    /**
     * timestampMs val.
     */
    val timestampMs: Long,
    /**
     * voltage val.
     */
    val voltage: Double,
    /**
     * velocity val.
     */
    val velocity: Double,
    /**
     * accel val.
     */
    val accel: Double
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class FftResult(
    /**
     * frequencies val.
     */
    val frequencies: DoubleArray,
    /**
     * magnitudes val.
     */
    val magnitudes: DoubleArray,
    /**
     * dominantFrequency val.
     */
    val dominantFrequency: Double
)
