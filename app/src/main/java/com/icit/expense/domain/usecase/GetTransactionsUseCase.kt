package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getAllTransactions()
    }

    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return repository.getTransactionsByDateRange(start, end)
    }
}
