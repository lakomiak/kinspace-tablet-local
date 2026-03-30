package com.adhdfocus.app.ui.family

import com.adhdfocus.app.domain.userswitching.PinManagementManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PinManagementViewModelTest {
    private lateinit var pinManagementManager: PinManagementManager
    private lateinit var viewModel: PinManagementViewModel

    private val testUserId = "user-123"
    private val testPin = "1234"
    private val testNewPin = "5678"

    @Before
    fun setup() {
        pinManagementManager = mockk()
        viewModel = PinManagementViewModel(pinManagementManager)
    }

    // ============ Initialization Tests ============

    @Test
    fun `initialize should load PIN status for unprotected user`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false

        viewModel.initialize(testUserId)

        assertEquals(PinStatus.UNPROTECTED, viewModel.currentPinStatus.value)
    }

    @Test
    fun `initialize should load PIN status for protected user`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true

        viewModel.initialize(testUserId)

        assertEquals(PinStatus.PROTECTED, viewModel.currentPinStatus.value)
    }

    @Test
    fun `initialize should set error message on failure`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } throws Exception("Load failed")

        viewModel.initialize(testUserId)

        assertTrue(viewModel.errorMessage.value?.contains("Failed to load PIN status") == true)
    }

    // ============ setupPin Tests ============

    @Test
    fun `setupPin should set PIN successfully`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)

        assertEquals(PinStatus.PROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.successMessage.value?.contains("PIN set successfully") == true)
    }

    @Test
    fun `setupPin should show error on failure`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns false

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)

        assertEquals(PinStatus.UNPROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to set PIN") == true)
    }

    @Test
    fun `setupPin should show error on exception`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } throws Exception("Setup failed")

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)

        assertTrue(viewModel.errorMessage.value?.contains("Error setting PIN") == true)
    }

    @Test
    fun `setupPin should set loading state`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `setupPin should clear previous messages`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)

        assertNull(viewModel.errorMessage.value)
    }

    // ============ changePin Tests ============

    @Test
    fun `changePin should change PIN successfully`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.changePinForUser(testUserId, testPin, testNewPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.changePin(testPin, testNewPin)

        assertEquals(PinStatus.PROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.successMessage.value?.contains("PIN changed successfully") == true)
    }

    @Test
    fun `changePin should show error on failure`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.changePinForUser(testUserId, testPin, testNewPin) } returns false

        viewModel.initialize(testUserId)
        viewModel.changePin(testPin, testNewPin)

        assertEquals(PinStatus.PROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to change PIN") == true)
    }

    @Test
    fun `changePin should show error on exception`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.changePinForUser(testUserId, testPin, testNewPin) } throws Exception("Change failed")

        viewModel.initialize(testUserId)
        viewModel.changePin(testPin, testNewPin)

        assertTrue(viewModel.errorMessage.value?.contains("Error changing PIN") == true)
    }

    @Test
    fun `changePin should set loading state`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.changePinForUser(testUserId, testPin, testNewPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.changePin(testPin, testNewPin)

        assertFalse(viewModel.isLoading.value)
    }

    // ============ removePin Tests ============

    @Test
    fun `removePin should remove PIN successfully`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.removePinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.removePin(testPin)

        assertEquals(PinStatus.UNPROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.successMessage.value?.contains("PIN protection removed") == true)
    }

    @Test
    fun `removePin should show error on failure`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.removePinForUser(testUserId, testPin) } returns false

        viewModel.initialize(testUserId)
        viewModel.removePin(testPin)

        assertEquals(PinStatus.PROTECTED, viewModel.currentPinStatus.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to remove PIN") == true)
    }

    @Test
    fun `removePin should show error on exception`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.removePinForUser(testUserId, testPin) } throws Exception("Remove failed")

        viewModel.initialize(testUserId)
        viewModel.removePin(testPin)

        assertTrue(viewModel.errorMessage.value?.contains("Error removing PIN") == true)
    }

    @Test
    fun `removePin should set loading state`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns true
        coEvery { pinManagementManager.removePinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.removePin(testPin)

        assertFalse(viewModel.isLoading.value)
    }

    // ============ clearMessages Tests ============

    @Test
    fun `clearMessages should clear error message`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns false

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)
        assertTrue(viewModel.errorMessage.value != null)

        viewModel.clearMessages()

        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearMessages should clear success message`() = runTest {
        coEvery { pinManagementManager.isPinProtected(testUserId) } returns false
        coEvery { pinManagementManager.setPinForUser(testUserId, testPin) } returns true

        viewModel.initialize(testUserId)
        viewModel.setupPin(testPin)
        assertTrue(viewModel.successMessage.value != null)

        viewModel.clearMessages()

        assertNull(viewModel.successMessage.value)
    }

    // ============ State Management Tests ============

    @Test
    fun `initial state should be UNPROTECTED`() = runTest {
        assertEquals(PinStatus.UNPROTECTED, viewModel.currentPinStatus.value)
    }

    @Test
    fun `initial isLoading should be false`() = runTest {
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `initial errorMessage should be null`() = runTest {
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `initial successMessage should be null`() = runTest {
        assertNull(viewModel.successMessage.value)
    }

    @Test
    fun `setupPin without initialization should not crash`() = runTest {
        coEvery { pinManagementManager.setPinForUser(any(), testPin) } returns true

        // Should not throw
        viewModel.setupPin(testPin)
    }

    @Test
    fun `changePin without initialization should not crash`() = runTest {
        coEvery { pinManagementManager.changePinForUser(any(), testPin, testNewPin) } returns true

        // Should not throw
        viewModel.changePin(testPin, testNewPin)
    }

    @Test
    fun `removePin without initialization should not crash`() = runTest {
        coEvery { pinManagementManager.removePinForUser(any(), testPin) } returns true

        // Should not throw
        viewModel.removePin(testPin)
    }
}
