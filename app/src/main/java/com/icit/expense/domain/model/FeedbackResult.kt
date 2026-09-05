package com.icit.expense.domain.model

/** Outcome of a submit attempt. The message is always stored locally first, so every branch has a local id. */
sealed interface FeedbackResult {
    val localId: Long

    /** Reached the server; [remoteId] is the id the user can quote in follow-ups. */
    data class Sent(override val localId: Long, val remoteId: String) : FeedbackResult

    /** No network. Kept in the local queue and retried the next time the screen opens. */
    data class Queued(override val localId: Long, val reason: String) : FeedbackResult

    /** Server was reachable but rejected the write after all retries. */
    data class Error(override val localId: Long, val message: String) : FeedbackResult
}
