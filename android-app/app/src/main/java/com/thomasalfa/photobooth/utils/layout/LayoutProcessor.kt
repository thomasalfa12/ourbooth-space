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
        // Ukuran Canvas (2:3 Ratio - 4x6 Print)
        val canvasWidth = 1200
        val canvasHeight = 1800

        val resultBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 1. Gambar Background Putih
        canvas.drawColor(Color.WHITE)

        // 2. Gambar Foto-foto (Langsung resize di Canvas)
        if (layoutType == "STRIP") {
            drawStripLayout(canvas, photos)
        } else {
            drawGridLayout(canvas, photos)
        }

        // 3. Gambar Frame Overlay (Langsung resize di Canvas)
        if (frameBitmap != null) {
            // Trik Optimasi: Tentukan kotak tujuan (Full Canvas)
            val destRect = Rect(0, 0, canvasWidth, canvasHeight)
            // null pada parameter ke-2 artinya pakai seluruh bagian gambar frame asli
            canvas.drawBitmap(frameBitmap, null, destRect, null)
        }

        return resultBitmap
    }

    // --- LOGIC TIPE A: CLASSIC GRID (2x3) ---
    private fun drawGridLayout(canvas: Canvas, photos: List<Bitmap>) {
        val photoW = 520
        val photoH = 390

        // Koordinat X, Y pojok kiri atas setiap foto
        val coordinates = listOf(
            Pair(60, 350), Pair(620, 350),
            Pair(60, 780), Pair(620, 780),
            Pair(60, 1210), Pair(620, 1210)
        )

        for (i in 0 until minOf(photos.size, 6)) {
            val (x, y) = coordinates[i]

            // OPTIMASI: Tentukan kotak tujuan (Destination Rectangle)
            // Canvas akan otomatis me-resize foto agar pas di kotak ini
            val destRect = Rect(x, y, x + photoW, y + photoH)

            // Gambar langsung! Tidak perlu createScaledBitmap
            canvas.drawBitmap(photos[i], null, destRect, null)
        }
    }

    // --- LOGIC TIPE B: PHOTO STRIP (Cut 2) ---
    private fun drawStripLayout(canvas: Canvas, photos: List<Bitmap>) {
        val photoW = 500
        val photoH = 375

        val leftCoords = listOf(Pair(50, 300), Pair(50, 705), Pair(50, 1110))
        val rightCoords = listOf(Pair(650, 300), Pair(650, 705), Pair(650, 1110))

        for (i in 0 until minOf(photos.size, 3)) {
            val photo = photos[i]

            // Sisi Kiri
            val (lx, ly) = leftCoords[i]
            val leftRect = Rect(lx, ly, lx + photoW, ly + photoH)
            canvas.drawBitmap(photo, null, leftRect, null)

            // Sisi Kanan (Duplikat)
            val (rx, ry) = rightCoords[i]
            val rightRect = Rect(rx, ry, rx + photoW, ry + photoH)
            canvas.drawBitmap(photo, null, rightRect, null)
        }
    }
}