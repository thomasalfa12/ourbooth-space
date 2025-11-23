package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jcodec.api.android.AndroidSequenceEncoder
import java.io.File

object VideoProcessor {

    private const val TAG = "DEBUG_KUBIK"

    suspend fun generateStopMotion(
        context: Context,
        photoPaths: List<String>
    ): String? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🎬 JCodec: Start generating ULTRA HD video...")

            if (photoPaths.isEmpty()) return@withContext null

            val videoFileName = "kubik_motion_${System.currentTimeMillis()}.mp4"
            val videoFile = File(context.cacheDir, videoFileName)
            if (videoFile.exists()) videoFile.delete()

            var encoder: AndroidSequenceEncoder? = null

            try {
                // 1. Buat Encoder (MP4 H.264)
                encoder = AndroidSequenceEncoder.createSequenceEncoder(videoFile, 4) // 4 FPS

                // 2. Loop Foto (2x Putaran)
                val loopPaths = photoPaths + photoPaths

                loopPaths.forEachIndexed { index, path ->
                    // --- LOGIC ULTRA QUALITY ---
                    // Kita coba load Full Size (inSampleSize = 1)
                    // Tapi kita bungkus try-catch khusus OutOfMemoryError

                    var bitmap = try {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = 1 // FULL RESOLUTION (ORIGINAL)
                        }
                        BitmapFactory.decodeFile(path, options)
                    } catch (e: OutOfMemoryError) {
                        Log.w(TAG, "⚠️ RAM Penuh! Turunkan kualitas dikit...")
                        // Fallback: Kalau RAM jebol, kita kecilkan 50% (Masih HD)
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = 2
                        }
                        BitmapFactory.decodeFile(path, options)
                    }

                    if (bitmap != null) {
                        encoder.encodeImage(bitmap)
                        Log.d(TAG, "Frame ${index + 1} encoded (High Res)")

                        // PENTING: Langsung buang dari RAM setelah dipakai
                        bitmap.recycle()
                    } else {
                        Log.e(TAG, "Failed to decode: $path")
                    }
                }

                // 3. Selesai
                encoder.finish()

                Log.d(TAG, "✅ JCodec SUCCESS: ${videoFile.absolutePath} Size: ${videoFile.length() / 1024} KB")
                return@withContext videoFile.absolutePath

            } catch (e: Exception) {
                Log.e(TAG, "❌ JCodec Failed: ${e.message}")
                e.printStackTrace()
                return@withContext null
            } finally {
                // JCodec finish() sudah menutup file
            }
        }
    }
}