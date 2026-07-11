package com.example.wificamerastreaming.ui.camera

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wificamerastreaming.camera.CameraStreamer
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.example.wificamerastreaming.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(streamer: CameraStreamer, onOpenDevices: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val frame by streamer.latestFrame.collectAsState()
    var isCapturing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.patreon.com/cw/shibisty")
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_patreon),
                            contentDescription = stringResource(R.string.patreon_button_description),
                            tint = Color.Unspecified
                        )
                    }

                    IconButton(onClick = onOpenDevices) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = stringResource(R.string.devices_button_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            frame?.let { bmp ->
                Image(
                    bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Button(
                enabled = !isCapturing,
                onClick = {
                    isCapturing = true
                    scope.launch {
                        try {
                            streamer.captureAndSaveToGallery()
                        } finally {
                            isCapturing = false
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    if (isCapturing) stringResource(R.string.button_capturing)
                    else stringResource(R.string.button_take_photo)
                )
            }
        }
    }
}
