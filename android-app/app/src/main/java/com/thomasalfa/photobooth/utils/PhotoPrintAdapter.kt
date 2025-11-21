package com.thomasalfa.photobooth.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
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
        // 1. Buat Dokumen PDF Virtual
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        // 2. Beri tahu sistem kalau kita punya 1 halaman
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        // 3. Proses Menggambar Bitmap ke Kertas
        val pdf = pdfDocument ?: return
        val page = pdf.startPage(0)
        val canvas = page.canvas

        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            pdf.close()
            pdfDocument = null
            return
        }

        // --- LOGIKA SCALING AGAR FIT & CENTER (MIRIP CSS COVER) ---
        val contentWidth = bitmap.width.toFloat()
        val contentHeight = bitmap.height.toFloat()

        // Ukuran Kertas (Canvas) yang diberikan oleh Printer
        val paperWidth = canvas.width.toFloat()
        val paperHeight = canvas.height.toFloat()

        // Hitung Scale
        val scaleX = paperWidth / contentWidth
        val scaleY = paperHeight / contentHeight

        // Pilih scale terbesar agar gambar memenuhi kertas (Fill)
        // Gunakan scaleX/scaleY yang lebih besar agar tidak ada ruang putih
        val scale = maxOf(scaleX, scaleY)

        // Hitung posisi tengah
        val scaledWidth = contentWidth * scale
        val scaledHeight = contentHeight * scale
        val translateX = (paperWidth - scaledWidth) / 2
        val translateY = (paperHeight - scaledHeight) / 2

        // Terapkan Matrix
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(translateX, translateY)

        // Gambar Bitmap ke Canvas PDF
        canvas.drawBitmap(bitmap, matrix, null)

        pdf.finishPage(page)

        // 4. Simpan ke Output Stream Printer
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