package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.thomasalfa.photobooth.utils.gif.AnimatedGifEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object GifProcessor {

    fun generateBoomerangGif(
        context: Context,
        framePaths: List<String>,
        delayMs: Int = 100 // Percepat dikit biar smooth (standar 80-100ms)
    ): String? {
        if (framePaths.isEmpty()) return null

        try {
            val bos = ByteArrayOutputStream()
            val encoder = AnimatedGifEncoder()

            encoder.start(bos)
            encoder.setDelay(delayMs)
            encoder.setRepeat(0) // 0 artinya looping forever

            // SETTING KUALITAS
            // 10 = default, 1 = best (lambat), 20 = fast (burik).
            // Kita pakai 10 atau 5 biar balance.
            encoder.setQuality(10)

            // --- FUNGSI PEMBANTU: Load & Resize ---
            // Kita targetkan lebar GIF sekitar 600-720px sudah cukup tajam untuk HP
            // tapi file size tetap masuk akal.
            fun loadAndResize(path: String): Bitmap? {
                try {
                    // 1. Cek dimensi asli dulu
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, options)

                    // 2. Hitung skala biar ga kegedean (misal target width 720px)
                    val targetWidth = 720
                    var sampleSize = 1
                    while (options.outWidth / sampleSize > targetWidth) {
                        sampleSize *= 2
                    }

                    // 3. Decode beneran dengan sample size yang aman
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }
                    val rawBitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return null

                    // 4. Resize presisi ke target width (misal 720px) biar tajam (Filtering = true)
                    val scaleFactor = targetWidth.toFloat() / rawBitmap.width.toFloat()
                    val matrix = Matrix().apply { postScale(scaleFactor, scaleFactor) }

                    val resizedBitmap = Bitmap.createBitmap(
                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                    )

                    if (rawBitmap != resizedBitmap) {
                        rawBitmap.recycle()
                    }

                    return resizedBitmap
                } catch (e: Exception) {
                    return null
                }
            }

            // --- LOGIC LOOPING (RAM FRIENDLY) ---
            // Jangan simpan semua bitmap di List! Proses satu-satu.

            // 1. Loop Maju (Forward)
            for (path in framePaths) {
                val bmp = loadAndResize(path)
                if (bmp != null) {
                    encoder.addFrame(bmp)
                    bmp.recycle() // PENTING: Langsung buang dari RAM
                }
            }

            // 2. Loop Mundur (Backward) - Boomerang Effect
            // Kita skip foto terakhir dan pertama biar ga ada frame ganda (stutter)
            if (framePaths.size > 2) {
                for (i in framePaths.size - 2 downTo 1) {
                    val bmp = loadAndResize(framePaths[i])
                    if (bmp != null) {
                        encoder.addFrame(bmp)
                        bmp.recycle() // PENTING: Langsung buang dari RAM
                    }
                }
            }

            encoder.finish()

            // Simpan ke File
            val gifFileName = "kubik_boom_${System.currentTimeMillis()}.gif"
            val gifFile = File(context.cacheDir, gifFileName)

            val fos = FileOutputStream(gifFile)
            fos.write(bos.toByteArray())
            fos.flush()
            fos.close()
            bos.close()

            return gifFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}