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
        contentResolver: ContentResolver
    ) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ai = AiService.fromPrefs(prefs)
                val img64 = imageToBase64(imageUri, contentResolver)
                val result = ai.analyzeEcg(img64, symptoms, age, gender, history)

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
                val model = prefs.getString("model", "") ?: ""

                val record = EcgRecord(
                    patientName = patientName, patientAge = age, patientGender = gender,
                    symptoms = symptoms, clinicalHistory = history, imagePath = imagePath,
                    diagnosis = diagnosis, confidence = confidence, urgencyLevel = urgency,
                    heartRate = hr, rhythm = rhythm, axis = axis, prInterval = prI,
                    qrsDuration = qrsD, qtInterval = qtI, fullAnalysis = result,
                    ecgParams = "", aiModel = model
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

    private fun saveImage(uri: Uri, cr: ContentResolver): String {
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
