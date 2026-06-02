package com.adhdfocus.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ManagedFamilyMember(
    val id: String,
    val displayName: String,
    val role: UserRole,
    val birthDate: LocalDate?
)

@HiltViewModel
class FamilyManagementViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val setupManager: TabletSetupManager,
    private val categoryReminderScheduler: CategoryReminderScheduler
) : ViewModel() {

    private val _members = MutableStateFlow<List<ManagedFamilyMember>>(emptyList())
    val members: StateFlow<List<ManagedFamilyMember>> = _members

    private val _activeMemberId = MutableStateFlow<String?>(null)
    val activeMemberId: StateFlow<String?> = _activeMemberId

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun initialize() {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return
        viewModelScope.launch {
            _activeMemberId.value = setupManager.getAssignedMemberId()
            refreshMembers(householdId)
        }
    }

    fun addMember(displayName: String, birthDate: LocalDate?, role: UserRole) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return

        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            _errorMessage.value = "Member name cannot be blank."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val now = Instant.now()
            val id = UUID.randomUUID().toString()
            userRepository.saveUser(
                User(
                    id = id,
                    householdId = householdId,
                    email = "$id@kinspace.family",
                    displayName = trimmedName,
                    birthDate = birthDate,
                    role = role,
                    createdAt = now,
                    updatedAt = now
                )
            )
            refreshMembers(householdId)
            _errorMessage.value = null
            _isSaving.value = false
        }
    }

    fun updateMember(memberId: String, displayName: String, birthDate: LocalDate?, role: UserRole) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return

        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            _errorMessage.value = "Member name cannot be blank."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val existing = userRepository.getUserById(memberId)
            if (existing != null) {
                userRepository.updateUser(
                    existing.copy(
                        displayName = trimmedName,
                        birthDate = birthDate,
                        role = role,
                        updatedAt = Instant.now()
                    )
                )
                if (_activeMemberId.value == memberId) {
                    setupManager.completeSetup(
                        memberId = memberId,
                        memberName = trimmedName,
                        householdId = householdId,
                        householdName = setupManager.getHouseholdName()
                    )
                }
            }
            refreshMembers(householdId)
            _errorMessage.value = null
            _isSaving.value = false
        }
    }

    fun removeMember(memberId: String) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return
        val currentMembers = _members.value
        if (currentMembers.size <= 1) {
            _errorMessage.value = "At least one family member must remain on the tablet."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            userRepository.deleteUser(memberId)
            if (_activeMemberId.value == memberId) {
                val replacement = currentMembers.firstOrNull { it.id != memberId }
                if (replacement != null) {
                    setupManager.completeSetup(
                        memberId = replacement.id,
                        memberName = replacement.displayName,
                        householdId = householdId,
                        householdName = setupManager.getHouseholdName()
                    )
                    _activeMemberId.value = replacement.id
                }
            }
            refreshMembers(householdId)
            _isSaving.value = false
        }
    }

    fun setActiveMember(memberId: String) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return
        val member = _members.value.firstOrNull { it.id == memberId } ?: return
        setupManager.completeSetup(
            memberId = member.id,
            memberName = member.displayName,
            householdId = householdId,
            householdName = setupManager.getHouseholdName()
        )
        _activeMemberId.value = memberId
        viewModelScope.launch {
            categoryReminderScheduler.rescheduleForCurrentSetup()
        }
    }

    private suspend fun refreshMembers(householdId: String) {
        _members.value = userRepository.getUsersByHousehold(householdId)
            .sortedBy { it.displayName.lowercase() }
            .map {
                ManagedFamilyMember(
                    id = it.id,
                    displayName = it.displayName,
                    role = it.role,
                    birthDate = it.birthDate
                )
            }
    }
}
