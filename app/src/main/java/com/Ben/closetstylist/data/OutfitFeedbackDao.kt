package com.Ben.closetstylist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OutfitFeedbackDao {

    @Insert
    suspend fun insert(feedback: OutfitFeedback)

    @Delete
    suspend fun delete(feedback: OutfitFeedback)

    @Query(
        "SELECT * FROM outfit_feedback WHERE feedbackType = 'REJECTED_COMBO' AND persona = :personaName " +
            "ORDER BY createdAt DESC LIMIT 10",
    )
    suspend fun getRecentRejections(personaName: String): List<OutfitFeedback>
}
