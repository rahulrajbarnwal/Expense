package com.icit.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.model.FeedbackCategory
import com.icit.expense.domain.model.FeedbackLimits
import com.icit.expense.domain.model.FeedbackResult
import com.icit.expense.domain.usecase.DeleteFeedbackUseCase
import com.icit.expense.domain.usecase.GetFeedbackDraftUseCase
import com.icit.expense.domain.usecase.GetFeedbackHistoryUseCase
import com.icit.expense.domain.usecase.RetryUnsentFeedbackUseCase
import com.icit.expense.domain.usecase.SaveFeedbackDraftUseCase
import com.icit.expense.domain.usecase.SubmitFeedbackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val draftId: Long = 0,
    val subject: String = "",
    val message: String = "",
    val category: FeedbackCategory = FeedbackCategory.FEEDBACK,
    val subjectError: String? = null,
    val messageError: String? = null,
    val isLoadingDraft: Boolean = true,
    val isSubmitting: Boolean = false,
    val lastDraftSavedAt: Long? = null
) {
    val canSubmit: Boolean
        get() = !isSubmitting && subject.isNotBlank() && message.isNotBlank()
}

/** One-shot outcomes the screen turns into a snackbar. */
sealed interface FeedbackEvent {
    data class Sent(val messageId: String) : FeedbackEvent
    data class Queued(val reason: String) : FeedbackEvent
    data class Failed(val reason: String) : FeedbackEvent
    data object DraftSaved : FeedbackEvent
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
    private val saveFeedbackDraftUseCase: SaveFeedbackDraftUseCase,
    private val getFeedbackDraftUseCase: GetFeedbackDraftUseCase,
    private val getFeedbackHistoryUseCase: GetFeedbackHistoryUseCase,
    private val retryUnsentFeedbackUseCase: RetryUnsentFeedbackUseCase,
    private val deleteFeedbackUseCase: DeleteFeedbackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FeedbackEvent> = _events

    val history: StateFlow<List<Feedback>> = getFeedbackHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Set whenever the form changes and cleared by an autosave, so an idle screen writes nothing. */
    private var isDraftDirty = false

    init {
        viewModelScope.launch { restoreDraft() }
        // Anything that never reached the server gets another chance every time the screen opens.
        viewModelScope.launch { retryUnsentFeedbackUseCase() }
        viewModelScope.launch { autoSaveDrafts() }
    }

    private suspend fun restoreDraft() {
        val draft = getFeedbackDraftUseCase()
        _uiState.update { state ->
            if (draft == null) {
                state.copy(isLoadingDraft = false)
            } else {
                state.copy(
                    draftId = draft.id,
                    subject = draft.subject,
                    message = draft.message,
                    category = draft.category,
                    isLoadingDraft = false
                )
            }
        }
    }

    private suspend fun autoSaveDrafts() {
        while (viewModelScope.isActive) {
            delay(FeedbackLimits.DRAFT_AUTOSAVE_INTERVAL_MS)
            saveDraft(notify = false)
        }
    }

    fun onSubjectChange(value: String) {
        if (value.length > FeedbackLimits.SUBJECT_MAX) return
        isDraftDirty = true
        _uiState.update { it.copy(subject = value, subjectError = null) }
    }

    fun onMessageChange(value: String) {
        if (value.length > FeedbackLimits.MESSAGE_MAX) return
        isDraftDirty = true
        _uiState.update { it.copy(message = value, messageError = null) }
    }

    fun onCategoryChange(category: FeedbackCategory) {
        isDraftDirty = true
        _uiState.update { it.copy(category = category) }
    }

    /** Explicit "Save draft" tap. [notify] is false for the background autosave. */
    fun saveDraftNow() {
        viewModelScope.launch { saveDraft(notify = true) }
    }

    private suspend fun saveDraft(notify: Boolean) {
        val state = _uiState.value
        val isEmpty = state.subject.isBlank() && state.message.isBlank()
        if (state.isSubmitting || (!isDraftDirty && !notify) || isEmpty) return

        val id = saveFeedbackDraftUseCase(
            Feedback(
                id = state.draftId,
                subject = state.subject,
                message = state.message,
                category = state.category
            )
        )
        isDraftDirty = false
        _uiState.update { it.copy(draftId = id, lastDraftSavedAt = System.currentTimeMillis()) }
        if (notify) _events.tryEmit(FeedbackEvent.DraftSaved)
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val subjectError = validateSubject(state.subject)
        val messageError = validateMessage(state.message)
        if (subjectError != null || messageError != null) {
            _uiState.update { it.copy(subjectError = subjectError, messageError = messageError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            // The draft row is reused, so the message is never duplicated in history.
            val result = submitFeedbackUseCase(
                Feedback(
                    id = state.draftId,
                    subject = state.subject.trim(),
                    message = state.message.trim(),
                    category = state.category
                )
            )
            isDraftDirty = false
            when (result) {
                is FeedbackResult.Sent -> {
                    clearForm()
                    _events.tryEmit(FeedbackEvent.Sent(result.remoteId))
                }
                is FeedbackResult.Queued -> {
                    clearForm()
                    _events.tryEmit(FeedbackEvent.Queued(result.reason))
                }
                is FeedbackResult.Error -> {
                    // Keep the text on screen so the user can copy it or try again.
                    _uiState.update { it.copy(isSubmitting = false, draftId = 0) }
                    _events.tryEmit(FeedbackEvent.Failed(result.message))
                }
            }
        }
    }

    fun retryUnsent() {
        viewModelScope.launch { retryUnsentFeedbackUseCase() }
    }

    fun deleteFeedback(id: Long) {
        viewModelScope.launch { deleteFeedbackUseCase(id) }
    }

    private fun clearForm() {
        _uiState.update {
            FeedbackUiState(isLoadingDraft = false, category = it.category)
        }
    }

    private fun validateSubject(subject: String): String? {
        val trimmed = subject.trim()
        return when {
            trimmed.isEmpty() -> "Subject is required"
            trimmed.length < FeedbackLimits.SUBJECT_MIN ->
                "At least ${FeedbackLimits.SUBJECT_MIN} characters"
            else -> null
        }
    }

    private fun validateMessage(message: String): String? {
        val trimmed = message.trim()
        return when {
            trimmed.isEmpty() -> "Message is required"
            trimmed.length < FeedbackLimits.MESSAGE_MIN ->
                "At least ${FeedbackLimits.MESSAGE_MIN} characters"
            else -> null
        }
    }
}
