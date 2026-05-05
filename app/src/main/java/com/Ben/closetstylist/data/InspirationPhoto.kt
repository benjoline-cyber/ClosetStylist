package com.Ben.closetstylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "inspiration_photos")
data class InspirationPhoto(
    @PrimaryKey val id: String,
    val imagePath: String,
    val addedAt: Instant,
)
