package com.wificamerastreaming.ui.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.wificamerastreaming.discovery.RemoteDevice
import com.wificamerastreaming.network.CaptureClient
import com.wificamerastreaming.network.StreamClient
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.wificamerastreaming.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(device: RemoteDevice, onBack: () -> Unit) {
    val captureClient = remember { CaptureClient() }
    val streamClient = remember { StreamClient() }
    val scope = rememberCoroutineScope()

    var liveFrame by remember { mutableStateOf<ByteArray?>(null) }
    var lastPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(device) {
        streamClient.connectToStream(device.host, device.port).collect { jpegBytes ->
            liveFrame = jpegBytes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device.name)
                        // Показываем адрес — чтобы явно видеть, к какому устройству подключены
                        Text(
                            "${device.host}:${device.port}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // lastPhoto — это ТОЛЬКО байты для показа превью, никакого MediaStore-сохранения здесь нет
                val bytesToShow = lastPhoto ?: liveFrame
                bytesToShow?.let { bytes ->
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        Image(
                            bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Button(
                enabled = !isCapturing,
                onClick = {
                    isCapturing = true
                    scope.launch {
                        try {
                            // Единственное сетевое действие — POST на удалённое устройство.
                            // Съёмка и сохранение в галерею происходят ТАМ, не здесь.
                            lastPhoto = captureClient.requestCapture(device.host, device.port)
                        } finally {
                            isCapturing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    if (isCapturing) stringResource(R.string.button_capturing)
                    else stringResource(R.string.button_take_photo_remote)
                )
            }
        }
    }
}
