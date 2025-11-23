package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import kotlin.math.max

class PhotoPrintAdapter(
    private val context: Context,
    private val bitmap: Bitmap,
    private val jobName: String
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()

        // Return true agar print preview me-refresh layout jika atribut berubah
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        val pdf = pdfDocument ?: return

        // Halaman baru
        val page = pdf.startPage(0)
        val canvas = page.canvas

        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            pdf.close()
            pdfDocument = null
            return
        }

        // --- LOGIC CENTER CROP (FILL PAPER) ---
        // Agar hasil print full kertas Epson L8050 (4R) tanpa border putih

        val paperWidth = canvas.width.toFloat()
        val paperHeight = canvas.height.toFloat()
        val imgWidth = bitmap.width.toFloat()
        val imgHeight = bitmap.height.toFloat()

        // Hitung scale agar gambar memenuhi kertas (ambil scale terbesar)
        val scale = max(paperWidth / imgWidth, paperHeight / imgHeight)

        // Hitung dimensi gambar setelah discale
        val scaledWidth = imgWidth * scale
        val scaledHeight = imgHeight * scale

        // Hitung posisi agar gambar berada tepat di tengah kertas
        val translateX = (paperWidth - scaledWidth) / 2f
        val translateY = (paperHeight - scaledHeight) / 2f

        // Terapkan Matrix
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(translateX, translateY)

        // Gambar dengan filtering agar halus
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // Draw
        canvas.drawBitmap(bitmap, matrix, paint)

        pdf.finishPage(page)

        try {
            destination?.let {
                pdf.writeTo(FileOutputStream(it.fileDescriptor))
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdf.close()
            pdfDocument = null
        }
    }
}