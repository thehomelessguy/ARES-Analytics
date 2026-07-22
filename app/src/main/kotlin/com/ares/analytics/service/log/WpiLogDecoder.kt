package com.ares.analytics.service.log

import com.ares.analytics.service.FrameBatcher
import com.ares.analytics.shared.TelemetryFrame
import java.io.File
import java.io.FileInputStream
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
class WpiLogDecoder {

    suspend fun parseWpiLog(file: File, sessionId: String, batcher: FrameBatcher) {
        FileInputStream(file).use { fis ->
            /**
             * headerBytes val.
             */
            val headerBytes = ByteArray(12)
            if (fis.read(headerBytes) != 12) return
            /**
             * magic val.
             */
            val magic = String(headerBytes, 0, 6)
            if (magic != "WPILOG") return

            // Map from entryId to (name, type)
            /**
             * entryMap val.
             */
            val entryMap = mutableMapOf<Int, Pair<String, String>>()

            /**
             * fileLength val.
             */
            val fileLength = file.length()
            /**
             * bytesRead var.
             */
            var bytesRead = 12L

            while (bytesRead < fileLength) {
                /**
                 * headerByte val.
                 */
                val headerByte = fis.read()
                if (headerByte == -1) break
                bytesRead++

                /**
                 * entryIdSize val.
                 */
                val entryIdSize = (headerByte and 0x03) + 1
                /**
                 * payloadSizeSize val.
                 */
                val payloadSizeSize = ((headerByte ushr 2) and 0x03) + 1
                /**
                 * timestampSize val.
                 */
                val timestampSize = ((headerByte ushr 4) and 0x03) + 1

                /**
                 * recordHeaderSize val.
                 */
                val recordHeaderSize = entryIdSize + payloadSizeSize + timestampSize
                /**
                 * recordHeaderBytes val.
                 */
                val recordHeaderBytes = ByteArray(recordHeaderSize)
                if (fis.read(recordHeaderBytes) != recordHeaderSize) break
                bytesRead += recordHeaderSize

                /**
                 * buffer val.
                 */
                val buffer = ByteBuffer.wrap(recordHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)

                /**
                 * entryId val.
                 */
                val entryId = readVariableInt(buffer, entryIdSize)
                /**
                 * payloadSize val.
                 */
                val payloadSize = readVariableInt(buffer, payloadSizeSize)
                /**
                 * timestampMicro val.
                 */
                val timestampMicro = readVariableLong(buffer, timestampSize)
                /**
                 * timestampMs val.
                 */
                val timestampMs = timestampMicro / 1000

                /**
                 * payload val.
                 */
                val payload = ByteArray(payloadSize)
                if (fis.read(payload) != payloadSize) break
                bytesRead += payloadSize

                if (entryId == 0) {
                    // Control Record
                    if (payload.isNotEmpty()) {
                        /**
                         * controlType val.
                         */
                        val controlType = payload[0].toInt()
                        /**
                         * controlBuffer val.
                         */
                        val controlBuffer = ByteBuffer.wrap(payload, 1, payload.size - 1).order(ByteOrder.LITTLE_ENDIAN)
                        when (controlType) {
                            0 -> { // Start
                                if (controlBuffer.remaining() >= 4) {
                                    /**
                                     * newEntryId val.
                                     */
                                    val newEntryId = controlBuffer.int
                                    /**
                                     * name val.
                                     */
                                    val name = readNullTerminatedString(controlBuffer)
                                    /**
                                     * type val.
                                     */
                                    val type = readNullTerminatedString(controlBuffer)
                                    entryMap[newEntryId] = Pair(name, type)
                                }
                            }
                            1 -> { // Finish
                                if (controlBuffer.remaining() >= 4) {
                                    /**
                                     * finishEntryId val.
                                     */
                                    val finishEntryId = controlBuffer.int
                                    entryMap.remove(finishEntryId)
                                }
                            }
                        }
                    }
                } else {
                    // Data Record
                    /**
                     * entry val.
                     */
                    val entry = entryMap[entryId] ?: continue
                    /**
                     * name val.
                     */
                    val name = entry.first
                    /**
                     * type val.
                     */
                    val type = entry.second

                    /**
                     * valBuffer val.
                     */
                    val valBuffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                    when (type) {
                        "double" -> {
                            if (payload.size == 8) {
                                batcher.add(TelemetryFrame(timestampMs, sessionId, name, valBuffer.double))
                            }
                        }
                        "float" -> {
                            if (payload.size == 4) {
                                batcher.add(TelemetryFrame(timestampMs, sessionId, name, valBuffer.float.toDouble()))
                            }
                        }
                        "int64" -> {
                            if (payload.size == 8) {
                                batcher.add(TelemetryFrame(timestampMs, sessionId, name, valBuffer.long.toDouble()))
                            }
                        }
                        "int32" -> {
                            if (payload.size == 4) {
                                batcher.add(TelemetryFrame(timestampMs, sessionId, name, valBuffer.int.toDouble()))
                            }
                        }
                        "boolean" -> {
                            if (payload.isNotEmpty()) {
                                batcher.add(TelemetryFrame(timestampMs, sessionId, name, if (payload[0].toInt() != 0) 1.0 else 0.0))
                            }
                        }
                        "double[]" -> {
                            /**
                             * count val.
                             */
                            val count = payload.size / 8
                            for (i in 0 until count) {
                                if (valBuffer.remaining() >= 8) {
                                    batcher.add(TelemetryFrame(timestampMs, sessionId, "$name[$i]", valBuffer.double))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readVariableInt(buffer: ByteBuffer, size: Int): Int {
        /**
         * value var.
         */
        var value = 0
        for (i in 0 until size) {
            value = value or ((buffer.get().toInt() and 0xFF) shl (i * 8))
        }
        return value
    }

    private fun readVariableLong(buffer: ByteBuffer, size: Int): Long {
        /**
         * value var.
         */
        var value = 0L
        for (i in 0 until size) {
            value = value or ((buffer.get().toLong() and 0xFFL) shl (i * 8))
        }
        return value
    }

    private fun readNullTerminatedString(buffer: ByteBuffer): String {
        /**
         * bytes val.
         */
        val bytes = mutableListOf<Byte>()
        while (buffer.hasRemaining()) {
            /**
             * b val.
             */
            val b = buffer.get()
            if (b == 0.toByte()) break
            bytes.add(b)
        }
        return String(bytes.toByteArray())
    }
}
