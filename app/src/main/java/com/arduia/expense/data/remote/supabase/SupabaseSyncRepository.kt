package com.arduia.expense.data.remote.supabase

import com.arduia.expense.data.SettingsRepository
import com.arduia.expense.data.local.ExpenseDao
import com.arduia.expense.data.local.ExpenseEnt
import com.arduia.expense.domain.Amount
import com.arduia.expense.model.Result
import com.arduia.expense.model.getDataOrError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Serializable
data class RemoteExpense(
    val id: String? = null,
    val user_id: String? = null,
    val name: String? = null,
    val amount: Long,
    val category: Int,
    val note: String? = null,
    val created_date: Long,
    val modified_date: Long,
    val deleted_at: String? = null,
    val updated_at: String? = null
)

data class SyncResult(
    val inserted: Int,
    val skipped: Boolean
)

@Singleton
class SupabaseSyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val expenseDao: ExpenseDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun syncDownExpenses(): Result<SyncResult> {
        return try {
            val localCount = expenseDao.getExpenseTotalCountSync()
            if (localCount > 0) {
                return Result.Success(SyncResult(inserted = 0, skipped = true))
            }

            val remote = supabase.from("expenses")
                .select()
                .decodeList<RemoteExpense>()

            val entities = remote.map {
                ExpenseEnt(
                    expenseId = 0,
                    remoteId = it.id,
                    name = it.name,
                    amount = Amount.createFromStore(it.amount),
                    category = it.category,
                    note = it.note,
                    createdDate = it.created_date,
                    modifiedDate = it.modified_date,
                    deletedAt = null,
                    syncState = ExpenseEnt.SYNCED
                )
            }

            if (entities.isNotEmpty()) {
                expenseDao.insertExpenseAll(entities)
            }

            Result.Success(SyncResult(inserted = entities.size, skipped = false))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun syncTwoWay(): Result<SyncResult> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"))

            // Push local changes
            val pending = expenseDao.getPendingSync()
            for (local in pending) {
                when (local.syncState) {
                    ExpenseEnt.DIRTY -> {
                        val payload = RemoteExpense(
                            id = local.remoteId,
                            user_id = userId,
                            name = local.name,
                            amount = local.amount.getStore(),
                            category = local.category,
                            note = local.note,
                            created_date = local.createdDate,
                            modified_date = local.modifiedDate
                        )
                        val inserted = if (local.remoteId.isNullOrBlank()) {
                            supabase.from("expenses")
                                .insert(payload)
                                .decodeSingle<RemoteExpense>()
                        } else {
                            supabase.from("expenses")
                                .upsert(payload)
                                .decodeSingle<RemoteExpense>()
                        }

                        val remoteId = inserted?.id
                        if (!remoteId.isNullOrBlank()) {
                            expenseDao.updateRemoteIdAndSyncState(
                                local.expenseId,
                                remoteId,
                                ExpenseEnt.SYNCED
                            )
                        } else {
                            expenseDao.updateSyncState(local.expenseId, ExpenseEnt.SYNCED)
                        }
                    }
                    ExpenseEnt.DELETED -> {
                        val remoteId = local.remoteId
                        if (!remoteId.isNullOrBlank()) {
                            supabase.from("expenses")
                                .update({
                                    set("deleted_at", Instant.ofEpochMilli(local.deletedAt ?: System.currentTimeMillis()).toString())
                                }) {
                                    filter { eq("id", remoteId) }
                                }
                        }
                        expenseDao.updateSyncState(local.expenseId, ExpenseEnt.SYNCED)
                    }
                    else -> Unit
                }
            }

            // Pull remote changes
            val lastSync = settingsRepository.getLastSyncAt().getDataOrError()
            val remoteChanges = supabase.from("expenses")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RemoteExpense>()

            for (remote in remoteChanges) {
                val remoteId = remote.id ?: continue
                val existing = expenseDao.getByRemoteId(remoteId)

                val deletedAtMillis = remote.deleted_at?.let { Instant.parse(it).toEpochMilli() }
                if (deletedAtMillis != null) {
                    if (existing != null) {
                        expenseDao.markDeletedById(
                            existing.expenseId,
                            deletedAtMillis,
                            ExpenseEnt.SYNCED
                        )
                    }
                    continue
                }

                val entity = ExpenseEnt(
                    expenseId = existing?.expenseId ?: 0,
                    remoteId = remoteId,
                    name = remote.name,
                    amount = Amount.createFromStore(remote.amount),
                    category = remote.category,
                    note = remote.note,
                    createdDate = remote.created_date,
                    modifiedDate = remote.modified_date,
                    deletedAt = null,
                    syncState = ExpenseEnt.SYNCED
                )
                if (existing == null) {
                    expenseDao.insertExpense(entity)
                } else {
                    expenseDao.updateExpense(entity)
                }
            }

            settingsRepository.setLastSyncAt(System.currentTimeMillis())

            Result.Success(SyncResult(inserted = remoteChanges.size, skipped = false))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
