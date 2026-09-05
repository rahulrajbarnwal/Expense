package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.updateTransaction(transaction)
    }
}
