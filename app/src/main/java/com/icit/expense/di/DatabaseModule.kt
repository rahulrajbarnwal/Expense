package com.icit.expense.di

import android.content.Context
import com.icit.expense.data.local.db.ExpenseDatabase
import com.icit.expense.data.local.dao.BudgetDao
import com.icit.expense.data.local.dao.CategoryDao
import com.icit.expense.data.local.dao.FeedbackDao
import com.icit.expense.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseDatabase {
        return ExpenseDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: ExpenseDatabase): TransactionDao {
        return database.transactionDao
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
        return database.categoryDao
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: ExpenseDatabase): BudgetDao {
        return database.budgetDao
    }

    @Provides
    @Singleton
    fun provideFeedbackDao(database: ExpenseDatabase): FeedbackDao {
        return database.feedbackDao
    }
}
