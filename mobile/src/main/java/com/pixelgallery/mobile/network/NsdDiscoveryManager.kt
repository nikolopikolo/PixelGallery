package com.pixelgallery.mobile.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.pixelgallery.shared.model.WatchDeviceInfo
import com.pixelgallery.shared.protocol.TransferConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class NsdDiscoveryManager(context: Context) {
    private val tag = "NsdDiscoveryManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredDevices = MutableStateFlow<List<WatchDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<WatchDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (_isDiscovering.value || nsdManager == null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(tag, "NSD Service discovery started: $regType")
                _isDiscovering.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                if (service.serviceType.contains("pixelgallery", ignoreCase = true)) {
                    resolveService(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(tag, "Service lost: ${service.serviceName}")
                _discoveredDevices.value = _discoveredDevices.value.filter { it.deviceName != service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "Discovery stopped: $serviceType")
                _isDiscovering.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Stop discovery failed: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(
                TransferConstants.NSD_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start NSD discovery", e)
            _isDiscovering.value = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host: InetAddress? = resolvedInfo.host
                val port: Int = resolvedInfo.port
                val deviceName = resolvedInfo.serviceName

                Log.d(tag, "Resolved service: $deviceName at $host:$port")

                if (host != null) {
                    val hostAddress = host.hostAddress ?: return
                    val newDevice = WatchDeviceInfo(
                        deviceName = deviceName,
                        host = hostAddress,
                        port = port
                    )

                    val currentList = _discoveredDevices.value.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.host == hostAddress && it.port == port }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = newDevice
                    } else {
                        currentList.add(newDevice)
                    }
                    _discoveredDevices.value = currentList
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve service", e)
        }
    }

    fun stopDiscovery() {
        if (!_isDiscovering.value) return
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping discovery", e)
        } finally {
            _isDiscovering.value = false
            discoveryListener = null
        }
    }
}
