package com.icit.expense.data.mapper

import com.icit.expense.data.local.entity.TransactionEntity
import com.icit.expense.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date
    )
}
