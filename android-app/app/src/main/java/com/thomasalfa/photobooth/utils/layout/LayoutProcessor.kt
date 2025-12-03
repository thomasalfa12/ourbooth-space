package com.thomasalfa.photobooth.utils.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Paint

data class SlotDefinition(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
object LayoutProcessor {
    // Canvas Size (4x6 Print @ 300dpi approximation)
    const val CANVAS_WIDTH = 1200
    const val CANVAS_HEIGHT = 1800

    // --- SINGLE SOURCE OF TRUTH KOORDINAT ---
    fun getSlotsForLayout(layoutType: String): List<SlotDefinition> {
        return if (layoutType == "STRIP") {
            val w = 500; val h = 375
            // Strip biasanya 3 foto diduplikasi kiri-kanan. Kita definisikan slot logisnya (0,1,2)
            // Di UI Editor, user hanya perlu mengatur 3 foto ini.
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
        photos: List<Bitmap>, // Foto yang sudah diurutkan user
        layoutType: String,
        frameBitmap: Bitmap?
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE) // Base background

        val slots = getSlotsForLayout(layoutType)

        // Draw Photos
        // Logic: Jika STRIP, kita gambar foto index 0 ke slot kiri DAN slot kanan
        if (layoutType == "STRIP") {
            val rightOffset = 600 // Jarak geser ke strip kanan
            slots.forEachIndexed { index, slot ->
                if (index < photos.size) {
                    val photo = photos[index]
                    // Gambar Kiri
                    canvas.drawBitmap(photo, null, Rect(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height), null)
                    // Gambar Kanan (Duplikat)
                    canvas.drawBitmap(photo, null, Rect(slot.x + rightOffset, slot.y, slot.x + rightOffset + slot.width, slot.y + slot.height), null)
                }
            }
        } else {
            // GRID
            slots.forEachIndexed { index, slot ->
                if (index < photos.size) {
                    canvas.drawBitmap(photos[index], null, Rect(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height), null)
                }
            }
        }

        // Draw Frame Overlay
        if (frameBitmap != null) {
            canvas.drawBitmap(frameBitmap, null, Rect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT), null)
        }

        return resultBitmap
    }
}