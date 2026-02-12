package com.arduia.expense.data.remote.supabase

import com.arduia.expense.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    suspend fun signUp(email: String, password: String, username: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val payload = buildJsonObject {
                    put("id", userId)
                    put("username", username)
                }
                supabase.from("profiles").insert(payload)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
