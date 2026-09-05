package com.icit.expense.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.icit.expense.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT SUM(amount) FROM transactions")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    fun getTransactionsByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getTotalSpentByDateRange(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
