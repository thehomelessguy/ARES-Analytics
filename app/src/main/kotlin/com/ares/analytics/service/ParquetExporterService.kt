package com.ares.analytics.service

import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.File

class ParquetExporterService(private val databaseService: DatabaseService) {

    private val schemaString = """
        {
          "type": "record",
          "name": "TelemetryFrame",
          "namespace": "com.ares.analytics",
          "fields": [
            {"name": "timestamp_ms", "type": "long"},
            {"name": "session_id", "type": "string"},
            {"name": "key", "type": "string"},
            {"name": "value", "type": "double"}
          ]
        }
    """.trimIndent()

    private val avroSchema = Schema.Parser().parse(schemaString)

    suspend fun exportSessionToParquet(sessionId: String, destinationFile: File) = withContext(Dispatchers.IO) {
        val frames = databaseService.getTelemetryRange(sessionId, 0L, Long.MAX_VALUE)
        if (frames.isEmpty()) {
            throw IllegalArgumentException("Cannot export empty session: $sessionId")
        }

        // Ensure parent folder exists
        destinationFile.parentFile?.mkdirs()

        // Hadoop configuration to bypass winutils.exe requirement on Windows
        val conf = Configuration()
        conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem")

        val hadoopPath = Path(destinationFile.absolutePath)

        val writer = AvroParquetWriter.builder<GenericRecord>(hadoopPath)
            .withSchema(avroSchema)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
            .withConf(conf)
            .build()

        try {
            for (frame in frames) {
                val record = GenericData.Record(avroSchema).apply {
                    put("timestamp_ms", frame.timestampMs)
                    put("session_id", frame.sessionId)
                    put("key", frame.key)
                    put("value", frame.value)
                }
                writer.write(record)
            }
        } finally {
            writer.close()
        }
    }
}
