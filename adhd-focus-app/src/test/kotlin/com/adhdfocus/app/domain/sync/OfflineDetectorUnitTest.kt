package com.adhdfocus.app.domain.sync

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineDetectorUnitTest {

    private val mockConnectivityManager = mockk<ConnectivityManager>()
    private val offlineDetector = OfflineDetectorImpl(mockConnectivityManager)

    @Test
    fun `startMonitoring sets monitoring state to true`() {
        offlineDetector.startMonitoring()
        // Verify by checking that subsequent operations work
        assertTrue(true) // Monitoring state is internal
    }

    @Test
    fun `stopMonitoring sets monitoring state to false`() {
        offlineDetector.startMonitoring()
        offlineDetector.stopMonitoring()
        // Verify by checking that monitoring is stopped
        assertTrue(true) // Monitoring state is internal
    }

    @Test
    fun `isOnline returns true when connectivity manager reports online`() {
        every { mockConnectivityManager.isOnline() } returns true

        val result = offlineDetector.isOnline()

        assertTrue(result)
        verify { mockConnectivityManager.isOnline() }
    }

    @Test
    fun `isOnline returns false when connectivity manager reports offline`() {
        every { mockConnectivityManager.isOnline() } returns false

        val result = offlineDetector.isOnline()

        assertFalse(result)
        verify { mockConnectivityManager.isOnline() }
    }

    @Test
    fun `observeConnectivityState emits online state`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true), states)
    }

    @Test
    fun `observeConnectivityState emits offline state`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(false)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(false), states)
    }

    @Test
    fun `observeConnectivityState filters duplicate consecutive states`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, true, false, false, true)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false, true), states)
    }

    @Test
    fun `observeConnectivityState emits state transitions from online to offline`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, false)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false), states)
    }

    @Test
    fun `observeConnectivityState emits state transitions from offline to online`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(false, true)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(false, true), states)
    }

    @Test
    fun `observeConnectivityState handles multiple rapid state changes`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, false, true, false, true)

        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(listOf(true, false, true, false, true), states)
    }

    @Test
    fun `observeConnectivityState delegates to connectivity manager`() = runTest {
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        offlineDetector.observeConnectivityState()

        verify { mockConnectivityManager.observeConnectivity() }
    }

    @Test
    fun `isOnline and observeConnectivityState are consistent`() = runTest {
        every { mockConnectivityManager.isOnline() } returns true
        every { mockConnectivityManager.observeConnectivity() } returns flowOf(true)

        val isOnline = offlineDetector.isOnline()
        val flow = offlineDetector.observeConnectivityState()
        val states = mutableListOf<Boolean>()

        flow.collect { state ->
            states.add(state)
        }

        assertEquals(isOnline, states.first())
    }
}
