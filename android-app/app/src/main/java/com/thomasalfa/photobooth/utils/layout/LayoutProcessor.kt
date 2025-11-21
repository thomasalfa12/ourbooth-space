package com.thomasalfa.photobooth.utils.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect

object LayoutProcessor {

    fun processLayout(
        photos: List<Bitmap>,
        layoutType: String,
        frameBitmap: Bitmap?
    ): Bitmap {
        val canvasWidth = 1200
        val canvasHeight = 1800
        val resultBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        canvas.drawColor(Color.WHITE)

        if (layoutType == "STRIP") {
            drawStripLayout(canvas, photos)
        } else {
            drawGridLayout(canvas, photos)
        }

        if (frameBitmap != null) {
            val scaledFrame = Bitmap.createScaledBitmap(frameBitmap, canvasWidth, canvasHeight, true)
            canvas.drawBitmap(scaledFrame, 0f, 0f, null)
        }

        return resultBitmap
    }

    // --- PERBAIKAN DISINI: HAPUS LOGIKA MATRIX/MIRROR ---
    // Cukup resize gambar agar pas dengan lubang frame.
    // Karena foto dari CaptureScreen SUDAH di-mirror, jadi disini jangan dibalik lagi.
    private fun getScaledPhoto(original: Bitmap, targetW: Int, targetH: Int): Bitmap {
        return Bitmap.createScaledBitmap(original, targetW, targetH, true)
    }

    // --- LOGIC TIPE A: CLASSIC GRID (2x3) ---
    private fun drawGridLayout(canvas: Canvas, photos: List<Bitmap>) {
        val photoW = 520
        val photoH = 390

        val coordinates = listOf(
            Pair(60, 350), Pair(620, 350),
            Pair(60, 780), Pair(620, 780),
            Pair(60, 1210), Pair(620, 1210)
        )

        for (i in 0 until minOf(photos.size, 6)) {
            val (x, y) = coordinates[i]

            // Panggil fungsi resize biasa (tanpa mirror)
            val finalPhoto = getScaledPhoto(photos[i], photoW, photoH)

            canvas.drawBitmap(finalPhoto, x.toFloat(), y.toFloat(), null)
        }
    }

    // --- LOGIC TIPE B: PHOTO STRIP (Cut 2) ---
    private fun drawStripLayout(canvas: Canvas, photos: List<Bitmap>) {
        val photoW = 500
        val photoH = 375

        val leftCoords = listOf(Pair(50, 300), Pair(50, 705), Pair(50, 1110))
        val rightCoords = listOf(Pair(650, 300), Pair(650, 705), Pair(650, 1110))

        for (i in 0 until minOf(photos.size, 3)) {
            // Panggil fungsi resize biasa (tanpa mirror)
            val finalPhoto = getScaledPhoto(photos[i], photoW, photoH)

            val (lx, ly) = leftCoords[i]
            canvas.drawBitmap(finalPhoto, lx.toFloat(), ly.toFloat(), null)

            val (rx, ry) = rightCoords[i]
            canvas.drawBitmap(finalPhoto, rx.toFloat(), ry.toFloat(), null)
        }
    }
}