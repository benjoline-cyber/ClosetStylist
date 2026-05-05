package com.Ben.closetstylist.data

import kotlinx.coroutines.flow.Flow

class InspirationRepository(private val dao: InspirationPhotoDao) {

    fun getAllPhotos(): Flow<List<InspirationPhoto>> = dao.getAll()

    suspend fun addPhoto(photo: InspirationPhoto) = dao.insert(photo)

    suspend fun deletePhoto(photo: InspirationPhoto) = dao.delete(photo)
}
