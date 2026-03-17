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
    val confidence: Int = 0,
    val urgencyLevel: String = "",
    val heartRate: Int = 0,
    val rhythm: String = "",
    val axis: String = "",
    val prInterval: String = "",
    val qrsDuration: String = "",
    val qtInterval: String = "",
    val stChanges: String = "",
    val fullAnalysis: String = "",
    val ecgParams: String = "",
    val aiModel: String = "",
    // v2.0 fields
    val ecgLayout: String = "SinglePage",
    val paperSpeed: String = "25",
    val voltageGain: String = "10",
    val acsRisk: String = "",
    val lvefStatus: String = "",
    val axisClassification: String = "",
    val leadImportance: String = "",
    val diagnosticGroups: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String = "user",
    val content: String = "",
    val imageUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "training_records")
data class TrainingRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String = "",
    val knownDiagnosis: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
