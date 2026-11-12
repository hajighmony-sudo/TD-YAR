package com.tradenex.app.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** A device found on the local network. [tradix] = runs the TRADIX TV receiver; otherwise a Cast-capable TV/speaker. */
data class FoundDevice(val name: String, val host: String, val port: Int, val tradix: Boolean)

/**
 * mDNS device search (like the YouTube "Cast" picker). Finds Google Cast / Android TV devices (_googlecast._tcp)
 * and a future TRADIX TV app (_tradix-tv._tcp). Also advertises this phone as _tradix-tv-host._tcp while sharing.
 */
object TvDiscovery {
    val devices = mutableStateListOf<FoundDevice>()
    var searching by mutableStateOf(false); private set

    private const val TYPE_CAST = "_googlecast._tcp."
    private const val TYPE_TRADIX = "_tradix-tv._tcp."
    private const val TYPE_HOST = "_tradix-tv-host._tcp."
    private var nsd: NsdManager? = null
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var registration: NsdManager.RegistrationListener? = null

    fun start(context: Context) {
        if (searching) return
        val m = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = m
        devices.clear()
        searching = true
        for (type in listOf(TYPE_CAST, TYPE_TRADIX)) {
            val tradix = type == TYPE_TRADIX
            val l = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { searching = false }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onServiceLost(info: NsdServiceInfo) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { devices.removeAll { it.name == info.serviceName && it.tradix == tradix } }
                }
                @Suppress("DEPRECATION")
                override fun onServiceFound(info: NsdServiceInfo) {
                    runCatching {
                        m.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(i: NsdServiceInfo, errorCode: Int) {}
                            override fun onServiceResolved(i: NsdServiceInfo) {
                                val host = i.host?.hostAddress ?: return
                                val friendly = if (tradix) i.serviceName else i.serviceName.substringBeforeLast('-').ifBlank { i.serviceName }
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    if (devices.none { it.host == host && it.tradix == tradix }) devices.add(FoundDevice(friendly, host, i.port, tradix))
                                }
                            }
                        })
                    }
                }
            }
            runCatching { m.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l); listeners += l }
        }
    }

    fun stop() {
        val m = nsd
        if (m != null) listeners.forEach { runCatching { m.stopServiceDiscovery(it) } }
        listeners.clear()
        searching = false
    }

    /** Advertise this phone so a TRADIX TV app can find it without typing an address. */
    fun advertise(context: Context, port: Int, name: String) {
        val m = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = m
        unadvertise()
        val info = NsdServiceInfo().apply { serviceName = name; serviceType = TYPE_HOST; setPort(port) }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        registration = l
        runCatching { m.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun unadvertise() {
        registration?.let { r -> runCatching { nsd?.unregisterService(r) } }
        registration = null
    }
}
