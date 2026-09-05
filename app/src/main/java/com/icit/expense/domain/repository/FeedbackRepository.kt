package com.icit.expense.domain.repository

import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.model.FeedbackResult
import kotlinx.coroutines.flow.Flow

interface FeedbackRepository {
    fun getSubmittedFeedback(): Flow<List<Feedback>>
    suspend fun loadDraft(): Feedback?
    suspend fun saveDraft(feedback: Feedback): Long
    suspend fun discardDraft()
    suspend fun submitFeedback(feedback: Feedback): FeedbackResult

    /** Pushes anything still queued. Returns how many reached the server. */
    suspend fun retryUnsent(): Int
    suspend fun deleteFeedback(id: Long)
    suspend fun clearAllFeedback()
}
