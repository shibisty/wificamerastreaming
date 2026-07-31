package com.wificamerastreaming.network

import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.cio.CIO
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.ContentType

class CaptureServer(
    private val port: Int,
    private val onCaptureRequested: suspend () -> ByteArray
) {
    private var engine: ApplicationEngine? = null

    fun start() {
        engine = embeddedServer(CIO, port = port) {
            routing {
                post("/capture") {
                    val jpegBytes = onCaptureRequested()
                    call.respondBytes(jpegBytes, contentType = ContentType.Image.JPEG)
                }
                get("/ping") {
                    call.respondText("ok")
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(1000, 2000)
    }
}
