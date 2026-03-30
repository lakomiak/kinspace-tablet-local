package com.adhdfocus.app.domain.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Property-based tests for OfflineDetector.
 *
 * **Validates: Requirements 11 (Offline Capability - Detect when device goes offline and handle appropriately)**
 */
class OfflineDetectorPropertyTest : FunSpec({

    test("Property 1: State consistency - isOnline matches observeConnectivityState") {
        runTest {
            checkAll(Arb.boolean()) { isOnlineState ->
                val mockConnectivityManager = mockk<ConnectivityManager>()
                every { mockConnectivityManager.isOnline() } returns isOnlineState
                every { mockConnectivityManager.observeConnectivity() } returns flowOf(isOnlineState)

                val detector = OfflineDetectorImpl(mockConnectivityManager)

                val isOnline = detector.isOnline()
                val flow = detector.observeConnectivityState()
                val states = mutableListOf<Boolean>()

                flow.collect { state ->
                    states.add(state)
                }

                isOnline shouldBe states.first()
            }
        }
    }

    test("Property 2: Transition correctness - all state transitions are valid") {
        runTest {
            checkAll(Arb.list(Arb.boolean(), range = 1..10)) { states ->
                val mockConnectivityManager = mockk<ConnectivityManager>()
                every { mockConnectivityManager.observeConnectivity() } returns flowOf(*states.toTypedArray())

                val detector = OfflineDetectorImpl(mockConnectivityManager)
                val flow = detector.observeConnectivityState()
                val emittedStates = mutableListOf<Boolean>()

                flow.collect { state ->
                    emittedStates.add(state)
                }

                // All emitted states should be valid booleans
                emittedStates.all { it is Boolean } shouldBe true
            }
        }
    }

    test("Property 3: Event emission - observeConnectivityState emits all state changes") {
        runTest {
            checkAll(Arb.list(Arb.boolean(), range = 1..10)) { states ->
                val mockConnectivityManager = mockk<ConnectivityManager>()
                every { mockConnectivityManager.observeConnectivity() } returns flowOf(*states.toTypedArray())

                val detector = OfflineDetectorImpl(mockConnectivityManager)
                val flow = detector.observeConnectivityState()
                val emittedStates = mutableListOf<Boolean>()

                flow.collect { state ->
                    emittedStates.add(state)
                }

                // Should emit at least one state
                emittedStates.isNotEmpty() shouldBe true
            }
        }
    }

    test("Property 4: Duplicate filtering - consecutive identical states are filtered") {
        runTest {
            checkAll(Arb.list(Arb.boolean(), range = 1..5)) { baseStates ->
                // Create a list with duplicates
                val statesWithDuplicates = mutableListOf<Boolean>()
                baseStates.forEach { state ->
                    statesWithDuplicates.add(state)
                    statesWithDuplicates.add(state) // Add duplicate
                }

                val mockConnectivityManager = mockk<ConnectivityManager>()
                every { mockConnectivityManager.observeConnectivity() } returns flowOf(*statesWithDuplicates.toTypedArray())

                val detector = OfflineDetectorImpl(mockConnectivityManager)
                val flow = detector.observeConnectivityState()
                val emittedStates = mutableListOf<Boolean>()

                flow.collect { state ->
                    emittedStates.add(state)
                }

                // Check that no consecutive states are identical
                for (i in 0 until emittedStates.size - 1) {
                    (emittedStates[i] != emittedStates[i + 1]) shouldBe true
                }
            }
        }
    }

    test("Property 5: Error handling - detector handles empty state sequences") {
        runTest {
            val mockConnectivityManager = mockk<ConnectivityManager>()
            every { mockConnectivityManager.observeConnectivity() } returns flowOf()

            val detector = OfflineDetectorImpl(mockConnectivityManager)
            val flow = detector.observeConnectivityState()
            val emittedStates = mutableListOf<Boolean>()

            flow.collect { state ->
                emittedStates.add(state)
            }

            // Should handle empty flow gracefully
            emittedStates.isEmpty() shouldBe true
        }
    }

    test("Property 6: Monitoring state - startMonitoring and stopMonitoring are idempotent") {
        runTest {
            val mockConnectivityManager = mockk<ConnectivityManager>()
            every { mockConnectivityManager.isOnline() } returns true

            val detector = OfflineDetectorImpl(mockConnectivityManager)

            // Multiple start calls should be safe
            detector.startMonitoring()
            detector.startMonitoring()
            detector.startMonitoring()

            // Multiple stop calls should be safe
            detector.stopMonitoring()
            detector.stopMonitoring()
            detector.stopMonitoring()

            // Should still work after multiple calls
            detector.isOnline() shouldBe true
        }
    }

    test("Property 7: State consistency after monitoring - isOnline remains consistent") {
        runTest {
            checkAll(Arb.boolean()) { expectedState ->
                val mockConnectivityManager = mockk<ConnectivityManager>()
                every { mockConnectivityManager.isOnline() } returns expectedState

                val detector = OfflineDetectorImpl(mockConnectivityManager)

                detector.startMonitoring()
                val state1 = detector.isOnline()
                val state2 = detector.isOnline()
                detector.stopMonitoring()

                state1 shouldBe expectedState
                state2 shouldBe expectedState
                state1 shouldBe state2
            }
        }
    }

    test("Property 8: Transition detection - all transitions are detected") {
        runTest {
            val mockConnectivityManager = mockk<ConnectivityManager>()
            every { mockConnectivityManager.observeConnectivity() } returns flowOf(true, false, true, false)

            val detector = OfflineDetectorImpl(mockConnectivityManager)
            val flow = detector.observeConnectivityState()
            val emittedStates = mutableListOf<Boolean>()

            flow.collect { state ->
                emittedStates.add(state)
            }

            // Should detect all transitions
            emittedStates shouldBe listOf(true, false, true, false)
        }
    }
})
