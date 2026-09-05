package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.FeedbackRepository
import javax.inject.Inject

class DeleteFeedbackUseCase @Inject constructor(private val repository: FeedbackRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteFeedback(id)
}
