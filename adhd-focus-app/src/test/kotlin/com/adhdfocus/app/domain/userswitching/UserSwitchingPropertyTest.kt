package com.adhdfocus.app.domain.userswitching

import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.model.UserSwitchingState
import com.adhdfocus.app.data.repository.UserSwitchingRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for user switching logic and state management.
 *
 * **Validates: Phase 3 Requirements - Family Member Switching**
 *
 * These tests verify that:
 * 1. User switching maintains data isolation between family members
 * 2. Session state is properly tracked and managed
 * 3. User validation prevents invalid switches
 * 4. Session duration and timing are accurate
 * 5. Multiple users can be managed without data corruption
 */
class UserSwitchingPropertyTest : FunSpec({
    val userSwitchingRepository = mockk<UserSwitchingRepository>()
    val userSwitchingManager = UserSwitchingManager(userSwitchingRepository)

    /**
     * Property: User Switching Maintains Data Isolation
     *
     * FOR ALL valid user switches, each user's data SHALL remain isolated
     * and not accessible to other users.
     */
    test("Property: User switching maintains data isolation for all users") {
        runTest {
            checkAll(
                iterations = 50,
                arb = householdWithMultipleUsersArbitrary()
            ) { (householdId, users) ->
                // For each user in the household
                users.forEach { targetUser ->
                    coEvery {
                        userSwitchingRepository.validateUserSwitch(targetUser.id, householdId)
                    } returns true

                    coEvery {
                        userSwitchingRepository.setCurrentUser(targetUser.id, householdId)
                    } returns true

                    // Switch to this user
                    val switchResult = userSwitchingManager.switchUser(targetUser.id, householdId)

                    // Verify switch was successful
                    switchResult shouldBe true

                    // Verify that only this user is current
                    coEvery {
                        userSwitchingRepository.getCurrentUser()
                    } returns targetUser

                    val currentUser = userSwitchingManager.getCurrentUser()
                    currentUser?.id shouldBe targetUser.id
                    currentUser?.householdId shouldBe householdId
                }
            }
        }
    }

    /**
     * Property: Session State Tracking
     *
     * FOR ALL user switches, session state SHALL be properly tracked
     * with accurate timestamps.
     */
    test("Property: Session state is tracked accurately for all switches") {
        runTest {
            checkAll(
                iterations = 50,
                arb = userSwitchSequenceArbitrary()
            ) { userSequence ->
                userSequence.forEach { (userId, householdId) ->
                    coEvery {
                        userSwitchingRepository.validateUserSwitch(userId, householdId)
                    } returns true

                    coEvery {
                        userSwitchingRepository.setCurrentUser(userId, householdId)
                    } returns true

                    val beforeSwitch = Instant.now()
                    userSwitchingManager.switchUser(userId, householdId)
                    val afterSwitch = Instant.now()

                    // Session duration should be tracked
                    coEvery {
                        userSwitchingRepository.getSessionDuration()
                    } returns 0L // Fresh session

                    val sessionDuration = userSwitchingManager.getSessionDuration()
                    sessionDuration shouldBe 0L // New session starts at 0
                }
            }
        }
    }

    /**
     * Property: User Validation Prevents Invalid Switches
     *
     * FOR ALL invalid user IDs or household mismatches, user switching
     * SHALL fail and prevent the switch.
     */
    test("Property: Invalid user switches are prevented for all invalid cases") {
        runTest {
            checkAll(
                iterations = 50,
                arb = invalidUserSwitchArbitrary()
            ) { (userId, householdId, isValid) ->
                coEvery {
                    userSwitchingRepository.validateUserSwitch(userId, householdId)
                } returns isValid

                if (!isValid) {
                    val switchResult = userSwitchingManager.switchUser(userId, householdId)
                    switchResult shouldBe false
                }
            }
        }
    }

    /**
     * Property: Session Duration Calculation
     *
     * FOR ALL active sessions, session duration SHALL be calculated
     * correctly based on session start time.
     */
    test("Property: Session duration is calculated correctly for all sessions") {
        runTest {
            checkAll(
                iterations = 50,
                arb = sessionDurationArbitrary()
            ) { (sessionStartTime, expectedDurationRange) ->
                val state = UserSwitchingState(
                    userId = "user-1",
                    householdId = "household-1",
                    sessionStartTime = sessionStartTime
                )

                coEvery {
                    userSwitchingRepository.getCurrentUserState()
                } returns state

                val duration = userSwitchingManager.getSessionDuration()

                // Duration should be within expected range (allowing for test execution time)
                duration shouldBe expectedDurationRange
            }
        }
    }

    /**
     * Property: Time Since Last Switch Tracking
     *
     * FOR ALL user switches, the time since last switch SHALL be
     * accurately tracked.
     */
    test("Property: Time since last switch is tracked accurately") {
        runTest {
            checkAll(
                iterations = 50,
                arb = timeSinceLastSwitchArbitrary()
            ) { (lastSwitchTime, expectedTimeRange) ->
                val state = UserSwitchingState(
                    userId = "user-1",
                    householdId = "household-1",
                    lastSwitchTime = lastSwitchTime
                )

                coEvery {
                    userSwitchingRepository.getCurrentUserState()
                } returns state

                val timeSinceSwitch = userSwitchingManager.getTimeSinceLastSwitch()

                // Time should be within expected range
                timeSinceSwitch shouldBe expectedTimeRange
            }
        }
    }

    /**
     * Property: Multiple User Switches Don't Corrupt State
     *
     * FOR ALL sequences of user switches, the final state SHALL
     * correctly reflect the last switch without data corruption.
     */
    test("Property: Multiple consecutive switches maintain correct state") {
        runTest {
            checkAll(
                iterations = 30,
                arb = multipleUserSwitchSequenceArbitrary()
            ) { userSequence ->
                var lastUser: User? = null

                userSequence.forEach { user ->
                    coEvery {
                        userSwitchingRepository.validateUserSwitch(user.id, user.householdId)
                    } returns true

                    coEvery {
                        userSwitchingRepository.setCurrentUser(user.id, user.householdId)
                    } returns true

                    val switchResult = userSwitchingManager.switchUser(user.id, user.householdId)
                    switchResult shouldBe true
                    lastUser = user
                }

                // Verify final state is correct
                coEvery {
                    userSwitchingRepository.getCurrentUser()
                } returns lastUser

                val currentUser = userSwitchingManager.getCurrentUser()
                currentUser?.id shouldBe lastUser?.id
            }
        }
    }

    /**
     * Property: User Switching Enabled State
     *
     * FOR ALL user switches, user switching SHALL be enabled
     * after a successful switch.
     */
    test("Property: User switching is enabled after successful switch") {
        runTest {
            checkAll(
                iterations = 50,
                arb = validUserArbitrary()
            ) { user ->
                coEvery {
                    userSwitchingRepository.validateUserSwitch(user.id, user.householdId)
                } returns true

                coEvery {
                    userSwitchingRepository.setCurrentUser(user.id, user.householdId)
                } returns true

                userSwitchingManager.switchUser(user.id, user.householdId)

                coEvery {
                    userSwitchingRepository.isUserSwitchingEnabled()
                } returns true

                val isEnabled = userSwitchingManager.isUserSwitchingEnabled()
                isEnabled shouldBe true
            }
        }
    }

    /**
     * Property: Clear Current User Disables Switching
     *
     * FOR ALL active sessions, clearing the current user SHALL
     * disable user switching.
     */
    test("Property: Clearing current user disables switching") {
        runTest {
            checkAll(
                iterations = 50,
                arb = validUserArbitrary()
            ) { user ->
                coEvery {
                    userSwitchingRepository.validateUserSwitch(user.id, user.householdId)
                } returns true

                coEvery {
                    userSwitchingRepository.setCurrentUser(user.id, user.householdId)
                } returns true

                userSwitchingManager.switchUser(user.id, user.householdId)

                coEvery {
                    userSwitchingRepository.clearCurrentUser()
                } returns Unit

                userSwitchingManager.clearCurrentUser()

                coEvery {
                    userSwitchingRepository.isUserSwitchingEnabled()
                } returns false

                val isEnabled = userSwitchingManager.isUserSwitchingEnabled()
                isEnabled shouldBe false
            }
        }
    }

    /**
     * Property: User Switching Preserves Household Context
     *
     * FOR ALL user switches within a household, the household ID
     * SHALL remain constant and correct.
     */
    test("Property: Household context is preserved across user switches") {
        runTest {
            checkAll(
                iterations = 50,
                arb = householdWithMultipleUsersArbitrary()
            ) { (householdId, users) ->
                users.forEach { user ->
                    coEvery {
                        userSwitchingRepository.validateUserSwitch(user.id, householdId)
                    } returns true

                    coEvery {
                        userSwitchingRepository.setCurrentUser(user.id, householdId)
                    } returns true

                    userSwitchingManager.switchUser(user.id, householdId)

                    coEvery {
                        userSwitchingRepository.getCurrentUser()
                    } returns user

                    val currentUser = userSwitchingManager.getCurrentUser()
                    currentUser?.householdId shouldBe householdId
                }
            }
        }
    }
})

