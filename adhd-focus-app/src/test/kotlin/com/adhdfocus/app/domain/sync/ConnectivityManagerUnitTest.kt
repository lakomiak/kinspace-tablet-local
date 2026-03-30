package com.adhdfocus.app.domain.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectivityManagerUnitTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var manager: ConnectivityManagerImpl

    @Before
    fun setup() {
        context = mockk()
        connectivityManager = mockk()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    }

    @Test
    fun `isOnline returns true when internet capability available`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        manager = ConnectivityManagerImpl(context)

        assertTrue(manager.isOnline())
    }

    @Test
    fun `isOnline returns false when no active network`() {
        every { connectivityManager.activeNetwork } returns null

        manager = ConnectivityManagerImpl(context)

        assertFalse(manager.isOnline())
    }

    @Test
    fun `isOnline returns false when no internet capability`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        manager = ConnectivityManagerImpl(context)

        assertFalse(manager.isOnline())
    }

    @Test
    fun `observeConnectivity emits current state on subscription`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { connectivityManager.registerNetworkCallback(any(), any()) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any()) } returns Unit

        manager = ConnectivityManagerImpl(context)

        val state = runBlocking {
            manager.observeConnectivity().first()
        }

        assertTrue(state)
    }

    @Test
    fun `observeConnectivity emits false when offline`() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.registerNetworkCallback(any(), any()) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any()) } returns Unit

        manager = ConnectivityManagerImpl(context)

        val state = runBlocking {
            manager.observeConnectivity().first()
        }

        assertFalse(state)
    }

    @Test
    fun `registerNetworkCallback is called on observeConnectivity`() {
        val callbackSlot = slot<android.net.ConnectivityManager.NetworkCallback>()

        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any()) } returns Unit

        manager = ConnectivityManagerImpl(context)

        runBlocking {
            manager.observeConnectivity().first()
        }

        verify { connectivityManager.registerNetworkCallback(any(), any()) }
    }

    @Test
    fun `unregisterNetworkCallback is called on flow cancellation`() {
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.registerNetworkCallback(any(), any()) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any()) } returns Unit

        manager = ConnectivityManagerImpl(context)

        runBlocking {
            val flow = manager.observeConnectivity()
            flow.first()
        }

        verify { connectivityManager.unregisterNetworkCallback(any()) }
    }
}
