package com.thomasalfa.photobooth.utils

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import io.github.jan.supabase.postgrest.rpc

// --- MODEL DATA ---

// Model Akun Device (Untuk Login)
@Serializable
data class DeviceTable(
    val id: String,
    val name: String,
    val type: String,
    val status: String
)

// Model Sesi Foto (Raw Photos menggantikan Video)
@Serializable
data class SessionTable(
    @SerialName("session_uuid") val sessionUuid: String,
    @SerialName("final_photo_url") val finalPhotoUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null, // Opsional/Legacy
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("raw_photos_urls") val rawPhotosUrls: String? = null // JSON String
)

// Model untuk Update Foto Final
@Serializable
data class UpdatePhotoTable(
    @SerialName("final_photo_url") val finalPhotoUrl: String
)

// Model untuk Update Raw Photos (Batch)
@Serializable
data class UpdateRawPhotosTable(
    @SerialName("raw_photos_urls") val rawPhotosUrls: String
)

@Serializable
data class RedeemParams(
    val input_code: String,
    val input_device_id: String
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
            transferTimeout = 5.minutes // Timeout lebih lama untuk bulk upload
        }
    }

    // --- 1. AUTHENTICATION ---
    suspend fun loginDevice(username: String, pin: String): DeviceTable? {
        return withContext(Dispatchers.IO) {
            try {
                // Query ke tabel 'devices'
                val result = client.from("devices").select {
                    filter {
                        eq("username", username)
                        eq("pin_code", pin)
                    }
                }.decodeSingleOrNull<DeviceTable>()

                // Cek Validitas
                if (result != null && result.status == "ACTIVE") {
                    result
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("SUPABASE", "Login Error: ${e.message}")
                null
            }
        }
    }

    suspend fun redeemTicket(code: String, deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Panggil RPC 'redeem_ticket' yang baru kita buat di SQL
                val result = client.postgrest.rpc(
                    "redeem_ticket",
                    RedeemParams(input_code = code, input_device_id = deviceId)
                ).decodeAs<Boolean>()

                result
            } catch (e: Exception) {
                Log.e("SUPABASE", "Redeem Failed: ${e.message}")
                false
            }
        }
    }

    // --- 2. STORAGE (SINGLE & BATCH) ---

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

    // [NEW] Upload Banyak File Sekaligus (Parallel Async)
    suspend fun uploadMultipleFiles(files: List<File>): List<String> {
        return withContext(Dispatchers.IO) {
            val uploadJobs = files.map { file ->
                async {
                    uploadFile(file)
                }
            }
            // Tunggu semua selesai, ambil yang sukses saja
            uploadJobs.awaitAll().filterNotNull()
        }
    }

    // --- 3. DATABASE OPERATIONS ---

    // Insert Awal Sesi
    suspend fun insertInitialSession(uuid: String, videoUrl: String?, deviceId: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = SessionTable(
                    sessionUuid = uuid,
                    finalPhotoUrl = null,
                    videoUrl = videoUrl,
                    deviceId = deviceId, // Link ke Device
                    rawPhotosUrls = null
                )
                client.from("sessions").insert(data)
                Log.d("SUPABASE", "✅ Initial DB Insert Success")
                true
            } catch (e: Exception) {
                Log.e("SUPABASE", "❌ Initial DB Failed: ${e.message}")
                false
            }
        }
    }

    // Update Raw Photos (JSON Array)
    suspend fun updateSessionRawPhotos(uuid: String, photoUrls: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                // Convert List String -> JSON String
                val jsonString = Json.encodeToString(photoUrls)
                val updateData = UpdateRawPhotosTable(rawPhotosUrls = jsonString)

                client.from("sessions").update(updateData) {
                    filter { SessionTable::sessionUuid eq uuid }
                }
                Log.d("SUPABASE", "✅ Raw Photos Linked")
            } catch (e: Exception) {
                Log.e("SUPABASE", "❌ Raw Photos Link Failed: ${e.message}")
            }
        }
    }

    // Update Foto Final (Layout)
    suspend fun updateFinalSession(uuid: String, finalPhotoUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = UpdatePhotoTable(finalPhotoUrl = finalPhotoUrl)
                client.from("sessions").update(updateData) {
                    filter { SessionTable::sessionUuid eq uuid }
                }
                Log.d("SUPABASE", "✅ Final Photo Update Success")
                true
            } catch (e: Exception) {
                Log.e("SUPABASE", "❌ Final Photo Update Failed: ${e.message}")
                false
            }
        }
    }

    // Legacy support (biar ga error kalau masih ada yang panggil)
    suspend fun updateSessionVideo(uuid: String, videoUrl: String) {
        // Kosongkan atau implementasikan jika masih butuh video kedepannya
    }
}