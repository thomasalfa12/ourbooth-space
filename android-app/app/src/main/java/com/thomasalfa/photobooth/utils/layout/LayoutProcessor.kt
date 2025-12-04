package com.thomasalfa.photobooth.utils.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect

data class SlotDefinition(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

object LayoutProcessor {
    // --- LOGICAL COORDINATES (Tetap 300 DPI sebagai acuan dasar) ---
    // Jangan ubah ini agar UI Editor di layar HP/Tablet tidak berantakan.
    const val BASE_WIDTH = 1200
    const val BASE_HEIGHT = 1800

    // --- HIGH RES SCALER ---
    // 1f = 300 DPI (Standard)
    // 2f = 600 DPI (High Quality - Recommended for 6MP Camera)
    // 3f = 900 DPI (Ultra High - Hati-hati OutOfMemoryError di HP kentang)
    private const val SCALE_FACTOR = 2f

    // Ukuran Canvas Akhir (Fisik)
    val CANVAS_WIDTH = (BASE_WIDTH * SCALE_FACTOR).toInt()
    val CANVAS_HEIGHT = (BASE_HEIGHT * SCALE_FACTOR).toInt()

    // --- SINGLE SOURCE OF TRUTH KOORDINAT (Logis) ---
    fun getSlotsForLayout(layoutType: String): List<SlotDefinition> {
        return if (layoutType == "STRIP") {
            val w = 500; val h = 375
            listOf(
                SlotDefinition(0, 50, 300, w, h),
                SlotDefinition(1, 50, 705, w, h),
                SlotDefinition(2, 50, 1110, w, h)
            )
        } else {
            // GRID (2x3 = 6 Foto)
            val w = 520; val h = 390
            listOf(
                SlotDefinition(0, 60, 350, w, h), SlotDefinition(1, 620, 350, w, h),
                SlotDefinition(2, 60, 780, w, h), SlotDefinition(3, 620, 780, w, h),
                SlotDefinition(4, 60, 1210, w, h), SlotDefinition(5, 620, 1210, w, h)
            )
        }
    }

    fun getPhotoCountForLayout(layoutType: String): Int {
        return getSlotsForLayout(layoutType).size
    }

    fun processLayout(
        photos: List<Bitmap>,
        layoutType: String,
        frameBitmap: Bitmap?
    ): Bitmap {
        // 1. Buat Bitmap Raksasa (High Res)
        val resultBitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)

        val slots = getSlotsForLayout(layoutType)

        // 2. Helper Function untuk Scaling Koordinat
        fun scale(value: Int): Int = (value * SCALE_FACTOR).toInt()

        // 3. Draw Photos
        if (layoutType == "STRIP") {
            val rightOffset = scale(600) // Jarak geser ke strip kanan (Scaled)

            slots.forEachIndexed { index, slot ->
                if (index < photos.size) {
                    val photo = photos[index]

                    // Hitung Koordinat Scaled (Kiri)
                    val leftRect = Rect(
                        scale(slot.x),
                        scale(slot.y),
                        scale(slot.x + slot.width),
                        scale(slot.y + slot.height)
                    )
                    canvas.drawBitmap(photo, null, leftRect, null)

                    // Hitung Koordinat Scaled (Kanan - Duplikat)
                    val rightRect = Rect(
                        leftRect.left + rightOffset, // Geser dari posisi kiri yang sudah disscale
                        leftRect.top,
                        leftRect.right + rightOffset,
                        leftRect.bottom
                    )
                    canvas.drawBitmap(photo, null, rightRect, null)
                }
            }
        } else {
            // GRID
            slots.forEachIndexed { index, slot ->
                if (index < photos.size) {
                    val destRect = Rect(
                        scale(slot.x),
                        scale(slot.y),
                        scale(slot.x + slot.width),
                        scale(slot.y + slot.height)
                    )
                    canvas.drawBitmap(photos[index], null, destRect, null)
                }
            }
        }

        // 4. Draw Frame Overlay (Scaled)
        if (frameBitmap != null) {
            // Frame ditarik agar memenuhi canvas resolusi tinggi
            val destRect = Rect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
            canvas.drawBitmap(frameBitmap, null, destRect, null)
        }

        return resultBitmap
    }
}