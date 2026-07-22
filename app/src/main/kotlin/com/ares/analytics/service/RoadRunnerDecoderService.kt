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
class RoadRunnerDecoderService {

    sealed interface RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object IntSchema : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object LongSchema : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object DoubleSchema : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object StringSchema : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object BooleanSchema : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    class EnumSchema(val constants: List<String>) : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    class ArraySchema(val elementSchema: RRSchema) : RRSchema
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    class StructSchema(val fields: List<Pair<String, RRSchema>>) : RRSchema

    suspend fun parseRoadRunnerLog(
        file: File,
        sessionId: String,
        batcher: FrameBatcher
    ) {
        /**
         * bytes val.
         */
        val bytes = file.readBytes()
        /**
         * buffer val.
         */
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        
        /**
         * offset var.
         */
        var offset = 0
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        fun readString(): String {
            /**
             * len val.
             */
            val len = buffer.getInt(offset)
            offset += 4
            /**
             * str val.
             */
            val str = String(bytes, offset, len, Charsets.UTF_8)
            offset += len
            return str
        }

        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        fun readSchema(): RRSchema {
            /**
             * schemaType val.
             */
            val schemaType = buffer.getInt(offset)
            offset += 4
            return when (schemaType) {
                0 -> {
                    /**
                     * numFields val.
                     */
                    val numFields = buffer.getInt(offset)
                    offset += 4
                    /**
                     * fields val.
                     */
                    val fields = mutableListOf<Pair<String, RRSchema>>()
                    for (i in 0 until numFields) {
                        fields.add(Pair(readString(), readSchema()))
                    }
                    StructSchema(fields)
                }
                1 -> IntSchema
                2 -> LongSchema
                3 -> DoubleSchema
                4 -> StringSchema
                5 -> BooleanSchema
                6 -> {
                    /**
                     * numConstants val.
                     */
                    val numConstants = buffer.getInt(offset)
                    offset += 4
                    /**
                     * constants val.
                     */
                    val constants = mutableListOf<String>()
                    for (i in 0 until numConstants) {
                        constants.add(readString())
                    }
                    EnumSchema(constants)
                }
                7 -> ArraySchema(readSchema())
                else -> throw IllegalArgumentException("Unknown schema type: $schemaType")
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
        fun arraySchemaCount(schema: RRSchema): Int {
            return when (schema) {
                is StructSchema -> {
                    /**
                     * count var.
                     */
                    var count = 0
                    for (field in schema.fields) {
                        count += arraySchemaCount(field.second)
                    }
                    count
                }
                is ArraySchema -> 1 + arraySchemaCount(schema.elementSchema)
                else -> 0
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
        fun readMsg(schema: RRSchema): Any {
            return when (schema) {
                is StructSchema -> {
                    /**
                     * map val.
                     */
                    val map = mutableMapOf<String, Any>()
                    for (field in schema.fields) {
                        map[field.first] = readMsg(field.second)
                    }
                    map
                }
                is IntSchema -> {
                    /**
                     * v val.
                     */
                    val v = buffer.getInt(offset)
                    offset += 4
                    v
                }
                is LongSchema -> {
                    /**
                     * v val.
                     */
                    val v = buffer.getLong(offset)
                    offset += 8
                    v
                }
                is DoubleSchema -> {
                    /**
                     * v val.
                     */
                    val v = buffer.getDouble(offset)
                    offset += 8
                    v
                }
                is StringSchema -> readString()
                is BooleanSchema -> {
                    /**
                     * v val.
                     */
                    val v = bytes[offset].toInt() != 0
                    offset += 1
                    v
                }
                is EnumSchema -> {
                    /**
                     * ordinal val.
                     */
                    val ordinal = buffer.getInt(offset)
                    offset += 4
                    if (ordinal >= 0 && ordinal < schema.constants.size) {
                        schema.constants[ordinal]
                    } else {
                        "UNKNOWN"
                    }
                }
                is ArraySchema -> {
                    /**
                     * size val.
                     */
                    val size = buffer.getInt(offset)
                    offset += 4
                    /**
                     * list val.
                     */
                    val list = mutableListOf<Any>()
                    for (i in 0 until size) {
                        list.add(readMsg(schema.elementSchema))
                    }
                    list
                }
            }
        }

        // Check Magic
        if (bytes.size < 4) return
        /**
         * magic val.
         */
        val magic = String(bytes, offset, 2, Charsets.UTF_8)
        offset += 2
        if (magic != "RR") return

        /**
         * logRevision var.
         */
        var logRevision = buffer.getShort(offset).toInt()
        offset += 2
        if (logRevision != 0 && logRevision != 1) return

        /**
         * keyIDs val.
         */
        val keyIDs = mutableMapOf<Int, String>()
        /**
         * keySchemas val.
         */
        val keySchemas = mutableMapOf<Int, RRSchema>()
        /**
         * firstRRTimestamp var.
         */
        var firstRRTimestamp: Long? = null
        /**
         * lastTimestampMs var.
         */
        var lastTimestampMs = 0L

        suspend fun flatten(prefix: String, value: Any, timestampMs: Long) {
            when (value) {
                is Boolean -> {
                    batcher.add(TelemetryFrame(timestampMs, sessionId, prefix, if (value) 1.0 else 0.0))
                }
                is Number -> {
                    batcher.add(TelemetryFrame(timestampMs, sessionId, prefix, value.toDouble()))
                }
                is Map<*, *> -> {
                    /**
                     * x val.
                     */
                    val x = value["x"] as? Number
                    /**
                     * y val.
                     */
                    val y = value["y"] as? Number
                    /**
                     * heading val.
                     */
                    val heading = value["heading"] as? Number
                    if (x != null && y != null && heading != null) {
                        // Pose2d: convert inches to meters
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "$prefix/x", x.toDouble() * 0.0254))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "$prefix/y", y.toDouble() * 0.0254))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "$prefix/heading", heading.toDouble()))
                    } else {
                        for ((k, v) in value) {
                            if (k is String && v != null) {
                                flatten("$prefix/$k", v, timestampMs)
                            }
                        }
                    }
                }
                is List<*> -> {
                    for (i in value.indices) {
                        /**
                         * v val.
                         */
                        val v = value[i]
                        if (v != null) {
                            flatten("$prefix[$i]", v, timestampMs)
                        }
                    }
                }
            }
        }

        try {
            while (offset < bytes.size) {
                // Check for concatenated logs
                if (offset + 2 <= bytes.size && String(bytes, offset, 2, Charsets.UTF_8) == "RR") {
                    offset += 2
                    logRevision = buffer.getShort(offset).toInt()
                    offset += 2
                    keyIDs.clear()
                    keySchemas.clear()
                    continue
                }

                if (offset + 4 > bytes.size) break
                /**
                 * type val.
                 */
                val type = buffer.getInt(offset)
                offset += 4

                when (type) {
                    0 -> {
                        /**
                         * keyID val.
                         */
                        val keyID = keyIDs.size
                        /**
                         * keyName val.
                         */
                        val keyName = readString()
                        /**
                         * schema val.
                         */
                        val schema = readSchema()
                        keyIDs[keyID] = keyName
                        keySchemas[keyID] = schema
                        if (logRevision == 0) {
                            offset += 4 * arraySchemaCount(schema)
                        }
                    }
                    1 -> {
                        /**
                         * keyID val.
                         */
                        val keyID = buffer.getInt(offset)
                        offset += 4
                        /**
                         * key val.
                         */
                        val key = keyIDs[keyID] ?: continue
                        /**
                         * schema val.
                         */
                        val schema = keySchemas[keyID] ?: continue
                        /**
                         * msg val.
                         */
                        val msg = readMsg(schema)

                        // Update timestamp if timestamp is present in log
                        when {
                            (key == "OPMODE_PRE_INIT" || key == "OPMODE_PRE_START" || key == "OPMODE_POST_STOP" || key == "TIMESTAMP") && msg is Long -> {
                                if (firstRRTimestamp == null) {
                                    firstRRTimestamp = msg
                                }
                                lastTimestampMs = (msg - firstRRTimestamp) / 1_000_000
                            }
                            msg is Map<*, *> -> {
                                /**
                                 * ts val.
                                 */
                                val ts = msg["timestamp"] as? Long
                                if (ts != null) {
                                    if (firstRRTimestamp == null) {
                                        firstRRTimestamp = ts
                                    }
                                    lastTimestampMs = (ts - firstRRTimestamp) / 1_000_000
                                }
                            }
                        }

                        flatten(key, msg, lastTimestampMs)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle unexpected EOF or parsing errors gracefully
        }
    }
}
