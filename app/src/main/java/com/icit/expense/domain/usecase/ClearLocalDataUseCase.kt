package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.FeedbackRepository
import com.icit.expense.domain.repository.SettingsRepository
import com.icit.expense.domain.repository.TransactionRepository
import javax.inject.Inject

class ClearLocalDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke() {
        transactionRepository.clearAllTransactions()
        settingsRepository.clearSettings()
        // Drafts and message history are personal to the signed-in user.
        feedbackRepository.clearAllFeedback()
    }
}
