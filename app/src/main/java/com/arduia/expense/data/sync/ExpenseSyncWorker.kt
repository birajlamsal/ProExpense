package com.arduia.expense.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.arduia.expense.data.remote.supabase.SupabaseSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExpenseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SupabaseSyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        return when (val result = syncRepository.syncTwoWay()) {
            is com.arduia.expense.model.Result.Success -> ListenableWorker.Result.success()
            is com.arduia.expense.model.Result.Error -> ListenableWorker.Result.retry()
            com.arduia.expense.model.Result.Loading -> ListenableWorker.Result.retry()
        }
    }
}
