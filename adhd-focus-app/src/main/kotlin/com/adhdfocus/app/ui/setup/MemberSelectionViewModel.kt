package com.adhdfocus.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FamilyMember(
    val id: String,
    val name: String,
    val email: String?,
    val avatarUrl: String?,
    val birthDate: LocalDate? = null
)

@HiltViewModel
class MemberSelectionViewModel @Inject constructor(
    private val setupManager: TabletSetupManager,
    private val categoryReminderScheduler: CategoryReminderScheduler,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val householdId: String?
        get() = setupManager.getHouseholdId()

    init {
        loadMembers()
    }

    fun retry() = loadMembers()

    fun selectMember(member: FamilyMember) {
        val resolvedHouseholdId = householdId ?: return
        setupManager.completeSetup(
            memberId = member.id,
            memberName = member.name,
            householdId = resolvedHouseholdId,
            householdName = setupManager.getHouseholdName()
        )
        viewModelScope.launch {
            categoryReminderScheduler.rescheduleForCurrentSetup()
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val resolvedHouseholdId = householdId
            if (resolvedHouseholdId.isNullOrBlank()) {
                _members.value = emptyList()
                _error.value = "This tablet has not been set up yet."
                _isLoading.value = false
                return@launch
            }

            val users = userRepository.getUsersByHousehold(resolvedHouseholdId)
                .sortedBy { it.displayName.lowercase() }

            _members.value = users.map { user ->
                FamilyMember(
                    id = user.id,
                    name = user.displayName,
                    email = user.email.takeUnless { it.endsWith("@kinspace.family") },
                    avatarUrl = user.avatarUrl,
                    birthDate = user.birthDate
                )
            }

            if (_members.value.isEmpty()) {
                _error.value = "No local family members have been created on this tablet yet."
            }

            _isLoading.value = false
        }
    }
}
