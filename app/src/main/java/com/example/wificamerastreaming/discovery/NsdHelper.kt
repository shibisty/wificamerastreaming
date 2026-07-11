package com.example.wificamerastreaming.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

private const val SERVICE_TYPE = "_camapp._tcp."
private const val TAG = "NsdHelper"

data class RemoteDevice(
    val name: String,
    val host: String,
    val port: Int
)

class NsdHelper(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val myServiceName = "CamApp-${DeviceIdProvider.getDeviceId(context)}"

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = myServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Registered: ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService() {
        registrationListener?.let { nsdManager.unregisterService(it) }
    }

    fun discoverDevices() = callbackFlow<List<RemoteDevice>> {
        val found = mutableMapOf<String, RemoteDevice>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                // Пропускаем самих себя ещё до resolve — экономим ресурсы
                if (service.serviceName == myServiceName) {
                    Log.d(TAG, "Skipping own service: ${service.serviceName}")
                    return
                }

                if (service.serviceType.contains("_camapp")) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            // Двойная проверка на случай, если имя после resolve отличается
                            if (info.serviceName == myServiceName) return

                            val device = RemoteDevice(
                                name = info.serviceName,
                                host = info.host.hostAddress ?: return,
                                port = info.port
                            )
                            found[device.name] = device
                            trySend(found.values.toList())
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                found.remove(service.serviceName)
                trySend(found.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose { nsdManager.stopServiceDiscovery(discoveryListener) }
    }
}
