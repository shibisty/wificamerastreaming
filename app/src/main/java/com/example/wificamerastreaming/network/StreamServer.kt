package com.example.wificamerastreaming.network

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.CIO
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.time.Duration

class StreamServer(
    private val port: Int,
    private val frameFlow: Flow<ByteArray>,
    private val onCaptureRequested: suspend () -> ByteArray
) {
    private var engine: ApplicationEngine? = null

    fun start() {
        engine = embeddedServer(CIO, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
            }
            routing {
                post("/capture") {
                    val jpegBytes = onCaptureRequested()
                    call.respondBytes(jpegBytes, contentType = io.ktor.http.ContentType.Image.JPEG)
                }
                webSocket("/stream") {
                    try {
                        frameFlow.collect { jpegBytes ->
                            send(Frame.Binary(true, jpegBytes))
                        }
                    } catch (e: Exception) {
                        // клиент отключился — просто выходим
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(1000, 2000)
    }
}
