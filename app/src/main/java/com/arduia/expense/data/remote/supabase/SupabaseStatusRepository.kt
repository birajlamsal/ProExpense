package com.arduia.expense.data.remote.supabase

import com.arduia.expense.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseStatusRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    suspend fun checkConnection(): Result<String> {
        return try {
            // Small, harmless query to verify connectivity.
            supabase.from("profiles").select {
                limit(1)
            }
            Result.Success("Connected")
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val authLikely = msg.contains("jwt", ignoreCase = true) ||
                msg.contains("unauthorized", ignoreCase = true) ||
                msg.contains("permission", ignoreCase = true)
            if (authLikely) {
                Result.Success("Connected (auth required)")
            } else {
                Result.Error(e)
            }
        }
    }
}
