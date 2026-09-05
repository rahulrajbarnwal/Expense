package com.icit.expense.data.repository

import android.util.Log
import com.icit.expense.data.local.dao.TransactionDao
import com.icit.expense.data.mapper.toDomain
import com.icit.expense.data.mapper.toEntity
import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private val TAG = "FirestoreSync"

    private val userCollection get() = auth.currentUser?.uid?.let { uid ->
        Log.d(TAG, "Syncing for user: $uid")
        firestore.collection("users").document(uid).collection("transactions")
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalSpent(): Flow<Double?> {
        return transactionDao.getTotalSpent()
    }

    override fun getTotalSpentByDateRange(start: Long, end: Long): Flow<Double?> {
        return transactionDao.getTotalSpentByDateRange(start, end)
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        // Save locally first and get the generated ID if it was 0
        val rowId = transactionDao.insertTransaction(transaction.toEntity())
        val finalTransaction = if (transaction.id == 0L) transaction.copy(id = rowId) else transaction

        // Sync to Firestore if user is logged in
        val collection = userCollection
        if (collection == null) {
            Log.w(TAG, "Sync failed: User not logged in")
            return
        }

        try {
            Log.d(TAG, "Attempting to save to Firestore: ${finalTransaction.id}")
            collection.document(finalTransaction.id.toString()).set(finalTransaction).await()
            Log.d(TAG, "Successfully saved to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to Firestore: ${e.message}", e)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
        
        userCollection?.let { collection ->
            try {
                collection.document(transaction.id.toString()).delete().await()
                Log.d(TAG, "Successfully deleted from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting from Firestore: ${e.message}", e)
            }
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
        
        val collection = userCollection
        if (collection == null) return

        try {
            collection.document(transaction.id.toString()).set(transaction).await()
            Log.d(TAG, "Successfully updated in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Firestore: ${e.message}", e)
        }
    }

    override suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
        Log.d(TAG, "Locally cleared all transactions on logout")
    }

    override suspend fun syncFromFirestore() {
        userCollection?.let { collection ->
            try {
                val snapshot = collection.get().await()
                val transactions = snapshot.toObjects(Transaction::class.java)
                transactions.forEach { transaction ->
                    transactionDao.insertTransaction(transaction.toEntity())
                }
                Log.d(TAG, "Successfully synced ${transactions.size} transactions from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from Firestore: ${e.message}", e)
            }
        }
    }
}
