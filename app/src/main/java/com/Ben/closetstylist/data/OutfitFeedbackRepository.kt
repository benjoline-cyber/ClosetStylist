package com.Ben.closetstylist.data

import com.Ben.closetstylist.domain.StylistPersona

class OutfitFeedbackRepository(private val dao: OutfitFeedbackDao) {

    suspend fun save(feedback: OutfitFeedback) = dao.insert(feedback)

    suspend fun delete(feedback: OutfitFeedback) = dao.delete(feedback)

    suspend fun getRecentRejections(persona: StylistPersona): List<OutfitFeedback> =
        dao.getRecentRejections(persona.name)
}
