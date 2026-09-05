package com.icit.expense.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Feedback(
    val id: Long = 0,
    val subject: String = "",
    val message: String = "",
    val category: FeedbackCategory = FeedbackCategory.FEEDBACK,
    val status: FeedbackStatus = FeedbackStatus.DRAFT,
    val userId: String? = null,
    val userEmail: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val remoteId: String? = null,
    val lastError: String? = null
)

enum class FeedbackCategory(val label: String) {
    FEEDBACK("Feedback"),
    COMPLAINT("Complaint"),
    SUGGESTION("Suggestion"),
    GENERAL("General")
}

enum class FeedbackStatus {
    /** Saved locally, never submitted. Only one draft is kept at a time. */
    DRAFT,

    /** Submitted by the user but not yet acknowledged by the server (offline queue). */
    PENDING,

    /** Acknowledged by the server. */
    SENT,

    /** Server rejected it after all retries. Retried again on next screen open. */
    FAILED
}

object FeedbackLimits {
    const val SUBJECT_MIN = 5
    const val SUBJECT_MAX = 100
    const val MESSAGE_MIN = 10
    const val MESSAGE_MAX = 500

    /** How often an edited draft is written to Room while the user types. */
    const val DRAFT_AUTOSAVE_INTERVAL_MS = 30_000L
}
