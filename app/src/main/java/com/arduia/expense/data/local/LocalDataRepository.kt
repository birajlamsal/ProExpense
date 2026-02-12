package com.arduia.expense.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LocalDataRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val backupDao: BackupDao
) {
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        backupDao.clearAll()
        expenseDao.clearAll()
    }
}
