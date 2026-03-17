package com.roox.ecgpro.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.*
import com.roox.ecgpro.EcgProApp
import com.roox.ecgpro.data.model.ChatMessage
import com.roox.ecgpro.data.model.EcgRecord
import com.roox.ecgpro.data.model.TrainingRecord
import com.roox.ecgpro.service.AiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class EcgViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EcgProApp
    private val repo = app.repo

    val allRecords: LiveData<List<EcgRecord>> = repo.allRecords
    val allChats: LiveData<List<ChatMessage>> = repo.allChats
    val allTraining: LiveData<List<TrainingRecord>> = repo.allTraining

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _analysisResult = MutableLiveData<EcgRecord?>()
    val analysisResult: LiveData<EcgRecord?> = _analysisResult

    fun clearResult() { _analysisResult.value = null }

    suspend fun getRecord(id: Int): EcgRecord? = repo.getRecord(id)

    fun analyzeEcg(
        imageUri: Uri,
        symptoms: String,
        age: String,
        gender: String,
        history: String,
        patientName: String,
        prefs: SharedPreferences,
        contentResolver: ContentResolver,
        ecgLayout: String = "standard_3x2",
        paperSpeed: String = "25",
        voltageGain: String = "10"
    ) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ai = AiService.fromPrefs(prefs)
                val img64 = imageToBase64(imageUri, contentResolver)
                val result = ai.analyzeEcg(img64, symptoms, age, gender, history, ecgLayout, paperSpeed, voltageGain)

                val imagePath = saveImage(imageUri, contentResolver)
                val hr = extractInt(result, """heart\s*rate[:\s]*(\d+)""") ?: extractInt(result, """(\d+)\s*bpm""") ?: 0
                val rhythm = extractString(result, """rhythm[:\s]*(.+)""") ?: "Unknown"
                val axis = extractString(result, """axis[:\s]*(.+)""") ?: ""
                val prI = extractString(result, """pr(?:\s*interval)?[:\s]*(.+?ms)""") ?: ""
                val qrsD = extractString(result, """qrs(?:\s*(?:duration|complex))?[:\s]*(.+?ms)""") ?: ""
                val qtI = extractString(result, """qt(?:/qtc)?[:\s]*(.+?ms)""") ?: ""
                val urgency = when {
                    result.lowercase().contains("emergent") || result.lowercase().contains("🔴") -> "emergent"
                    result.lowercase().contains("urgent") || result.lowercase().contains("🟡") -> "urgent"
                    else -> "routine"
                }
                val confidence = extractInt(result, """confidence[:\s]*(\d+)""") ?: 0
                val diagnosis = extractString(result, """(?:primary\s*)?diagnosis[:\s]*(.+)""") ?: "See full analysis"
                val modelName = prefs.getString("model", "") ?: ""

                // v2.0: Parse ACS risk
                val acsRisk = parseAcsRisk(result)

                // v2.0: Parse lead importance as JSON
                val leadImportance = parseLeadImportance(result)

                val record = EcgRecord(
                    patientName = patientName, patientAge = age, patientGender = gender,
                    symptoms = symptoms, clinicalHistory = history, imagePath = imagePath,
                    diagnosis = diagnosis, confidence = confidence, urgencyLevel = urgency,
                    heartRate = hr, rhythm = rhythm, axis = axis, prInterval = prI,
                    qrsDuration = qrsD, qtInterval = qtI, fullAnalysis = result,
                    ecgParams = "", aiModel = modelName,
                    ecgLayout = ecgLayout, paperSpeed = paperSpeed, voltageGain = voltageGain,
                    acsRisk = acsRisk, leadImportance = leadImportance
                )
                val id = repo.insertRecord(record)
                _analysisResult.postValue(record.copy(id = id.toInt()))
            } catch (e: Exception) {
                _analysisResult.postValue(null)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun sendChat(message: String, imageUri: Uri?, prefs: SharedPreferences, contentResolver: ContentResolver) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imgPath = if (imageUri != null) saveImage(imageUri, contentResolver) else ""
                repo.insertChat(ChatMessage(role = "user", content = message, imageUri = imgPath))

                val ai = AiService.fromPrefs(prefs)
                val img64 = if (imageUri != null) imageToBase64(imageUri, contentResolver) else null
                val recent = repo.recentChats(10).reversed()
                val historyList = recent.map { mapOf<String, Any>("role" to it.role, "content" to it.content) }
                val result = ai.chat(message, img64, historyList)

                repo.insertChat(ChatMessage(role = "assistant", content = result))
            } catch (e: Exception) {
                repo.insertChat(ChatMessage(role = "assistant", content = "❌ Error: ${e.message}"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun insertTraining(record: TrainingRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertTraining(record)
        }
    }

    fun deleteTraining(record: TrainingRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteTraining(record)
        }
    }

    /** Parse ACS Risk from AI text */
    private fun parseAcsRisk(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("acs risk: confirmed") || lower.contains("acs suspicion: yeswithsymptoms") -> "confirmed"
            lower.contains("acs risk: indeterminate") || lower.contains("acs suspicion: yeswithout") -> "indeterminate"
            lower.contains("acs risk: not omi") || lower.contains("acs suspicion: no") -> "not_omi"
            lower.contains("acs risk: outside population") || lower.contains("outside_population") -> "outside_population"
            lower.contains("acs risk: reperfused") -> "reperfused"
            lower.contains("acs risk: presentation missing") || lower.contains("presentation missing") -> "presentation_missing"
            else -> "unknown"
        }
    }

    /** Parse lead importance from AI text into JSON string */
    private fun parseLeadImportance(text: String): String {
        val leads = listOf("I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6")
        val result = mutableMapOf<String, String>()
        val lower = text.lowercase()

        // Try parsing "Lead:level" format
        for (lead in leads) {
            val leadLower = lead.lowercase()
            val pattern = Regex("""$leadLower\s*:\s*(critical|high|moderate|low|normal)""", RegexOption.IGNORE_CASE)
            val match = pattern.find(lower)
            if (match != null) {
                result.put(lead, match.groupValues.get(1).lowercase())
            } else {
                result.put(lead, "normal")
            }
        }

        // Also check for ranges like "V1-V4(critical)"
        val rangePattern = Regex("""(V\d)\s*-\s*(V\d)\s*[\(:]?\s*(critical|high|moderate|low|normal)""", RegexOption.IGNORE_CASE)
        for (match in rangePattern.findAll(text)) {
            val startNum = match.groupValues.get(1).substring(1).toIntOrNull() ?: continue
            val endNum = match.groupValues.get(2).substring(1).toIntOrNull() ?: continue
            val level = match.groupValues.get(3).lowercase()
            for (n in startNum..endNum) {
                result.put("V$n", level)
            }
        }

        val sb = StringBuilder("{")
        var first = true
        for (entry in result.entries) {
            if (!first) sb.append(",")
            sb.append("\"${entry.key}\":\"${entry.value}\"")
            first = false
        }
        sb.append("}")
        return sb.toString()
    }

    private fun imageToBase64(uri: Uri, cr: ContentResolver): String {
        return try {
            val input = cr.openInputStream(uri) ?: return ""
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            val scaled = scaleBitmap(bitmap, 1024)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    private fun scaleBitmap(bmp: Bitmap, maxDim: Int): Bitmap {
        val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
        if (ratio >= 1f) return bmp
        return Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
    }

    fun saveImage(uri: Uri, cr: ContentResolver): String {
        return try {
            val input = cr.openInputStream(uri) ?: return ""
            val dir = File(app.filesDir, "ecg_images"); dir.mkdirs()
            val file = File(dir, "ecg_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> input.copyTo(out) }; input.close()
            file.absolutePath
        } catch (_: Exception) { "" }
    }

    private fun extractInt(text: String, pattern: String) =
        Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()?.toIntOrNull()

    private fun extractString(text: String, pattern: String) =
        Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
}
