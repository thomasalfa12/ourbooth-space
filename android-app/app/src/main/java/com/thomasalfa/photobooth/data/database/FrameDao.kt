package com.thomasalfa.photobooth.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameDao {
    @Query("SELECT * FROM frames")
    fun getAllFrames(): Flow<List<FrameEntity>> // Realtime update

    @Query("SELECT * FROM frames WHERE category = :cat")
    fun getFramesByCategory(cat: String): Flow<List<FrameEntity>>

    @Insert
    suspend fun insertFrame(frame: FrameEntity)

    @Delete
    suspend fun deleteFrame(frame: FrameEntity)
}