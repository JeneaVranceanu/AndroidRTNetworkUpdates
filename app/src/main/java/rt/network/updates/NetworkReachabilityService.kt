package rt.network.updates

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.net.Inet6Address
import java.net.NetworkInterface

sealed class NetworkState {
    data class Available(val type: NetworkType) : NetworkState()
    object Unavailable : NetworkState()
    object Connecting : NetworkState()
    object Losing : NetworkState()
    object Lost : NetworkState()
}

sealed class NetworkType {
    object WiFi : NetworkType()
    object CELL : NetworkType()
    object OTHER : NetworkType()
}

class NetworkReachabilityService private constructor(context: Application) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // There are more functions to override!

        override fun onLost(network: Network) {
            super.onLost(network)
            postUpdate(NetworkState.Lost)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            postUpdate(NetworkState.Unavailable)
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            postUpdate(NetworkState.Losing)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateAvailability(connectivityManager.getNetworkCapabilities(network))
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateAvailability(networkCapabilities)
        }
    }

    companion object {
        // Subscribe to this subject to get updates on network changes
        val NETWORK_REACHABILITY: BehaviorSubject<NetworkState> =
            BehaviorSubject.createDefault(NetworkState.Unavailable)

        private var INSTANCE: NetworkReachabilityService? = null

        @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
        fun getService(context: Application): NetworkReachabilityService {
            if (INSTANCE == null) {
                INSTANCE = NetworkReachabilityService(context)
            }
            return INSTANCE!!
        }
    }

    private fun updateAvailability(networkCapabilities: NetworkCapabilities?) {
        if (networkCapabilities == null) {
            postUpdate(NetworkState.Unavailable)
            return
        }
        var networkType: NetworkType = NetworkType.OTHER

        if (networkCapabilities.hasTransport(TRANSPORT_CELLULAR)) {
            networkType = NetworkType.CELL
        }
        if (networkCapabilities.hasTransport(TRANSPORT_WIFI)) {
            networkType = NetworkType.WiFi
        }

        postUpdate(NetworkState.Available(networkType))
    }

    private fun postUpdate(networkState: NetworkState) {
        NETWORK_REACHABILITY.onNext(networkState)
    }

    fun getIpv4HostAddress(): String? =
        NetworkInterface.getNetworkInterfaces()?.toList()?.mapNotNull { networkInterface ->
            networkInterface.inetAddresses?.toList()
                ?.filter { !it.isLoopbackAddress && it.hostAddress.indexOf(':') < 0 }
                ?.mapNotNull { if (it.hostAddress.isNullOrBlank()) null else it.hostAddress }
                ?.firstOrNull { it.isNotEmpty() }
        }?.firstOrNull()

    fun getIpv6HostAddress(): String? =
        NetworkInterface.getNetworkInterfaces()?.toList()?.mapNotNull { networkInterface ->
            networkInterface.inetAddresses?.toList()
                ?.filter { !it.isLoopbackAddress && it is Inet6Address }
                ?.mapNotNull { if (it.hostAddress.isNullOrBlank()) null else it.hostAddress }
                ?.firstOrNull { it.isNotEmpty() }
        }?.firstOrNull()

    fun pauseListeningNetworkChanges() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Usually happens only once if: "NetworkCallback was not registered"
        }
    }

    fun resumeListeningNetworkChanges() {
        pauseListeningNetworkChanges()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                networkCallback
            )
        }
    }
}