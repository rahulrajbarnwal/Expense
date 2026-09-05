package com.icit.expense.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.icit.expense.data.local.dao.BudgetDao
import com.icit.expense.data.local.dao.CategoryDao
import com.icit.expense.data.local.dao.FeedbackDao
import com.icit.expense.data.local.dao.TransactionDao
import com.icit.expense.data.local.entity.BudgetEntity
import com.icit.expense.data.local.entity.CategoryBudgetEntity
import com.icit.expense.data.local.entity.CategoryEntity
import com.icit.expense.data.local.entity.FeedbackEntity
import com.icit.expense.data.local.entity.TransactionEntity

@Database(entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, CategoryBudgetEntity::class, FeedbackEntity::class], version = 4, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val categoryDao: CategoryDao
    abstract val budgetDao: BudgetDao
    abstract val feedbackDao: FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        /**
         * Adds the feedback table. Written out rather than falling back to a destructive migration
         * so users who skipped login (and therefore have no Firestore backup) keep their transactions.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `userId` TEXT,
                        `userEmail` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `remoteId` TEXT,
                        `lastError` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration() // Backstop for the older, un-migrated versions
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
