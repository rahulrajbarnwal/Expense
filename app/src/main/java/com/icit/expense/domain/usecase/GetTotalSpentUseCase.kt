package com.icit.expense.domain.usecase

import com.icit.expense.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTotalSpentUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<Double?> {
        return repository.getTotalSpent()
    }

    fun getByDateRange(start: Long, end: Long): Flow<Double?> {
        return repository.getTotalSpentByDateRange(start, end)
    }
}
