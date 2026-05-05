package com.Ben.closetstylist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WearLogDaoTest {

    private lateinit var db: ClosetDatabase
    private lateinit var clothingItemDao: ClothingItemDao
    private lateinit var wearLogDao: WearLogDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClosetDatabase::class.java,
        ).build()
        clothingItemDao = db.clothingItemDao()
        wearLogDao = db.wearLogDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndQueryForItem() = runTest {
        val item = ClothingItem("i1", "file:///", ClothingCategory.TOP, listOf("red"), listOf(Season.SUMMER), "A top", null)
        clothingItemDao.insert(item)

        val log = WearLog("l1", "i1", LocalDate.of(2026, 5, 1))
        wearLogDao.insert(log)

        val logs = wearLogDao.getLogsForItem("i1").first()
        assertEquals(1, logs.size)
        assertEquals(log, logs.first())
    }

    @Test
    fun cascadeDeleteRemovesLogs() = runTest {
        val item = ClothingItem("i2", "file:///", ClothingCategory.SHOES, emptyList(), emptyList(), "Shoes", null)
        clothingItemDao.insert(item)
        wearLogDao.insert(WearLog("l2", "i2", LocalDate.of(2026, 4, 20)))

        clothingItemDao.delete(item)
        assertEquals(0, wearLogDao.getLogsForItem("i2").first().size)
    }
}
