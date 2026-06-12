package com.adhdfocus.app.ui.setup

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

data class DraftFamilyMember(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val birthDate: LocalDate?,
    val role: UserRole
)

@HiltViewModel
class LocalSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val setupManager: TabletSetupManager,
    private val categoryReminderScheduler: CategoryReminderScheduler
) : ViewModel() {

    private val _householdName = MutableStateFlow("")
    val householdName: StateFlow<String> = _householdName

    private val _members = MutableStateFlow<List<DraftFamilyMember>>(emptyList())
    val members: StateFlow<List<DraftFamilyMember>> = _members

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun updateHouseholdName(value: String) {
        _householdName.value = value
    }

    fun addMember(displayName: String, birthDate: LocalDate?) {
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            _errorMessage.value = "Each family member needs a name."
            return
        }

        _members.value = _members.value + DraftFamilyMember(
            displayName = trimmedName,
            birthDate = birthDate,
            role = UserRole.ADHD_USER
        )
        _errorMessage.value = null
    }

    fun removeMember(memberId: String) {
        _members.value = _members.value.filterNot { it.id == memberId }
    }

    fun completeSetup(onComplete: () -> Unit) {
        val trimmedHouseholdName = _householdName.value.trim()
        val draftMembers = _members.value

        if (trimmedHouseholdName.isBlank()) {
            _errorMessage.value = "Give this tablet household a name."
            return
        }

        if (draftMembers.isEmpty()) {
            _errorMessage.value = "Add at least one family member before finishing setup."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            val householdId = "local-${UUID.randomUUID()}"
            val now = Instant.now()

            draftMembers.forEach { member ->
                userRepository.saveUser(
                    User(
                        id = member.id,
                        householdId = householdId,
                        email = "${member.id}@kinspace.family",
                        displayName = member.displayName,
                        birthDate = member.birthDate,
                        role = member.role,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            val defaultMember = draftMembers.first()
            setupManager.completeSetup(
                memberId = defaultMember.id,
                memberName = defaultMember.displayName,
                householdId = householdId,
                householdName = trimmedHouseholdName
            )

            categoryReminderScheduler.rescheduleForCurrentSetup()
            _isSaving.value = false
            onComplete()
        }
    }
}
