package com.thomasalfa.photobooth.utils

import android.content.Context
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.SessionEntity
import com.thomasalfa.photobooth.data.database.SessionPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalDataManager {

    suspend fun saveSessionToDb(
        context: Context,
        uuid: String,
        finalPath: String?,
        gifPath: String?,
        rawPhotoPaths: List<String>
    ) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)

            // 1. Buat Data Sesi
            val session = SessionEntity(
                sessionUuid = uuid,
                timestamp = System.currentTimeMillis(),
                totalPhotos = rawPhotoPaths.size,
                finalGridPath = finalPath,
                gifPath = gifPath,
                isPrinted = false // Default belum print
            )

            // 2. Simpan Sesi & Dapat ID-nya
            val sessionId = db.sessionDao().insertSession(session)

            // 3. Siapkan Data Foto Mentah
            val photoEntities = rawPhotoPaths.map { path ->
                SessionPhotoEntity(
                    sessionId = sessionId, // Link ke ID sesi diatas
                    filePath = path,
                    isFinalResult = false
                )
            }

            // 4. Simpan Foto Mentah Sekaligus
            db.sessionDao().insertPhotos(photoEntities)
        }
    }
}