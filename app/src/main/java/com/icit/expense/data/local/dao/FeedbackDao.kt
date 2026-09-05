package com.icit.expense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.icit.expense.data.local.entity.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    /** Everything the user has actually submitted, newest first. Drafts are excluded — they live in the form. */
    @Query("SELECT * FROM feedback WHERE status != 'DRAFT' ORDER BY updatedAt DESC")
    fun getSubmittedFeedback(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getDraft(): FeedbackEntity?

    /** Submitted messages that never reached the server. Retried on every screen open. */
    @Query("SELECT * FROM feedback WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getUnsent(): List<FeedbackEntity>

    @Query("SELECT * FROM feedback WHERE id = :id")
    suspend fun getById(id: Long): FeedbackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(feedback: FeedbackEntity): Long

    @Query("DELETE FROM feedback WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM feedback WHERE status = 'DRAFT'")
    suspend fun deleteDrafts()

    @Query("DELETE FROM feedback")
    suspend fun deleteAllFeedback()
}
