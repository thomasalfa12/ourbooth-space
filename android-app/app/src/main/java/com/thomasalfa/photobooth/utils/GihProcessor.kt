package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
// Import kelas lokal kita
import com.thomasalfa.photobooth.utils.gif.AnimatedGifEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object GifProcessor {

    fun generateBoomerangGif(
        context: Context,
        framePaths: List<String>,
        delayMs: Int = 100
    ): String? {
        if (framePaths.isEmpty()) return null

        try {
            val bos = ByteArrayOutputStream()
            val encoder = AnimatedGifEncoder()

            encoder.start(bos)
            encoder.setDelay(delayMs)
            encoder.setRepeat(0)

            // 1. Load & Downsample
            val bitmaps = framePaths.mapNotNull { path ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(path, options)
            }

            if (bitmaps.isEmpty()) return null

            // 2. Maju
            for (bmp in bitmaps) {
                encoder.addFrame(bmp)
            }

            // 3. Mundur (Boomerang)
            if (bitmaps.size > 2) {
                for (i in bitmaps.size - 2 downTo 1) {
                    encoder.addFrame(bitmaps[i])
                }
            }

            encoder.finish()

            // 4. Simpan
            val gifFileName = "boomerang_${System.currentTimeMillis()}.gif"
            val gifFile = File(context.cacheDir, gifFileName)

            val fos = FileOutputStream(gifFile)
            fos.write(bos.toByteArray())
            fos.flush()
            fos.close()

            // Cleanup
            bitmaps.forEach { it.recycle() }

            return gifFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}