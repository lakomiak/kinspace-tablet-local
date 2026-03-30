package com.adhdfocus.app.ui.common

import androidx.lifecycle.ViewModel
import com.adhdfocus.app.domain.affirmation.AffirmationEvent
import com.adhdfocus.app.domain.affirmation.AffirmationTriggerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * AffirmationViewModel manages affirmation display state.
 *
 * Responsibilities:
 * - Expose affirmation events from AffirmationTriggerManager
 * - Handle affirmation dismissal
 * - Provide affirmation state to UI components
 */
@HiltViewModel
class AffirmationViewModel @Inject constructor(
    private val affirmationTriggerManager: AffirmationTriggerManager
) : ViewModel() {

    /**
     * Current affirmation event to display.
     */
    val affirmationEvent: StateFlow<AffirmationEvent?> = affirmationTriggerManager.affirmationEvent

    /**
     * Dismisses the current affirmation.
     */
    fun dismissAffirmation() {
        affirmationTriggerManager.clearAffirmation()
    }
}
