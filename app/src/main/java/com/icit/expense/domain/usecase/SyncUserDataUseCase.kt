package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.CategoryRepository
import com.icit.expense.domain.repository.TransactionRepository
import javax.inject.Inject

class SyncUserDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke() {
        transactionRepository.syncFromFirestore()
        categoryRepository.syncFromFirestore()
    }
}
