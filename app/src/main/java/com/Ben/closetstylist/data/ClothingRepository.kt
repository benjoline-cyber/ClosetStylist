package com.Ben.closetstylist.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class ClothingRepository(
    private val clothingItemDao: ClothingItemDao,
    private val wearLogDao: WearLogDao,
) {
    fun getAllItems(): Flow<List<ClothingItem>> = clothingItemDao.getAll()

    fun getItemsByCategory(category: ClothingCategory): Flow<List<ClothingItem>> =
        clothingItemDao.getByCategory(category)

    fun getAvailableItems(cutoff: LocalDate): Flow<List<ClothingItem>> =
        clothingItemDao.getAvailableItems(cutoff)

    suspend fun addItem(item: ClothingItem) = clothingItemDao.insert(item)

    suspend fun updateItem(item: ClothingItem) = clothingItemDao.update(item)

    suspend fun deleteItem(item: ClothingItem) = clothingItemDao.delete(item)

    suspend fun recordWorn(itemId: String, date: LocalDate) {
        val item = clothingItemDao.getById(itemId) ?: return
        clothingItemDao.update(item.copy(lastWornDate = date))
        wearLogDao.insert(WearLog(id = UUID.randomUUID().toString(), itemId = itemId, wornOn = date))
    }

    suspend fun undoWorn(itemId: String, previousDate: LocalDate?) {
        val item = clothingItemDao.getById(itemId) ?: return
        clothingItemDao.update(item.copy(lastWornDate = previousDate))
        wearLogDao.deleteByItemIdAndDate(itemId, LocalDate.now().toString())
    }
}
