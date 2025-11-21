package com.thomasalfa.photobooth.utils

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

// Model Data (raw_photos_urls boleh null dan kita acuhkan)
@Serializable
data class SessionTable(
    @SerialName("session_uuid") val sessionUuid: String,
    @SerialName("final_photo_url") val finalPhotoUrl: String? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
    @SerialName("raw_photos_urls") val rawPhotosUrls: String? = null
)

@Serializable
data class UpdateSessionTable(
    @SerialName("final_photo_url") val finalPhotoUrl: String
)

object SupabaseManager {

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

    suspend fun uploadFile(file: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("public-photos")
                val fileName = "${UUID.randomUUID()}_${file.name}"
                val bytes = file.readBytes()
                bucket.upload(fileName, bytes)
                bucket.publicUrl(fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // REVISI: Hapus parameter rawPhotoUrls
    suspend fun insertInitialSession(uuid: String, gifUrl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = SessionTable(
                    sessionUuid = uuid,
                    finalPhotoUrl = null,
                    gifUrl = gifUrl,
                    rawPhotosUrls = null // Kita biarkan null (kosong)
                )
                client.from("sessions").insert(data)
                Log.d("SUPABASE", "Initial DB Insert Success")
                true
            } catch (e: Exception) {
                Log.e("SUPABASE", "Initial DB Failed: ${e.message}")
                false
            }
        }
    }

    suspend fun updateFinalSession(uuid: String, finalPhotoUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = UpdateSessionTable(finalPhotoUrl = finalPhotoUrl)
                client.from("sessions").update(updateData) {
                    filter { SessionTable::sessionUuid eq uuid }
                }
                Log.d("SUPABASE", "Final DB Update Success")
                true
            } catch (e: Exception) {
                Log.e("SUPABASE", "Final DB Update Failed: ${e.message}")
                false
            }
        }
    }
}