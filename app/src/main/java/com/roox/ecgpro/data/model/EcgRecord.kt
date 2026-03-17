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
    val timestamp: Long = System.currentTimeMillis(),
    // v2.0 fields
    val ecgLayout: String = "standard_3x2",   // single_page, standard_6x1, standard_3x2, cabrera_6x1, cabrera_3x2
    val paperSpeed: String = "25",             // "25" or "50" mm/s
    val voltageGain: String = "10",            // "10" or "5" mm/mV
    val acsRisk: String = "",                  // confirmed, indeterminate, not_omi, outside_population, reperfused, presentation_missing
    val leadImportance: String = ""            // JSON: {"I":"normal","II":"high","V1":"critical",...}
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String = "user",         // user, assistant
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
