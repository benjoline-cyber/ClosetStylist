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

@RunWith(AndroidJUnit4::class)
class InspirationPhotoDaoTest {

    private lateinit var db: ClosetDatabase
    private lateinit var dao: InspirationPhotoDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClosetDatabase::class.java,
        ).build()
        dao = db.inspirationPhotoDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndQueryAll() = runTest {
        val photo = InspirationPhoto("p1", "file:///data/inspiration/p1.jpg", Instant.ofEpochMilli(1000))
        dao.insert(photo)
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals(photo, all.first())
    }

    @Test
    fun deleteRemovesPhoto() = runTest {
        val photo = InspirationPhoto("p2", "file:///data/inspiration/p2.jpg", Instant.ofEpochMilli(2000))
        dao.insert(photo)
        dao.delete(photo)
        assertEquals(0, dao.getAll().first().size)
    }
}
