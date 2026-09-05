package com.icit.expense.ui

import androidx.navigation3.runtime.NavKey
import com.icit.expense.domain.model.Transaction
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination : NavKey {
    @Serializable
    data object Login : NavDestination

    @Serializable
    data object ExpenseHome : NavDestination
    
    @Serializable
    data class AddExpense(val transaction: Transaction? = null) : NavDestination

    @Serializable
    data object History : NavDestination

    @Serializable
    data object Profile : NavDestination

    @Serializable
    data object CategoryManagement : NavDestination

    @Serializable
    data object BudgetPlanner : NavDestination

    @Serializable
    data object Analytics : NavDestination

    @Serializable
    data object Notifications : NavDestination

    @Serializable
    data object Lock : NavDestination

    @Serializable
    data object Security : NavDestination

    @Serializable
    data object Feedback : NavDestination
}
