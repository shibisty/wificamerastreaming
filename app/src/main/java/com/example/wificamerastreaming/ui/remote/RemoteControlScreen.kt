package com.example.wificamerastreaming.ui.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.wificamerastreaming.discovery.RemoteDevice
import com.example.wificamerastreaming.network.CaptureClient
import com.example.wificamerastreaming.network.StreamClient
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.wificamerastreaming.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(device: RemoteDevice, onBack: () -> Unit) {
    val captureClient = remember { CaptureClient() }
    val streamClient = remember { StreamClient() }
    val scope = rememberCoroutineScope()

    var liveFrame by remember { mutableStateOf<ByteArray?>(null) }
    var lastPhoto by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(device) {
        streamClient.connectToStream(device.host, device.port).collect { jpegBytes ->
            liveFrame = jpegBytes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
                onClick = {
                    scope.launch {
                        lastPhoto = captureClient.requestCapture(device.host, device.port)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.button_take_photo_remote))
            }
        }
    }
}
