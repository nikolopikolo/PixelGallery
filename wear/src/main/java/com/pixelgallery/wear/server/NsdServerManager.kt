package com.pixelgallery.wear.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.pixelgallery.shared.protocol.TransferConstants

class NsdServerManager(private val context: Context) {
    private val tag = "NsdServerManager"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    fun registerService(port: Int) {
        if (isRegistered || nsdManager == null) return

        try {
            // Acquire MulticastLock to enable mDNS on Wear OS
            multicastLock = wifiManager?.createMulticastLock("PixelGalleryMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val serviceInfo = NsdServiceInfo().apply {
                serviceName = TransferConstants.NSD_SERVICE_NAME
                serviceType = TransferConstants.NSD_SERVICE_TYPE
                setPort(port)
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                    Log.d(tag, "Service registered: ${NsdServiceInfo.serviceName}")
                    isRegistered = true
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(tag, "Service registration failed: $errorCode")
                    isRegistered = false
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Log.d(tag, "Service unregistered")
                    isRegistered = false
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(tag, "Service unregistration failed: $errorCode")
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to register NSD service", e)
        }
    }

    fun unregisterService() {
        if (!isRegistered) return
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering service", e)
        } finally {
            isRegistered = false
            registrationListener = null
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (_: Exception) {}
        }
    }
}
