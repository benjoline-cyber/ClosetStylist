package com.Ben.closetstylist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WearLogDao {

    @Insert
    suspend fun insert(log: WearLog)

    @Query("SELECT * FROM wear_logs WHERE itemId = :itemId ORDER BY wornOn DESC")
    fun getLogsForItem(itemId: String): Flow<List<WearLog>>

    @Query("SELECT * FROM wear_logs ORDER BY wornOn DESC")
    fun getAll(): Flow<List<WearLog>>

    @Query("DELETE FROM wear_logs WHERE itemId = :itemId AND wornOn = :date")
    suspend fun deleteByItemIdAndDate(itemId: String, date: String)
}
