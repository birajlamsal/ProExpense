package com.arduia.expense.di

import com.arduia.expense.data.remote.supabase.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        SupabaseConfig.validate()
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.url,
            supabaseKey = SupabaseConfig.anonKey
        ) {
            install(Auth) {
                // Minimal config to avoid SettingsSessionManager dependency issues.
                // Session persistence will be added once storage is wired.
                minimalConfig()
            }
            install(Postgrest)
        }
    }
}