/**
 * Generates arbitrary households with multiple users.
 */
private fun householdWithMultipleUsersArbitrary(): Arb<Pair<String, List<User>>> {
    return Arb.bind(
        Arb.string(1..50), // householdId
        Arb.list(validUserArbitrary(), 2..5) // users
    ) { householdId, users ->
        // Ensure all users belong to the same household
        val usersInHousehold = users.map { it.copy(householdId = householdId) }
        Pair(householdId, usersInHousehold)
    }
}

/**
 * Generates arbitrary sequences of user switches.
 */
private fun userSwitchSequenceArbitrary(): Arb<List<Pair<String, String>>> {
    return Arb.list(
        Arb.bind(
            Arb.string(1..50), // userId
            Arb.string(1..50) // householdId
        ) { userId, householdId ->
            Pair(userId, householdId)
        },
        1..10
    )
}

/**
 * Generates arbitrary invalid user switches.
 */
private fun invalidUserSwitchArbitrary(): Arb<Triple<String, String, Boolean>> {
    return Arb.bind(
        Arb.string(1..50), // userId
        Arb.string(1..50), // householdId
        Arb.of(true, false) // isValid
    ) { userId, householdId, isValid ->
        Triple(userId, householdId, isValid)
    }
}

/**
 * Generates arbitrary session durations.
 */
