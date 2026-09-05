package com.icit.expense.domain.usecase

import com.icit.expense.domain.model.Transaction
import com.icit.expense.domain.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class AddTransactionUseCaseTest {

    @Mock
    private lateinit var repository: TransactionRepository

    private lateinit var addTransactionUseCase: AddTransactionUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        addTransactionUseCase = AddTransactionUseCase(repository)
    }

    @Test
    fun `invoke should call insertTransaction on repository`() = runTest {
        val transaction = Transaction(
            id = 1,
            title = "Test",
            amount = 10.0,
            category = "Test",
            date = 123456L
        )

        addTransactionUseCase(transaction)

        verify(repository).insertTransaction(transaction)
    }
}
