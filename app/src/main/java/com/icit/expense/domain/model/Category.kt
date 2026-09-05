package com.icit.expense.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long = 0,
    val name: String = "",
    val isRemovable: Boolean = true
)
