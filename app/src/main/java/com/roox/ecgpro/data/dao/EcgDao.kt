package com.roox.ecgpro.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.roox.ecgpro.data.model.EcgRecord
import com.roox.ecgpro.data.model.ChatMessage

@Dao
interface EcgDao {
    @Insert suspend fun insertRecord(r: EcgRecord): Long
    @Update suspend fun updateRecord(r: EcgRecord)
    @Delete suspend fun deleteRecord(r: EcgRecord)
    @Query("SELECT * FROM ecg_records ORDER BY timestamp DESC") fun allRecords(): LiveData<List<EcgRecord>>
    @Query("SELECT * FROM ecg_records WHERE id = :id") suspend fun getRecord(id: Int): EcgRecord?
    @Query("SELECT COUNT(*) FROM ecg_records") suspend fun recordCount(): Int

    @Insert suspend fun insertChat(m: ChatMessage): Long
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC") fun allChats(): LiveData<List<ChatMessage>>
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit") suspend fun recentChats(limit: Int): List<ChatMessage>
    @Query("DELETE FROM chat_messages") suspend fun clearChat()
}
