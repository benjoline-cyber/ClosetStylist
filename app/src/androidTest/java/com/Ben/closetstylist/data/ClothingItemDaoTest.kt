package com.Ben.closetstylist.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ClothingItemDaoTest {

    private lateinit var db: ClosetDatabase
    private lateinit var dao: ClothingItemDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClosetDatabase::class.java,
        ).build()
        dao = db.clothingItemDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndQueryAll() = runTest {
        val item = testItem("1")
        dao.insert(item)
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals(item, all.first())
    }

    @Test
    fun updateLastWornDate() = runTest {
        val item = testItem("2")
        dao.insert(item)
        val updated = item.copy(lastWornDate = LocalDate.of(2026, 1, 10))
        dao.update(updated)
        val fetched = dao.getById("2")
        assertEquals(LocalDate.of(2026, 1, 10), fetched?.lastWornDate)
    }

    @Test
    fun deleteRemovesItem() = runTest {
        val item = testItem("3")
        dao.insert(item)
        dao.delete(item)
        assertNull(dao.getById("3"))
    }

    @Test
    fun availableItemsExcludesRecentlyWorn() = runTest {
        val worn = testItem("4").copy(lastWornDate = LocalDate.of(2026, 5, 4))
        val unworn = testItem("5")
        dao.insert(worn)
        dao.insert(unworn)
        // cutoff = 3 days ago: items worn after cutoff are excluded
        val cutoff = LocalDate.of(2026, 5, 1)
        val available = dao.getAvailableItems(cutoff).first()
        assertEquals(1, available.size)
        assertEquals("5", available.first().id)
    }

    private fun testItem(id: String) = ClothingItem(
        id = id,
        imagePath = "file:///data/closet/$id.jpg",
        category = ClothingCategory.TOP,
        colorTags = listOf("blue"),
        seasonTags = listOf(Season.SPRING),
        description = "A blue top",
        lastWornDate = null,
    )
}
