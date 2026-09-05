package com.icit.expense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val message: String,
    val category: String,
    val status: String,
    val userId: String?,
    val userEmail: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String?,
    val lastError: String?
)
