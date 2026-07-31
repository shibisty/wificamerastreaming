package com.wificamerastreaming.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wificamerastreaming.R
import com.wificamerastreaming.discovery.NsdHelper
import com.wificamerastreaming.discovery.RemoteDevice
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onDeviceSelected: (RemoteDevice) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nsdHelper = remember { NsdHelper(context) }
    var devices by remember { mutableStateOf<List<RemoteDevice>>(emptyList()) }

    LaunchedEffect(Unit) {
        nsdHelper.discoverDevices().collect { devices = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (devices.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.devices_not_found),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(devices) { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text("${device.host}:${device.port}") },
                        modifier = Modifier.clickable { onDeviceSelected(device) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
