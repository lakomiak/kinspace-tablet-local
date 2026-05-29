package com.adhdfocus.app.ui.setup

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.User
import com.adhdfocus.app.data.model.UserRole
import com.adhdfocus.app.data.repository.UserRepository
import com.adhdfocus.app.data.network.ApiConfig
import com.adhdfocus.app.data.security.TokenStorage
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.setup.TabletSetupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
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
    private val tokenStorage: TokenStorage,
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

    private val _requiresReauth = MutableStateFlow(false)
    val requiresReauth: StateFlow<Boolean> = _requiresReauth

    private val _householdId = MutableStateFlow<String?>(null)

    private val http = OkHttpClient()

    init {
        loadMembers()
    }

    fun retry() = loadMembers()

    fun selectMember(member: FamilyMember) {
        val householdId = _householdId.value ?: return
        setupManager.completeSetup(member.id, member.name, householdId)
        viewModelScope.launch {
            categoryReminderScheduler.rescheduleForCurrentSetup()
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _requiresReauth.value = false
            try {
                val accessToken = tokenStorage.getAccessToken()
                val idToken = tokenStorage.getIdToken()

                if (accessToken == null && idToken == null) {
                    _error.value = "Not authenticated. Please sign in again."
                    _requiresReauth.value = true
                    return@launch
                }

                // Build token candidates list — try both access and ID token (same as mobile)
                val tokens = listOfNotNull(
                    accessToken,
                    idToken?.takeIf { it != accessToken }
                )

                val householdId = resolveHouseholdId(tokens)
                if (householdId == null) {
                    tokenStorage.clearTokens()
                    _error.value = "Could not determine your household. Please ensure your account is linked to a household in the Kinspace mobile app."
                    _requiresReauth.value = true
                    return@launch
                }

                _householdId.value = householdId
                log("Resolved householdId=$householdId, fetching family members")

                val members = fetchFamilyMembers(householdId, tokens)
                _members.value = members
                persistMembers(householdId, members)

                if (members.isEmpty()) {
                    _error.value = "No family members found. Please add members in the Kinspace app first."
                }
            } catch (e: Exception) {
                log("Failed to load members: ${e.message}")
                _error.value = "Failed to load family members: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Resolves household ID using the same strategy as calendar-mobile's SetupStatusController:
     * 1. custom:householdId claim in Cognito ID token
     * 2. /setup/status API endpoint
     * 3. /households list endpoint
     */
    private suspend fun resolveHouseholdId(tokens: List<String>): String? = withContext(Dispatchers.IO) {

        // Strategy 1: Check Cognito token claims
        for (token in tokens) {
            val fromClaim = extractClaim(token, "custom:householdId")
                ?: extractClaim(token, "householdId")
            if (!fromClaim.isNullOrBlank()) {
                log("householdId from token claim: $fromClaim")
                return@withContext fromClaim
            }
        }

        // Strategy 2: /setup/status endpoint
        val fromSetupStatus = fetchFromSetupStatus(tokens)
        if (!fromSetupStatus.isNullOrBlank()) {
            log("householdId from /setup/status: $fromSetupStatus")
            return@withContext fromSetupStatus
        }

        // Strategy 3: /households list endpoint
        val fromHouseholdsList = fetchFromHouseholdsList(tokens)
        if (!fromHouseholdsList.isNullOrBlank()) {
            log("householdId from /households list: $fromHouseholdsList")
            return@withContext fromHouseholdsList
        }

        log("Could not resolve householdId from any source")
        null
    }

    private fun fetchFromSetupStatus(tokens: List<String>): String? {
        for (token in tokens) {
            try {
                val response = get("${ApiConfig.BASE_URL}setup/status", token)
                if (response != null) {
                    val householdId = response.optString("householdId").takeIf { it.isNotEmpty() }
                    if (!householdId.isNullOrBlank()) return householdId
                    // Also check hasHousehold + nested id
                    if (response.optBoolean("hasHousehold")) {
                        val nested = response.optJSONObject("household")?.optString("id")
                        if (!nested.isNullOrBlank()) return nested
                    }
                }
            } catch (e: Exception) {
                log("fetchFromSetupStatus failed with token: ${e.message}")
            }
        }
        return null
    }

    private fun fetchFromHouseholdsList(tokens: List<String>): String? {
        for (token in tokens) {
            try {
                val response = get("${ApiConfig.BASE_URL}households", token)
                if (response != null) {
                    // Try households array
                    val array = response.optJSONArray("households")
                        ?: response.optJSONArray("items")
                        ?: response.optJSONArray("data")
                    if (array != null && array.length() > 0) {
                        val first = array.optJSONObject(0)
                        val id = first?.optString("id")?.takeIf { it.isNotEmpty() }
                            ?: first?.optString("householdId")?.takeIf { it.isNotEmpty() }
                        if (!id.isNullOrBlank()) return id
                    }
                    // Try single household object
                    val single = response.optJSONObject("household")
                    val id = single?.optString("id")?.takeIf { it.isNotEmpty() }
                    if (!id.isNullOrBlank()) return id
                }
            } catch (e: Exception) {
                log("fetchFromHouseholdsList failed with token: ${e.message}")
            }
        }
        return null
    }

    private suspend fun fetchFamilyMembers(
        householdId: String,
        tokens: List<String>
    ): List<FamilyMember> = withContext(Dispatchers.IO) {
        var lastError: String? = null
        for (token in tokens) {
            try {
                val response = get("${ApiConfig.BASE_URL}households/$householdId/family-members", token)
                if (response != null) {
                    val array = response.optJSONArray("familyMembers")
                        ?: return@withContext emptyList()
                    return@withContext (0 until array.length()).mapNotNull { i ->
                        val obj = array.optJSONObject(i) ?: return@mapNotNull null
                        val id = obj.optString("id").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                        FamilyMember(
                            id = id,
                            name = obj.optNullableString("name") ?: "Member",
                            email = obj.optNullableString("email"),
                            avatarUrl = obj.optNullableString("avatar")
                                ?: obj.optNullableString("photo")
                                ?: obj.optNullableString("avatar_url"),
                            birthDate = parseBirthDate(obj)
                        )
                    }
                }
            } catch (e: Exception) {
                lastError = e.message
                log("fetchFamilyMembers failed with token attempt: ${e.message}")
            }
        }
        throw Exception(lastError ?: "All token attempts failed for family-members endpoint")
    }

    private suspend fun persistMembers(householdId: String, members: List<FamilyMember>) {
        val now = Instant.now()
        members.forEach { member ->
            userRepository.saveUser(
                User(
                    id = member.id,
                    householdId = householdId,
                    email = sanitizePersistedEmail(member.id, member.email),
                    displayName = member.name,
                    avatarUrl = member.avatarUrl,
                    birthDate = member.birthDate,
                    role = UserRole.ADHD_USER,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    private fun get(url: String, token: String): JSONObject? {
        log("GET $url")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()
        val response = http.newCall(request).execute()
        log("GET $url -> ${response.code}")
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key)
            .trim()
            .takeIf { value ->
                value.isNotEmpty() &&
                    !value.equals("null", ignoreCase = true) &&
                    !value.equals("undefined", ignoreCase = true)
            }

    private fun sanitizePersistedEmail(memberId: String, rawEmail: String?): String {
        val normalized = rawEmail
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase()
        return if (normalized != null && EMAIL_PATTERN.matches(normalized)) {
            normalized
        } else {
            "$memberId@kinspace.family"
        }
    }

    private fun extractClaim(token: String, claim: String): String? = runCatching {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
        JSONObject(payload).optString(claim).takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun parseBirthDate(obj: JSONObject): LocalDate? {
        val raw = listOf("birthdate", "birthDate", "birthday", "dateOfBirth", "dob")
            .firstNotNullOfOrNull { key ->
                obj.optString(key).takeIf { it.isNotBlank() && it.lowercase() != "null" }
            }
            ?: return null

        val normalized = raw.take(10)
        return runCatching { LocalDate.parse(normalized) }.getOrNull()
    }

    private fun log(msg: String) = Log.d("MemberSelectionVM", msg)

    companion object {
        private val EMAIL_PATTERN = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
