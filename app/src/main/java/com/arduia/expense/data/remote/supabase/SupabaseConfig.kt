package com.arduia.expense.data.remote.supabase

import com.arduia.expense.BuildConfig

object SupabaseConfig {
    val url: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    fun validate() {
        require(url.isNotBlank()) {
            "Supabase URL is missing. Set supabase.url in local.properties."
        }
        require(anonKey.isNotBlank()) {
            "Supabase anon key is missing. Set supabase.anonKey in local.properties."
        }
    }
}
