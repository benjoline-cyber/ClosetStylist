package com.Ben.closetstylist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ClothingItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClothingItem)

    @Update
    suspend fun update(item: ClothingItem)

    @Delete
    suspend fun delete(item: ClothingItem)

    @Query("SELECT * FROM clothing_items ORDER BY category ASC, lastWornDate ASC")
    fun getAll(): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing_items WHERE id = :id")
    suspend fun getById(id: String): ClothingItem?

    // Items not worn in the last 3 days (or never worn) — candidates for outfit suggestions.
    @Query("SELECT * FROM clothing_items WHERE lastWornDate IS NULL OR lastWornDate <= :cutoff")
    fun getAvailableItems(cutoff: LocalDate): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY lastWornDate ASC")
    fun getByCategory(category: ClothingCategory): Flow<List<ClothingItem>>
}
