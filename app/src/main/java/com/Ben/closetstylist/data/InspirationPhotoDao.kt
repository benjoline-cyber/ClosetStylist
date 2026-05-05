package com.Ben.closetstylist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspirationPhotoDao {

    @Insert
    suspend fun insert(photo: InspirationPhoto)

    @Delete
    suspend fun delete(photo: InspirationPhoto)

    @Query("SELECT * FROM inspiration_photos ORDER BY addedAt DESC")
    fun getAll(): Flow<List<InspirationPhoto>>
}
