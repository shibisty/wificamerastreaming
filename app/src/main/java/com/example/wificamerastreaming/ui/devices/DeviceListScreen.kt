package com.example.wificamerastreaming.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.wificamerastreaming.discovery.NsdHelper
import com.example.wificamerastreaming.discovery.RemoteDevice
import androidx.compose.ui.res.stringResource
import com.example.wificamerastreaming.R

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
