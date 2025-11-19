package com.thomasalfa.photobooth.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object CloudShareManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Tambah waktu timeout biar gak putus
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // 1. Upload ke Litterbox (Gratis, Expire 1 Jam)
    suspend fun uploadFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            Log.d("KUBIKCAM", "Uploading to Litterbox: ${file.name}")

            // Setup Request untuk Litterbox
            // API: https://litterbox.catbox.moe/resources/internals/api.php
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("time", "1h") // File otomatis HILANG dalam 1 jam (Aman!)
                .addFormDataPart(
                    "fileToUpload",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://litterbox.catbox.moe/resources/internals/api.php")
                .post(requestBody)
                .build()

            // Eksekusi
            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && responseString != null) {
                // Litterbox langsung balikin URL (bukan JSON)
                // Contoh: https://litter.catbox.moe/xyz.jpg
                if (responseString.startsWith("http")) {
                    Log.d("KUBIKCAM", "Upload Sukses: $responseString")
                    return@withContext responseString
                } else {
                    Log.e("KUBIKCAM", "Response aneh: $responseString")
                }
            } else {
                Log.e("KUBIKCAM", "Upload Gagal Code: ${response.code}")
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e("KUBIKCAM", "Upload Error: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    // 2. Generate QR Code (Tidak berubah)
    suspend fun generateQrCode(content: String): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}