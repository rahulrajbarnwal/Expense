package com.icit.expense.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Long = 0,
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: Long = 0
)
