package com.adhdfocus.app.domain.sync

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineDetectorIntegrationTest {

    @Test
    fun `offline detector integrates with connectivity manager for online state`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.isOnline() } returns true
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()
        assertTrue(detector.isOnline())

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true), states)
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector integrates with connectivity manager for offline state`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.isOnline() } returns false
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(false)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()
        assertFalse(detector.isOnline())

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(false), states)
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector handles online to offline transition`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, false)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false), states)
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector handles offline to online transition`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(false, true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(false, true), states)
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector handles multiple rapid transitions`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, false, true, false, true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false, true, false, true), states)
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector state is consistent across multiple calls`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.isOnline() } returns true

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val state1 = detector.isOnline()
        val state2 = detector.isOnline()
        val state3 = detector.isOnline()

        assertEquals(state1, state2)
        assertEquals(state2, state3)
        assertTrue(state1)

        detector.stopMonitoring()
    }

    @Test
    fun `offline detector can be restarted after stopping`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.isOnline() } returns true
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()
        assertTrue(detector.isOnline())
        detector.stopMonitoring()

        detector.startMonitoring()
        assertTrue(detector.isOnline())
        detector.stopMonitoring()
    }

    @Test
    fun `offline detector emits current state immediately on subscription`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
            if (states.size >= 1) return@collect
        }

        assertEquals(1, states.size)
        assertEquals(true, states.first())

        detector.stopMonitoring()
    }

    @Test
    fun `offline detector filters duplicate consecutive states`() = runTest {
        val mockConnectivityManager = mockk<ConnectivityManager>()
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, true, true, false, false, true)

        val detector = OfflineDetectorImpl(mockConnectivityManager)

        detector.startMonitoring()

        val states = mutableListOf<Boolean>()
        detector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false, true), states)
        detector.stopMonitoring()
    }
}