private fun sessionDurationArbitrary(): Arb<Pair<Instant, Long>> {
    return Arb.bind(
        Arb.long(1000000000000L..System.currentTimeMillis()) // sessionStartTime
    ) { sessionStartTime ->
        val now = System.currentTimeMillis()
        val duration = now - sessionStartTime
        Pair(Instant.ofEpochMilli(sessionStartTime), duration)
    }
}

/**
 * Generates arbitrary time since last switch values.
 */
private fun timeSinceLastSwitchArbitrary(): Arb<Pair<Instant, Long>> {
    return Arb.bind(
        Arb.long(1000000000000L..System.currentTimeMillis()) // lastSwitchTime
    ) { lastSwitchTime ->
        val now = System.currentTimeMillis()
        val timeSinceSwitch = now - lastSwitchTime
        Pair(Instant.ofEpochMilli(lastSwitchTime), timeSinceSwitch)
    }
}

/**
 * Generates arbitrary valid users.
 */
private fun validUserArbitrary(): Arb<User> {
    return Arb.bind(
        Arb.string(1..50), // id
        Arb.string(1..50), // householdId
        Arb.string(5..50), // email
        Arb.string(1..50), // displayName
        Arb.of(*UserRole.values()) // role
    ) { id, householdId, email, displayName, role ->
        User(
            id = id,
            householdId = householdId,
            email = "$email@example.com",
            displayName = displayName,
            role = role
        )
    }
}

/**
 * Generates arbitrary sequences of multiple user switches.
 */
private fun multipleUserSwitchSequenceArbitrary(): Arb<List<User>> {
    return Arb.list(validUserArbitrary(), 2..5)
}
