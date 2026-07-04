package com.flowtimer.app.data

import com.flowtimer.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

/**
 * Single shared Supabase client for the app.
 *
 * Backed by the shared Perspective Library Supabase project. URL/anon key
 * are public, client-safe values (see app/build.gradle.kts) - never put
 * the service_role key here.
 */
object SupabaseClientProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
}
