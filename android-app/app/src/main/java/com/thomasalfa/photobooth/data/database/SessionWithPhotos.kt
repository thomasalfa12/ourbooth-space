package com.thomasalfa.photobooth.data.database

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithPhotos(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val photos: List<SessionPhotoEntity>
)