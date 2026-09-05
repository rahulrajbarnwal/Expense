package com.icit.expense.data.mapper

import com.icit.expense.data.local.entity.FeedbackEntity
import com.icit.expense.domain.model.Feedback
import com.icit.expense.domain.model.FeedbackCategory
import com.icit.expense.domain.model.FeedbackStatus

fun FeedbackEntity.toDomain(): Feedback {
    return Feedback(
        id = id,
        subject = subject,
        message = message,
        // Unknown values can only come from a hand-edited DB; fall back instead of crashing.
        category = FeedbackCategory.entries.firstOrNull { it.name == category } ?: FeedbackCategory.GENERAL,
        status = FeedbackStatus.entries.firstOrNull { it.name == status } ?: FeedbackStatus.DRAFT,
        userId = userId,
        userEmail = userEmail,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteId = remoteId,
        lastError = lastError
    )
}

fun Feedback.toEntity(): FeedbackEntity {
    return FeedbackEntity(
        id = id,
        subject = subject,
        message = message,
        category = category.name,
        status = status.name,
        userId = userId,
        userEmail = userEmail,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteId = remoteId,
        lastError = lastError
    )
}
