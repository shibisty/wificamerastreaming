package com.example.wificamerastreaming.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class CaptureClient {
    private val client = HttpClient(CIO)

    suspend fun requestCapture(host: String, port: Int): ByteArray {
        val response: HttpResponse = client.post("http://$host:$port/capture")
        return response.readBytes()
    }

    fun close() = client.close()
}
