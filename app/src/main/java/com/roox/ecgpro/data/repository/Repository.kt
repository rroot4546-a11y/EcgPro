package com.roox.ecgpro.data.repository

import com.roox.ecgpro.data.dao.EcgDao
import com.roox.ecgpro.data.model.EcgRecord
import com.roox.ecgpro.data.model.ChatMessage
import com.roox.ecgpro.data.model.TrainingRecord

class Repository(private val dao: EcgDao) {
    val allRecords = dao.allRecords()
    val allChats = dao.allChats()
    val allTraining = dao.allTraining()
    suspend fun insertRecord(r: EcgRecord) = dao.insertRecord(r)
    suspend fun updateRecord(r: EcgRecord) = dao.updateRecord(r)
    suspend fun deleteRecord(r: EcgRecord) = dao.deleteRecord(r)
    suspend fun getRecord(id: Int) = dao.getRecord(id)
    suspend fun recordCount() = dao.recordCount()
    suspend fun insertChat(m: ChatMessage) = dao.insertChat(m)
    suspend fun recentChats(limit: Int) = dao.recentChats(limit)
    suspend fun clearChat() = dao.clearChat()
    suspend fun insertTraining(r: TrainingRecord) = dao.insertTraining(r)
    suspend fun deleteTraining(r: TrainingRecord) = dao.deleteTraining(r)
    suspend fun trainingCount() = dao.trainingCount()
}
