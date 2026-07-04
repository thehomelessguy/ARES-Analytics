package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun CameraStreamCard(
    properties: Map<String, String>,
    onPropertiesChanged: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var streamUrl by remember(properties) { mutableStateOf(properties["streamUrl"] ?: "http://10.0.0.2:1181/stream.mjpg") }
    var isConfiguring by remember { mutableStateOf(properties["streamUrl"] == null) }
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    // MJPEG Streaming Coroutine
    LaunchedEffect(streamUrl) {
        if (isConfiguring || streamUrl.isBlank()) return@LaunchedEffect
        
        var retryDelayMs = 1000L
        while (isActive) {
            isConnected = false
            errorMessage = null

            val client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = Long.MAX_VALUE
                    socketTimeoutMillis = Long.MAX_VALUE
                }
            }

            try {
                client.prepareGet(streamUrl).execute { response ->
                    if (response.status.value in 200..299) {
                        isConnected = true
                        retryDelayMs = 1000L // reset on successful connection
                        val channel = response.bodyAsChannel()
                        val bos = ByteArrayOutputStream()
                        val readBuffer = ByteArray(8192)

                        while (isActive && !channel.isClosedForRead) {
                            val read = channel.readAvailable(readBuffer, 0, readBuffer.size)
                            if (read > 0) {
                                bos.write(readBuffer, 0, read)
                                val currentBytes = bos.toByteArray()
                                
                                // Find SOI and EOI
                                var soi = -1
                                var eoi = -1
                                for (i in 0 until currentBytes.size - 1) {
                                    if (currentBytes[i] == 0xFF.toByte() && currentBytes[i + 1] == 0xD8.toByte()) {
                                        soi = i
                                    }
                                    if (soi != -1 && currentBytes[i] == 0xFF.toByte() && currentBytes[i + 1] == 0xD9.toByte()) {
                                        eoi = i + 1
                                        break
                                    }
                                }

                                if (soi != -1 && eoi != -1 && eoi > soi) {
                                    val frameBytes = currentBytes.copyOfRange(soi, eoi + 1)
                                    val remainder = currentBytes.copyOfRange(eoi + 1, currentBytes.size)
                                    bos.reset()
                                    bos.write(remainder)

                                    try {
                                        val imageBitmap = org.jetbrains.skia.Image.makeFromEncoded(frameBytes).toComposeImageBitmap()
                                        withContext(Dispatchers.Main) {
                                            currentFrame = imageBitmap
                                        }
                                    } catch (e: Exception) {
                                        // Ignore bad frame decode
                                    }
                                } else if (currentBytes.size > 2_000_000) {
                                    // Safety: if no complete frame found within 2MB, drop buffer
                                    bos.reset()
                                }
                            }
                        }
                    } else {
                        errorMessage = "HTTP Error: ${response.status}"
                    }
                }
            } catch (e: Exception) {
                isConnected = false
                errorMessage = e.message ?: "Connection failed"
            } finally {
                client.close()
                isConnected = false
            }
            
            // Reconnect delay with exponential backoff
            if (isActive) {
                kotlinx.coroutines.delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 1.5).toLong().coerceAtMost(5000L)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AresSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AresBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AresSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Camera",
                        tint = if (isConnected) AresCyan else AresTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Camera Stream",
                        color = AresTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isConnected) {
                        Text("• LIVE", color = AresGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else if (errorMessage != null) {
                        Text("• ERROR", color = AresRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                IconButton(
                    onClick = { isConfiguring = !isConfiguring },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure", tint = AresTextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Divider(color = AresBorder, thickness = 1.dp)

            // Content
            Box(
                modifier = Modifier.fillMaxSize().background(AresBackground),
                contentAlignment = Alignment.Center
            ) {
                if (isConfiguring) {
                    // Config Panel
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("MJPEG Stream URL", color = AresTextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = streamUrl,
                            onValueChange = { streamUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AresCyan,
                                unfocusedBorderColor = AresBorder,
                                focusedContainerColor = AresSurfaceElevated,
                                unfocusedContainerColor = AresSurfaceElevated
                            ),
                            placeholder = { Text("http://10.0.0.2:1181/stream.mjpg", color = AresTextTertiary) }
                        )
                        Button(
                            onClick = {
                                isConfiguring = false
                                onPropertiesChanged(mapOf("streamUrl" to streamUrl))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Connect", color = AresBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (currentFrame != null) {
                    // Video Feed
                    Image(
                        bitmap = currentFrame!!,
                        contentDescription = "Live Video",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Loading or Error State
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (errorMessage != null) {
                            Icon(Icons.Default.VideocamOff, contentDescription = null, tint = AresRed, modifier = Modifier.size(32.dp))
                            Text(errorMessage!!, color = AresRed, fontSize = 12.sp)
                        } else {
                            CircularProgressIndicator(color = AresCyan, modifier = Modifier.size(32.dp))
                            Text("Connecting to stream...", color = AresTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
