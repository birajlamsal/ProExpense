package com.arduia.expense.data.remote.supabase

import com.arduia.expense.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

@Serializable
data class RemoteProfile(
    val id: String,
    val username: String
)

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun fetchUserName(): Result<String> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"))

            val profile = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingle<RemoteProfile>()

            Result.Success(profile.username)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
