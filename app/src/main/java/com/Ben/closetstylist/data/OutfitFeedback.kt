package com.Ben.closetstylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfit_feedback")
data class OutfitFeedback(
    @PrimaryKey val id: String,
    val itemIds: String,        // comma-joined, sorted item IDs
    val feedbackType: FeedbackType,
    val createdAt: Long,        // epoch millis
    val persona: String,        // StylistPersona.name
)
