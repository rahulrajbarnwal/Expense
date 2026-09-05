package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.repository.FeedbackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedbackHistoryUseCase @Inject constructor(private val repository: FeedbackRepository) {
    operator fun invoke(): Flow<List<Feedback>> = repository.getSubmittedFeedback()
}
