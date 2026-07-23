package com.ares.analytics.service

import com.areslib.networktables.NT4WireProtocol
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertTrue

/**
 * Connects to a LIVE running NT4Server on 127.0.0.1:5810
 * and monitors what frames arrive over 5 seconds.
 * Run this while the sim is running to diagnose real-world connectivity.
 */
class Nt4LiveWireDiagnosticTest {

    @Test
    fun testLiveWireConnectivity() = runBlocking {
        val client = HttpClient(OkHttp) { install(WebSockets) }
        val topicMap = ConcurrentHashMap<Int, String>()
        val receivedAnnounces = mutableListOf<String>()
        val receivedBinaryTopics = mutableListOf<String>()
        var textFrameCount = 0
        var binaryFrameCount = 0
        var announceCount = 0

        println("=== LIVE WIRE DIAGNOSTIC: Connecting to ws://127.0.0.1:5810 ===")

        val result = withTimeoutOrNull(10_000) {
            client.webSocket(
                method = io.ktor.http.HttpMethod.Get,
                host = "127.0.0.1",
                port = 5810,
                path = "/nt/ARES-Diagnostic-${System.currentTimeMillis()}",
                request = {
                    headers.append("Sec-WebSocket-Protocol", "v4.1.networktables.first.wpi.edu")
                }
            ) {
                println("[DIAG] WebSocket connected!")

                // Send subscribe for all topics
                val subMsg = """
                    [{"method": "subscribe", "params": {"topics": [""], "subuid": 1, "options": {"prefix": true}}}]
                """.trimIndent()
                send(Frame.Text(subMsg))
                println("[DIAG] Sent subscribe message")

                // Read frames for 5 seconds
                val readJob = launch {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                textFrameCount++
                                val text = frame.readText()
                                try {
                                    val jsonArray = Json.parseToJsonElement(text).jsonArray
                                    for (element in jsonArray) {
                                        val obj = element.jsonObject
                                        val method = obj["method"]?.jsonPrimitive?.content
                                        if (method == "announce") {
                                            val params = obj["params"]?.jsonObject
                                            val name = params?.get("name")?.jsonPrimitive?.content ?: "?"
                                            val id = params?.get("id")?.jsonPrimitive?.intOrNull ?: -1
                                            val type = params?.get("type")?.jsonPrimitive?.content ?: "?"
                                            topicMap[id] = name
                                            receivedAnnounces.add("$name (id=$id, type=$type)")
                                            announceCount++
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("[DIAG] Text frame parse error: ${e.message}")
                                    println("[DIAG] Raw text (first 200 chars): ${text.take(200)}")
                                }
                            }
                            is Frame.Binary -> {
                                binaryFrameCount++
                                val bytes = frame.readBytes()
                                try {
                                    val messages = NT4WireProtocol.unpackMessageFrames(bytes)
                                    for (msg in messages) {
                                        val topicName = topicMap[msg.topicId.toInt()] ?: "UNKNOWN(id=${msg.topicId})"
                                        receivedBinaryTopics.add(topicName)
                                    }
                                } catch (e: Exception) {
                                    println("[DIAG] Binary frame decode error: ${e.message}, bytes=${bytes.size}")
                                }
                            }
                            else -> {}
                        }
                    }
                }

                delay(5000)
                readJob.cancel()

                println("\n=== DIAGNOSTIC RESULTS ===")
                println("Text frames received: $textFrameCount")
                println("Binary frames received: $binaryFrameCount")
                println("Announces received: $announceCount")
                println("\n--- Announced Topics ($announceCount) ---")
                receivedAnnounces.forEach { println("  $it") }
                println("\n--- Binary Updates (unique topics) ---")
                val uniqueTopics = receivedBinaryTopics.groupBy { it }.mapValues { it.value.size }
                uniqueTopics.forEach { (topic, count) -> println("  $topic: $count updates") }
                println("\nTotal binary topic updates: ${receivedBinaryTopics.size}")
            }
            "OK"
        }

        if (result == null) {
            println("=== DIAGNOSTIC FAILED: Could not connect to NT4Server within 10 seconds ===")
            println("Is the simulator running? Check: netstat -ano | findstr 5810")
        }

        assertTrue(announceCount > 0, "Expected at least 1 announce from the server")
        assertTrue(binaryFrameCount > 0, "Expected at least 1 binary frame in 5 seconds")
        println("\n=== DIAGNOSTIC PASSED ===")
        client.close()
    }
}
