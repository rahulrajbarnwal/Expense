package com.icit.expense.di

import android.content.Context
import com.icit.expense.data.local.dao.BudgetDao
import com.icit.expense.data.local.dao.CategoryDao
import com.icit.expense.data.local.dao.FeedbackDao
import com.icit.expense.data.local.dao.TransactionDao
import com.icit.expense.data.repository.BudgetRepositoryImpl
import com.icit.expense.data.repository.CategoryRepositoryImpl
import com.icit.expense.data.repository.FeedbackRepositoryImpl
import com.icit.expense.data.repository.SettingsRepositoryImpl
import com.icit.expense.data.repository.TransactionRepositoryImpl
import com.icit.expense.domain.repository.BudgetRepository
import com.icit.expense.domain.repository.CategoryRepository
import com.icit.expense.domain.repository.FeedbackRepository
import com.icit.expense.domain.repository.SettingsRepository
import com.icit.expense.domain.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(
        dao: TransactionDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): TransactionRepository {
        return TransactionRepositoryImpl(dao, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        dao: CategoryDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): CategoryRepository {
        return CategoryRepositoryImpl(dao, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        dao: BudgetDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): BudgetRepository {
        return BudgetRepositoryImpl(dao, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        dao: FeedbackDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): FeedbackRepository {
        return FeedbackRepositoryImpl(dao, firestore, auth)
    }
}

/*
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Ye rule 'users' folder ke andar har cheez ko private rakhta hai
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}

rules_version = '2';
    service cloud.firestore {
      match /databases/{database}/documents {
        match /users/{userId}/transactions/{transactionId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
      }
      }

* */