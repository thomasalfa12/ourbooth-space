package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jcodec.api.android.AndroidSequenceEncoder
import java.io.File
import kotlin.math.max

object VideoProcessor {

    private const val TAG = "DEBUG_KUBIK"

    // SETTINGAN ESTETIK:
    private const val VIDEO_FPS = 3 // 3 FPS = Santai & Cinematic
    private const val TARGET_WIDTH = 1080 // Kita kunci di 1080p biar ringan tapi tajam

    suspend fun generateStopMotion(
        context: Context,
        photoPaths: List<String>
    ): String? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🎬 JCodec: Start generating 1080p Estetik video...")

            if (photoPaths.isEmpty()) return@withContext null

            val videoFileName = "kubik_motion_${System.currentTimeMillis()}.mp4"
            val videoFile = File(context.cacheDir, videoFileName)
            if (videoFile.exists()) videoFile.delete()

            var encoder: AndroidSequenceEncoder? = null

            try {
                // 1. Buat Encoder
                encoder = AndroidSequenceEncoder.createSequenceEncoder(videoFile, VIDEO_FPS)

                // 2. Loop Foto (3x Putaran biar durasi panjang ~6 detik)
                val loopPaths = photoPaths + photoPaths + photoPaths

                loopPaths.forEachIndexed { index, path ->

                    // --- LOGIC SMART RESIZE (TARGET 1080p) ---
                    // Langkah A: Baca ukuran asli tanpa load gambar ke RAM
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(path, options)

                    val srcWidth = options.outWidth

                    // Langkah B: Hitung pengecilan agar mendekati 1080px
                    // Contoh: Jika asli 3000px, sampleSize = 2 (jadi 1500px).
                    // Jika 4000px, sampleSize = 4 (jadi 1000px).
                    var sampleSize = 1
                    while ((srcWidth / sampleSize) > TARGET_WIDTH) {
                        sampleSize *= 2
                    }

                    // Langkah C: Load Gambar Beneran dengan ukuran pas
                    val decodeOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = false
                        inSampleSize = sampleSize
                    }

                    // Safety: Try-Catch khusus OOM (Jaga-jaga HP kentang)
                    val bitmap = try {
                        BitmapFactory.decodeFile(path, decodeOptions)
                    } catch (e: OutOfMemoryError) {
                        Log.w(TAG, "⚠️ RAM Penuh! Paksa turun kualitas lagi...")
                        decodeOptions.inSampleSize *= 2 // Kecilkan lagi 2x lipat
                        BitmapFactory.decodeFile(path, decodeOptions)
                    }

                    if (bitmap != null) {
                        encoder.encodeImage(bitmap)
                        Log.d(TAG, "Frame ${index + 1} encoded (${bitmap.width}x${bitmap.height})")

                        // PENTING: Buang dari RAM
                        bitmap.recycle()
                    } else {
                        Log.e(TAG, "Failed to decode: $path")
                    }
                }

                // 3. Selesai
                encoder.finish()

                Log.d(TAG, "✅ VIDEO SUCCESS! Path: ${videoFile.absolutePath}")
                return@withContext videoFile.absolutePath

            } catch (e: Exception) {
                Log.e(TAG, "❌ Video Failed: ${e.message}")
                e.printStackTrace()
                return@withContext null
            } finally {
                // JCodec finish() sudah menutup file
            }
        }
    }
}