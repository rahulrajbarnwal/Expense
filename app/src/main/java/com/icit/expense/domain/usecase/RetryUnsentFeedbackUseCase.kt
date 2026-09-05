package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.FeedbackRepository
import javax.inject.Inject

class RetryUnsentFeedbackUseCase @Inject constructor(private val repository: FeedbackRepository) {
    /** Returns how many queued messages reached the server. */
    suspend operator fun invoke(): Int = repository.retryUnsent()
}
