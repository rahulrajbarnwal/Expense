package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.repository.FeedbackRepository
import javax.inject.Inject

class SaveFeedbackDraftUseCase @Inject constructor(private val repository: FeedbackRepository) {
    suspend operator fun invoke(feedback: Feedback): Long = repository.saveDraft(feedback)
}
