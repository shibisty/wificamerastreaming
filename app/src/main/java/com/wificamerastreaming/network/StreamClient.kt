package com.wificamerastreaming.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StreamClient {
    private val client = HttpClient {
        install(WebSockets)
    }

    fun connectToStream(host: String, port: Int): Flow<ByteArray> = flow {
        client.webSocket(host = host, port = port, path = "/stream") {
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Binary) {
                    emit(frame.readBytes())
                }
            }
        }
    }

    fun close() = client.close()
}
