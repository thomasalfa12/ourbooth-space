package com.thomasalfa.photobooth.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.NetworkInterface
import java.util.Collections

object LocalShareManager {

    private var server: NettyApplicationEngine? = null
    private const val PORT = 8080

    // 1. Start Server
    fun startServer(filePath: String): String? {
        stopServer() // Matikan dulu kalau ada yg nyala

        val file = File(filePath)
        if (!file.exists()) return null

        // Perbaikan Warning: Gunakan safe call yang lebih rapi
        val ipAddress = getIpAddress() ?: return null

        // Start Ktor Server
        server = embeddedServer(Netty, PORT) {
            routing {
                get("/photo") {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "kubik_photo.jpg").toString()
                    )
                    call.respondFile(file)
                }
            }
        }.start(wait = false)

        return "http://$ipAddress:$PORT/photo"
    }

    // 2. Stop Server
    fun stopServer() {
        try {
            server?.stop(100, 100)
        } catch (e: Exception) {
            // Ignore error saat stop
        }
        server = null
    }

    // 3. Generate QR Bitmap
    suspend fun generateQrCode(content: String): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 4. Cari IP Address (IPv4) Tablet
    private fun getIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        // PERBAIKAN WARNING DISINI: Tambah kurung (...)
                        val isIPv4 = (sAddr?.indexOf(':') ?: -1) < 0

                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            // Warning "ex never used" hilang karena kita tidak pakai ex-nya
        }
        return null
    }
}