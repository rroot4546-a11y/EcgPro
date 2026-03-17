package com.roox.ecgpro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ecg_records")
data class EcgRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String = "",
    val patientAge: String = "",
    val patientGender: String = "",
    val symptoms: String = "",
    val clinicalHistory: String = "",
    val imagePath: String = "",
    val diagnosis: String = "",
    val confidence: Int = 0,           // 0-100%
    val urgencyLevel: String = "",     // routine, urgent, emergent
    val heartRate: Int = 0,
    val rhythm: String = "",
    val axis: String = "",
    val prInterval: String = "",
    val qrsDuration: String = "",
    val qtInterval: String = "",
    val stChanges: String = "",
    val fullAnalysis: String = "",
    val ecgParams: String = "",        // JSON of synthesized waveform params
    val aiModel: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String = "user",         // user, assistant
    val content: String = "",
    val imageUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
