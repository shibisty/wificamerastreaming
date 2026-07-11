package com.example.wificamerastreaming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.wificamerastreaming.camera.CameraStreamer
import com.example.wificamerastreaming.discovery.NsdHelper
import com.example.wificamerastreaming.discovery.RemoteDevice
import com.example.wificamerastreaming.network.StreamServer
import com.example.wificamerastreaming.ui.camera.CameraScreen
import com.example.wificamerastreaming.ui.devices.DeviceListScreen
import com.example.wificamerastreaming.ui.remote.RemoteControlScreen
import com.example.wificamerastreaming.ui.theme.WiFiCameraStreamingTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val SERVER_PORT = 9865
    }
    private lateinit var server: StreamServer
    private lateinit var nsdHelper: NsdHelper
    private lateinit var cameraStreamer: CameraStreamer

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cameraGranted = results[Manifest.permission.CAMERA] ?: false
        val storageGranted = results[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: true

        if (!cameraGranted) {
            Toast.makeText(this, getString(R.string.permission_camera_denied), Toast.LENGTH_LONG).show()
        }
        if (!storageGranted) {
            Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        cameraStreamer = CameraStreamer(this)
        cameraStreamer.bindCamera(this)

        nsdHelper = NsdHelper(this)
        server = StreamServer(
            port = SERVER_PORT,
            frameFlow = cameraStreamer.frameJpegFlow,
            onCaptureRequested = { cameraStreamer.captureAndSaveToGallery() }
        )
        server.start()
        nsdHelper.registerService(SERVER_PORT)

        setContent {
            WiFiCameraStreamingTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "camera") {
                    composable("camera") {
                        CameraScreen(
                            streamer = cameraStreamer,
                            onOpenDevices = { navController.navigate("devices") }
                        )
                    }
                    composable("devices") {
                        DeviceListScreen(
                            onDeviceSelected = { device ->
                                navController.navigate("remote/${device.host}/${device.port}/${device.name}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "remote/{host}/{port}/{name}",
                        arguments = listOf(
                            navArgument("host") { type = NavType.StringType },
                            navArgument("port") { type = NavType.IntType },
                            navArgument("name") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val host = backStackEntry.arguments?.getString("host")!!
                        val port = backStackEntry.arguments?.getInt("port")!!
                        val name = backStackEntry.arguments?.getString("name")!!
                        RemoteControlScreen(
                            device = RemoteDevice(name, host, port),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        nsdHelper.unregisterService()
    }
}
