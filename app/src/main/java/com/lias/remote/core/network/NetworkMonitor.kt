// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/NetworkMonitor.kt
// Version: 13.0.0
//
// Purpose:
//   Low-cost Android network availability observer.
//
// Notes:
//   - Uses ACCESS_NETWORK_STATE already present in the manifest.
//   - Does not try to decide whether LIAS itself is reachable.
//   - "Available" means Android has a usable network transport.
//   - LIAS reachability remains the responsibility of REST/SSE.
//
// This avoids wasting reconnect loops while the device has no network.
// ====================================================================

package com.lias.remote.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(
    context: Context
) {

    private val connectivityManager =
        context.getSystemService(
            ConnectivityManager::class.java
        )

    private val _available =
        MutableStateFlow(
            currentAvailability()
        )

    val available:
        StateFlow<Boolean> =
        _available.asStateFlow()

    private var started =
        false

    private val callback =
        object :
            ConnectivityManager.NetworkCallback() {

            override fun onAvailable(
                network: Network
            ) {
                publishCurrentState()
            }

            override fun onLost(
                network: Network
            ) {
                publishCurrentState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities:
                    NetworkCapabilities
            ) {
                publishCurrentState()
            }
        }

    @Synchronized
    fun start() {
        if (started) {
            return
        }

        started =
            true

        try {

            connectivityManager
                .registerDefaultNetworkCallback(
                    callback
                )

        } catch (
            _: Exception
        ) {
            /*
             * If registration fails on an unusual OEM implementation,
             * retain the synchronous availability snapshot and let
             * SSE's normal retry behavior continue.
             */
        }

        publishCurrentState()
    }

    @Synchronized
    fun stop() {
        if (
            !started
        ) {
            return
        }

        started =
            false

        try {

            connectivityManager
                .unregisterNetworkCallback(
                    callback
                )

        } catch (
            _: Exception
        ) {
            // Already unregistered.
        }
    }

    private fun publishCurrentState() {

        _available.value =
            currentAvailability()
    }

    private fun currentAvailability():
        Boolean {

        val activeNetwork =
            connectivityManager
                .activeNetwork
                ?: return false

        val capabilities =
            connectivityManager
                .getNetworkCapabilities(
                    activeNetwork
                )
                ?: return false

        /*
         * NET_CAPABILITY_INTERNET is intentionally not required.
         *
         * LIAS is commonly a LAN-only endpoint. A Wi-Fi network can be
         * perfectly valid for LIAS while Android considers Internet
         * validation unavailable.
         */
        return capabilities.hasTransport(
            NetworkCapabilities
                .TRANSPORT_WIFI
        ) ||
            capabilities.hasTransport(
                NetworkCapabilities
                    .TRANSPORT_CELLULAR
            ) ||
            capabilities.hasTransport(
                NetworkCapabilities
                    .TRANSPORT_ETHERNET
            ) ||
            capabilities.hasTransport(
                NetworkCapabilities
                    .TRANSPORT_VPN
            )
    }
}
