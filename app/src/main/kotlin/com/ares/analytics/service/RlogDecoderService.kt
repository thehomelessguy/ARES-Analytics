package com.ares.analytics.service

import com.ares.analytics.shared.TelemetryFrame
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class RlogDecoderService {

    suspend fun parseRlog(
        file: File,
        sessionId: String,
        batcher: FrameBatcher
    ) {
        /**
         * bytes val.
         */
        val bytes = file.readBytes()
        if (bytes.size < 2) return

        /**
         * buffer val.
         */
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        /**
         * offset var.
         */
        var offset = 0

        /**
         * logRevision val.
         */
        val logRevision = bytes[offset].toInt() and 0xFF
        offset += 1
        offset += 1 // skip second byte

        if (logRevision != 1 && logRevision != 2) return

        /**
         * keyIDs val.
         */
        val keyIDs = mutableMapOf<Int, String>()
        /**
         * keyTypes val.
         */
        val keyTypes = mutableMapOf<Int, String>()

        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        fun readString(length: Int): String {
            /**
             * str val.
             */
            val str = String(bytes, offset, length, Charsets.UTF_8)
            offset += length
            return str
        }

        try {
            while (offset < bytes.size) {
                if (offset + 8 > bytes.size) break
                /**
                 * timestampSec val.
                 */
                val timestampSec = buffer.getDouble(offset)
                offset += 8
                /**
                 * timestampMs val.
                 */
                val timestampMs = (timestampSec * 1000.0).toLong()

                while (true) {
                    if (offset >= bytes.size) break
                    /**
                     * type val.
                     */
                    val type = bytes[offset].toInt() and 0xFF
                    offset += 1

                    if (type == 0) {
                        // New timestamp record boundary
                        break
                    }

                    when (type) {
                        1 -> { // New key ID
                            /**
                             * keyID val.
                             */
                            val keyID = buffer.getShort(offset).toInt() and 0xFFFF
                            offset += 2
                            /**
                             * keyLength val.
                             */
                            val keyLength = buffer.getShort(offset).toInt() and 0xFFFF
                            offset += 2
                            /**
                             * newKey val.
                             */
                            val newKey = readString(keyLength)
                            keyIDs[keyID] = newKey

                            if (logRevision == 2) {
                                /**
                                 * typeLength val.
                                 */
                                val typeLength = buffer.getShort(offset).toInt() and 0xFFFF
                                offset += 2
                                /**
                                 * newType val.
                                 */
                                val newType = readString(typeLength)
                                keyTypes[keyID] = newType
                            }
                        }
                        2 -> { // Updated field
                            /**
                             * keyID val.
                             */
                            val keyID = buffer.getShort(offset).toInt() and 0xFFFF
                            offset += 2
                            /**
                             * keyName val.
                             */
                            val keyName = keyIDs[keyID] ?: continue

                            when {
                                logRevision == 2 -> {
                                    /**
                                     * valueLength val.
                                     */
                                    val valueLength = buffer.getShort(offset).toInt() and 0xFFFF
                                    offset += 2
                                    /**
                                     * fieldType val.
                                     */
                                    val fieldType = keyTypes[keyID] ?: "unknown"

                                    /**
                                     * startValOffset val.
                                     */
                                    val startValOffset = offset
                                    when (fieldType) {
                                        "boolean" -> {
                                            /**
                                             * v val.
                                             */
                                            val v = bytes[offset].toInt() != 0
                                            offset += 1
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, if (v) 1.0 else 0.0))
                                        }
                                        "int", "int64" -> {
                                            /**
                                             * v val.
                                             */
                                            val v = buffer.getLong(offset)
                                            offset += 8
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v.toDouble()))
                                        }
                                        "float" -> {
                                            /**
                                             * v val.
                                             */
                                            val v = buffer.getFloat(offset)
                                            offset += 4
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v.toDouble()))
                                        }
                                        "double" -> {
                                            /**
                                             * v val.
                                             */
                                            val v = buffer.getDouble(offset)
                                            offset += 8
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v))
                                        }
                                        "boolean[]" -> {
                                            for (i in 0 until valueLength) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = bytes[offset + i].toInt() != 0
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", if (v) 1.0 else 0.0))
                                            }
                                            offset += valueLength
                                        }
                                        "int[]", "int64[]" -> {
                                            /**
                                             * count val.
                                             */
                                            val count = valueLength / 8
                                            for (i in 0 until count) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = buffer.getLong(offset)
                                                offset += 8
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v.toDouble()))
                                            }
                                        }
                                        "float[]" -> {
                                            /**
                                             * count val.
                                             */
                                            val count = valueLength / 4
                                            for (i in 0 until count) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = buffer.getFloat(offset)
                                                offset += 4
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v.toDouble()))
                                            }
                                        }
                                        "double[]" -> {
                                            /**
                                             * count val.
                                             */
                                            val count = valueLength / 8
                                            for (i in 0 until count) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = buffer.getDouble(offset)
                                                offset += 8
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v))
                                            }
                                        }
                                        else -> {
                                            // Skip other complex types
                                            offset = startValOffset + valueLength
                                        }
                                    }
                                }
                                logRevision == 1 -> {
                                    /**
                                     * valueType val.
                                     */
                                    val valueType = bytes[offset].toInt() and 0xFF
                                    offset += 1
                                    when (valueType) {
                                        0 -> { // null -> default 0.0
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, 0.0))
                                        }
                                        1 -> { // Boolean
                                            /**
                                             * v val.
                                             */
                                            val v = bytes[offset].toInt() != 0
                                            offset += 1
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, if (v) 1.0 else 0.0))
                                        }
                                        9 -> { // Byte
                                            /**
                                             * v val.
                                             */
                                            val v = bytes[offset].toInt() and 0xFF
                                            offset += 1
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v.toDouble()))
                                        }
                                        3 -> { // Integer
                                            /**
                                             * v val.
                                             */
                                            val v = buffer.getInt(offset)
                                            offset += 4
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v.toDouble()))
                                        }
                                        5 -> { // Double
                                            /**
                                             * v val.
                                             */
                                            val v = buffer.getDouble(offset)
                                            offset += 8
                                            batcher.add(TelemetryFrame(timestampMs, sessionId, keyName, v))
                                        }
                                        7 -> { // String
                                            /**
                                             * strLen val.
                                             */
                                            val strLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            offset += strLen // skip string bytes
                                        }
                                        2 -> { // BooleanArray
                                            /**
                                             * arrLen val.
                                             */
                                            val arrLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            for (i in 0 until arrLen) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = bytes[offset].toInt() != 0
                                                offset += 1
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", if (v) 1.0 else 0.0))
                                            }
                                        }
                                        10 -> { // ByteArray
                                            /**
                                             * arrLen val.
                                             */
                                            val arrLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            for (i in 0 until arrLen) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = bytes[offset].toInt() and 0xFF
                                                offset += 1
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v.toDouble()))
                                            }
                                        }
                                        4 -> { // IntegerArray
                                            /**
                                             * arrLen val.
                                             */
                                            val arrLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            for (i in 0 until arrLen) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = buffer.getInt(offset)
                                                offset += 4
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v.toDouble()))
                                            }
                                        }
                                        6 -> { // DoubleArray
                                            /**
                                             * arrLen val.
                                             */
                                            val arrLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            for (i in 0 until arrLen) {
                                                /**
                                                 * v val.
                                                 */
                                                val v = buffer.getDouble(offset)
                                                offset += 8
                                                batcher.add(TelemetryFrame(timestampMs, sessionId, "$keyName[$i]", v))
                                            }
                                        }
                                        8 -> { // StringArray
                                            /**
                                             * arrLen val.
                                             */
                                            val arrLen = buffer.getShort(offset).toInt() and 0xFFFF
                                            offset += 2
                                            for (i in 0 until arrLen) {
                                                /**
                                                 * strLen val.
                                                 */
                                                val strLen = buffer.getShort(offset).toInt() and 0xFFFF
                                                offset += 2
                                                offset += strLen
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle parsing/corrupted data skips
        }
    }
}
