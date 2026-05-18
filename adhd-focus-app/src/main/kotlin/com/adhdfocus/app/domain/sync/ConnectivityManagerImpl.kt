package com.adhdfocus.app.domain.sync

import android.content.Context
import android.os.Build
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Android implementation of ConnectivityManager using system ConnectivityManager.
 *
 * Monitors network connectivity changes and emits state via Flow.
 * Handles different network types (WiFi, cellular, etc.).
 */
class ConnectivityManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectivityManager {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        as android.net.ConnectivityManager

    override fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(true)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
                trySend(true)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Emit current state immediately
        trySend(isOnline())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun isOnline(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            networkInfo.isConnected
        }
    }
}
