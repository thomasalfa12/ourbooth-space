package com.thomasalfa.photobooth.utils

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
// Import yang BENAR untuk Serialization
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Model Data (Disesuaikan dengan Kolom Database Supabase Anda)
@Serializable
data class SessionTable(
    @SerialName("session_uuid") val sessionUuid: String,       // Di DB: session_uuid
    @SerialName("final_photo_url") val finalPhotoUrl: String,  // Di DB: final_photo_url
    @SerialName("gif_url") val gifUrl: String?                 // Di DB: gif_url
)

object SupabaseManager {

    // GANTI DENGAN DATA ASLI ANDA
    private const val SUPABASE_URL = "https://xrbepnwafkbrvyncxqku.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhyYmVwbndhZmticnZ5bmN4cWt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM3MjAxMjYsImV4cCI6MjA3OTI5NjEyNn0.K1rbpv_Dduroh-_-mMSHQGdI1oClqNMpjl0j-t3ei1k"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage) {
            transferTimeout = 3.minutes
        }
    }

    // Fungsi Upload File
    suspend fun uploadFile(file: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("public-photos")
                val fileName = "${UUID.randomUUID()}_${file.name}"
                val bytes = file.readBytes()

                bucket.upload(fileName, bytes)

                val url = bucket.publicUrl(fileName)
                Log.d("SUPABASE", "Upload Success: $url")
                url
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SUPABASE", "Upload Failed: ${e.message}")
                null
            }
        }
    }

    // Fungsi Insert ke Database
    suspend fun insertSession(uuid: String, photoUrl: String, gifUrl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = SessionTable(
                    sessionUuid = uuid,
                    finalPhotoUrl = photoUrl,
                    gifUrl = gifUrl
                )
                // Masukkan data ke tabel 'sessions'
                client.from("sessions").insert(data)
                Log.d("SUPABASE", "DB Insert Success")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SUPABASE", "DB Insert Failed: ${e.message}")
                false
            }
        }
    }
}