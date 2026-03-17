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

    val allRecords = repo.allRecords
    val allChats = repo.allChats
    val allTraining = repo.allTraining

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _analysisResult = MutableLiveData<EcgRecord?>()
    val analysisResult: LiveData<EcgRecord?> = _analysisResult

    fun clearResult() { _analysisResult.value = null }
    suspend fun getRecord(id: Int) = repo.getRecord(id)

    fun analyzeEcg(
        imageUri: Uri, symptoms: String, age: String, gender: String, history: String,
        patientName: String, prefs: SharedPreferences, cr: ContentResolver,
        layout: String = "SinglePage", paperSpeed: String = "25", voltageGain: String = "10"
    ) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ai = AiService.fromPrefs(prefs)
                val img64 = imageToBase64(imageUri, cr)
                val result = ai.analyzeEcg(img64, symptoms, age, gender, history, layout, paperSpeed, voltageGain)

                val imagePath = saveImage(imageUri, cr)
                val hr = extractInt(result, """heart\s*rate[:\s]*(\d+)""") ?: extractInt(result, """(\d+)\s*bpm""") ?: 0
                val rhythm = extractStr(result, """rhythm[:\s]*(.+)""") ?: "Unknown"
                val axis = extractStr(result, """axis[:\s]*(.+?)[(\n]""") ?: ""
                val prI = extractStr(result, """pr(?:\s*interval)?[:\s]*(.+?ms)""") ?: ""
                val qrsD = extractStr(result, """qrs(?:\s*(?:duration|complex))?[:\s]*(.+?ms)""") ?: ""
                val qtI = extractStr(result, """qt(?:/qtc)?[:\s]*(.+?ms)""") ?: ""
                val urgency = when {
                    result.lowercase().contains("emergent") || result.contains("🔴") -> "emergent"
                    result.lowercase().contains("urgent") || result.contains("🟡") -> "urgent"
                    else -> "routine"
                }
                val confidence = extractInt(result, """confidence[:\s]*(\d+)""") ?: 0
                val diagnosis = extractStr(result, """(?:primary\s*)?diagnosis[:\s]*(.+)""") ?: "See full analysis"
                val acsRisk = extractStr(result, """acs\s*risk[:\s]*(\w+)""") ?: ""
                val lvef = extractStr(result, """lvef\s*status[:\s]*(\w+)""") ?: ""
                val axisClass = extractStr(result, """classification[:\s]*(Normal|Left|Right|Extreme)""") ?: ""
                val leadImp = extractStr(result, """(?:lead\s*importance|LEAD IMPORTANCE)[:\s]*(.+)""") ?: ""
                val diagGroups = extractStr(result, """(?:RHYTHM|rhythm)[:\s]*(.+)""") ?: ""
                val model = prefs.getString("model", "") ?: ""

                val record = EcgRecord(
                    patientName = patientName, patientAge = age, patientGender = gender,
                    symptoms = symptoms, clinicalHistory = history, imagePath = imagePath,
                    diagnosis = diagnosis, confidence = confidence, urgencyLevel = urgency,
                    heartRate = hr, rhythm = rhythm, axis = axis, prInterval = prI,
                    qrsDuration = qrsD, qtInterval = qtI, fullAnalysis = result,
                    ecgParams = "", aiModel = model,
                    ecgLayout = layout, paperSpeed = paperSpeed, voltageGain = voltageGain,
                    acsRisk = acsRisk, lvefStatus = lvef, axisClassification = axisClass,
                    leadImportance = leadImp, diagnosticGroups = diagGroups
                )
                val id = repo.insertRecord(record)
                _analysisResult.postValue(record.copy(id = id.toInt()))
            } catch (e: Exception) { _analysisResult.postValue(null) }
            finally { _isLoading.postValue(false) }
        }
    }

    fun sendChat(msg: String, imgUri: Uri?, prefs: SharedPreferences, cr: ContentResolver) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imgPath = if (imgUri != null) saveImage(imgUri, cr) else ""
                repo.insertChat(ChatMessage(role = "user", content = msg, imageUri = imgPath))
                val ai = AiService.fromPrefs(prefs)
                val img64 = if (imgUri != null) imageToBase64(imgUri, cr) else null
                val recent = repo.recentChats(10).reversed()
                val hist = recent.map { mapOf<String, Any>("role" to it.role, "content" to it.content) }
                val result = ai.chat(msg, img64, hist)
                repo.insertChat(ChatMessage(role = "assistant", content = result))
            } catch (e: Exception) {
                repo.insertChat(ChatMessage(role = "assistant", content = "❌ ${e.message}"))
            } finally { _isLoading.postValue(false) }
        }
    }

    fun addTraining(imgUri: Uri, diagnosis: String, notes: String, cr: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = saveImage(imgUri, cr)
            repo.insertTraining(TrainingRecord(imagePath = path, knownDiagnosis = diagnosis, notes = notes))
        }
    }

    private fun imageToBase64(uri: Uri, cr: ContentResolver): String = try {
        val input = cr.openInputStream(uri) ?: return ""
        val bmp = BitmapFactory.decodeStream(input); input.close()
        val s = scaleBitmap(bmp, 1024)
        val baos = ByteArrayOutputStream()
        s.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) { "" }

    private fun scaleBitmap(bmp: Bitmap, max: Int): Bitmap {
        val r = minOf(max.toFloat() / bmp.width, max.toFloat() / bmp.height)
        return if (r >= 1f) bmp else Bitmap.createScaledBitmap(bmp, (bmp.width * r).toInt(), (bmp.height * r).toInt(), true)
    }

    private fun saveImage(uri: Uri, cr: ContentResolver): String = try {
        val input = cr.openInputStream(uri) ?: return ""
        val dir = File(app.filesDir, "ecg_images"); dir.mkdirs()
        val file = File(dir, "ecg_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out -> input.copyTo(out) }; input.close()
        file.absolutePath
    } catch (_: Exception) { "" }

    private fun extractInt(t: String, p: String) = Regex(p, RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)?.trim()?.toIntOrNull()
    private fun extractStr(t: String, p: String) = Regex(p, RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)?.trim()
}
