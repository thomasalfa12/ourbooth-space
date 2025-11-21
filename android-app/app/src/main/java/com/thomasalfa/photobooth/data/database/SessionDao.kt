package com.thomasalfa.photobooth.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    // 1. Simpan Sesi Baru (Return ID sesi yang baru dibuat)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    // 2. Simpan Banyak Foto Sekaligus
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<SessionPhotoEntity>)

    // 3. Ambil Semua Sesi (Untuk History Admin) - Terbaru paling atas
    @Transaction // Wajib pakai @Transaction karena mengambil relasi
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionWithPhotos>>

    // 4. Hapus SEMUA Data (Fitur Reset)
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM session_photos")
    suspend fun deleteAllPhotos()

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
    // Transaksi Reset Total
    @Transaction
    suspend fun clearAllData() {
        deleteAllPhotos()
        deleteAllSessions()
    }
}