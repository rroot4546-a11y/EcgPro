package com.roox.ecgpro.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AiModels {
    data class Model(val id: String, val name: String, val desc: String, val free: Boolean = false)
    val list = listOf(
        Model("google/gemini-2.5-pro-preview", "Gemini 2.5 Pro", "Best medical reasoning"),
        Model("anthropic/claude-sonnet-4", "Claude Sonnet 4", "Detailed clinical analysis"),
        Model("openai/gpt-4o", "GPT-4o", "Strong vision + medical knowledge"),
        Model("google/gemini-2.0-flash-001", "Gemini 2.0 Flash", "Fast & free", true),
        Model("meta-llama/llama-4-maverick", "Llama 4 Maverick", "Open-source medical AI"),
        Model("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3", "Free tier", true),
    )
    fun default() = list.get(3)
}

/** All 38 PMcardio diagnostic codes */
object DiagnosticCodes {
    data class DiagCode(val code: String, val name: String, val group: String)

    val all = listOf(
        // Rhythm
        DiagCode("sinbrad", "Sinus Bradycardia", "Rhythm"),
        DiagCode("sinrhy", "Sinus Rhythm", "Rhythm"),
        DiagCode("sintach", "Sinus Tachycardia", "Rhythm"),
        DiagCode("pacerhy", "Pacemaker Rhythm", "Rhythm"),
        DiagCode("afib", "Atrial Fibrillation", "Rhythm"),
        DiagCode("afibrapid", "Atrial Fibrillation (Rapid)", "Rhythm"),
        DiagCode("afibslow", "Atrial Fibrillation (Slow)", "Rhythm"),
        DiagCode("aflut", "Atrial Flutter", "Rhythm"),
        DiagCode("aflutrapid", "Atrial Flutter (Rapid)", "Rhythm"),
        DiagCode("aflutslow", "Atrial Flutter (Slow)", "Rhythm"),
        DiagCode("svt", "Supraventricular Tachycardia", "Rhythm"),
        DiagCode("junrhy", "Junctional Rhythm", "Rhythm"),
        DiagCode("junbrad", "Junctional Bradycardia", "Rhythm"),
        DiagCode("accjunrhy", "Accelerated Junctional Rhythm", "Rhythm"),
        DiagCode("wqrsrhy", "Wide QRS Rhythm", "Rhythm"),
        DiagCode("idiovenrhy", "Idioventricular Rhythm", "Rhythm"),
        DiagCode("wqrstach", "Wide QRS Tachycardia", "Rhythm"),
        DiagCode("pcom", "Premature Complex", "Rhythm"),
        // Conduction Abnormalities
        DiagCode("avblock1", "1st Degree AV Block", "Conduction Abnormalities"),
        DiagCode("avblock2w", "2nd Degree AV Block (Wenckebach)", "Conduction Abnormalities"),
        DiagCode("avblockhd", "High Degree AV Block", "Conduction Abnormalities"),
        DiagCode("rbbb", "Right Bundle Branch Block", "Conduction Abnormalities"),
        DiagCode("irbbb", "Incomplete Right Bundle Branch Block", "Conduction Abnormalities"),
        DiagCode("lbbb", "Left Bundle Branch Block", "Conduction Abnormalities"),
        DiagCode("ilbbb", "Incomplete Left Bundle Branch Block", "Conduction Abnormalities"),
        DiagCode("ivcondelay", "Intraventricular Conduction Delay", "Conduction Abnormalities"),
        DiagCode("lafb", "Left Anterior Fascicular Block", "Conduction Abnormalities"),
        DiagCode("lpfb", "Left Posterior Fascicular Block", "Conduction Abnormalities"),
        DiagCode("bifasblocka", "Bifascicular Block (Anterior)", "Conduction Abnormalities"),
        DiagCode("bifasblockp", "Bifascicular Block (Posterior)", "Conduction Abnormalities"),
        DiagCode("trifasblocka", "Trifascicular Block (Anterior)", "Conduction Abnormalities"),
        DiagCode("trifasblockp", "Trifascicular Block (Posterior)", "Conduction Abnormalities"),
        // Other
        DiagCode("longqtsyn", "Long QT Syndrome", "Other"),
        DiagCode("shortqtsyn", "Short QT Syndrome", "Other"),
        DiagCode("atrenl", "Atrial Enlargement", "Other"),
        DiagCode("venhyp", "Ventricular Hypertrophy", "Other"),
        DiagCode("stemia", "STEMI (Acute)", "Other"),
        DiagCode("nstemi", "NSTEMI", "Other"),
    )

    fun codeListString(): String {
        val sb = StringBuilder()
        var currentGroup = ""
        for (dc in all) {
            if (dc.group != currentGroup) {
                currentGroup = dc.group
                sb.append("\n[$currentGroup]\n")
            }
            sb.append("  ${dc.code} = ${dc.name}\n")
        }
        return sb.toString()
    }
}

class AiService(private val apiKey: String, private val model: String) {
    companion object {
        private const val TAG = "AiService"
        private const val URL = "https://openrouter.ai/api/v1/chat/completions"
        fun fromPrefs(p: android.content.SharedPreferences) = AiService(
            p.getString("api_key", "") ?: "",
            p.getString("model", AiModels.default().id) ?: AiModels.default().id
        )
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS).build()
    private val gson = Gson()

    /** Full ECG analysis with image — v2.0 enhanced with 38 codes, ACS, LVEF, lead importance */
    suspend fun analyzeEcg(
        img64: String,
        symptoms: String,
        age: String,
        gender: String,
        history: String = "",
        ecgLayout: String = "standard_3x2",
        paperSpeed: String = "25",
        voltageGain: String = "10"
    ): String {
        val sys = buildString {
            append("You are Root (روت), a senior cardiologist AI assistant with 20+ years of experience in ECG interpretation.\n")
            append("You work inside the ECG Pro app to help Iraqi Board residents learn and diagnose.\n")
            append("Use latest AHA/ACC/HRS guidelines, Braunwald's Heart Disease, Harrison's, and Marriott's Practical ECG.\n\n")

            append("ECG Settings: Layout=$ecgLayout, Paper Speed=${paperSpeed}mm/s, Voltage Gain=${voltageGain}mm/mV\n\n")

            append("=== DIAGNOSTIC CODES (use these exact codes) ===\n")
            append(DiagnosticCodes.codeListString())
            append("\n")

            append("Analyze the 12-lead ECG systematically. Output in EXACTLY this format:\n\n")

            append("━━━ 📊 TECHNICAL PARAMETERS ━━━\n")
            append("Heart Rate: ___ bpm\n")
            append("Rhythm: ___\n")
            append("Axis: ___° (Classification: Normal|Left Axis Deviation|Right Axis Deviation|Extreme)\n")
            append("PR: ___ ms\n")
            append("QRS: ___ ms\n")
            append("QT/QTc: ___ ms\n\n")

            append("━━━ 🔍 SYSTEMATIC ANALYSIS ━━━\n")
            append("P Waves:\nPR Interval:\nQRS Complex:\nST Segment:\nT Waves:\nU Waves:\n\n")

            append("━━━ 🏷️ DIAGNOSTIC CODES ━━━\n")
            append("List ALL applicable codes with confidence:\n")
            append("FORMAT: code|Full Name|confidence(high/medium/low)|group(Rhythm/Conduction Abnormalities/Other)\n")
            append("Example:\n")
            append("sinrhy|Sinus Rhythm|high|Rhythm\n")
            append("rbbb|Right Bundle Branch Block|medium|Conduction Abnormalities\n\n")

            append("━━━ 🫀 PRIMARY DIAGNOSIS ━━━\n")
            append("[Diagnosis] (Confidence: ___%)\n\n")

            append("━━━ ⚠️ SECONDARY FINDINGS ━━━\n\n")

            append("━━━ 💉 LVEF ESTIMATION ━━━\n")
            append("LVEF Status: [Reduced(<40%)|Mildly Reduced(40-49%)|Preserved(>=50%)|Inconclusive]\n")
            append("Estimated LVEF: ___%\n")
            append("Basis: [explain ECG signs used]\n\n")

            append("━━━ 🚑 ACS ASSESSMENT ━━━\n")
            append("ACS Suspicion: [YesWithSymptoms|YesWithoutSymptoms|No|Unknown]\n")
            append("STEMI Presentation: [CONFIRMED|OUTSIDE_POPULATION|UNKNOWN]\n")
            append("ACS Risk: [Confirmed|Indeterminate|Not OMI|Outside Population|Reperfused|Presentation Missing]\n")
            append("Culprit Vessel: [LAD|RCA|LCx|None|Unclear]\n\n")

            append("━━━ 🔥 LEAD IMPORTANCE ━━━\n")
            append("Rate each lead's diagnostic importance for this ECG:\n")
            append("FORMAT: Lead:level (critical/high/moderate/low/normal)\n")
            append("I:___ II:___ III:___ aVR:___ aVL:___ aVF:___ V1:___ V2:___ V3:___ V4:___ V5:___ V6:___\n\n")

            append("━━━ 🏥 CLINICAL CORRELATION ━━━\n\n")

            append("━━━ 🚨 URGENCY ━━━\n")
            append("Level: [🟢 Routine / 🟡 Urgent / 🔴 Emergent]\nAction:\n\n")

            append("━━━ 💊 DIFFERENTIALS ━━━\n1.\n2.\n3.\n\n")

            append("━━━ 📚 REFERENCES ━━━\n\n")

            append("If the patient symptoms are written in Arabic, respond in Arabic. Otherwise respond in English.\n")
        }
        val user = buildString {
            append("Patient: Age $age, $gender\n")
            append("Symptoms: $symptoms")
            if (history.isNotBlank()) append("\nHistory: $history")
            append("\n\nAnalyze this 12-lead ECG.")
        }
        return callVision(sys, user, img64)
    }

    /** Chat with Root — the AI cardiology assistant */
    suspend fun chat(message: String, img64: String? = null, chatHistory: List<Map<String, Any>> = emptyList()): String {
        val sys = buildString {
            append("You are Root (روت 🌱), an AI cardiology assistant inside the ECG Pro app.\n")
            append("You help Iraqi Board Internal Medicine residents interpret ECGs and learn cardiology.\n")
            append("You're knowledgeable, direct, and concise. You reference Harrison's, Braunwald's, and ACC/AHA guidelines.\n")
            append("If the user sends an ECG image, analyze it. If they ask questions, answer with clinical accuracy.\n")
            append("You can speak Arabic or English based on what the user uses.\n")
            append("Be helpful but always remind: AI interpretation should be verified by a cardiologist.")
        }
        val messages = mutableListOf<Map<String, Any>>(mapOf("role" to "system", "content" to sys))
        // Add recent chat history for context
        chatHistory.takeLast(10).forEach { messages.add(it) }

        if (img64 != null) {
            messages.add(mapOf("role" to "user", "content" to listOf(
                mapOf("type" to "text", "text" to message),
                mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$img64"))
            )))
        } else {
            messages.add(mapOf("role" to "user", "content" to message))
        }
        return callApi(messages, 2000)
    }

    suspend fun test(): Boolean = try {
        val r = callApi(listOf(mapOf("role" to "user", "content" to "Reply OK")), 50)
        r.isNotBlank() && !r.startsWith("❌")
    } catch (_: Exception) { false }

    private suspend fun callVision(sys: String, user: String, img64: String): String {
        val msgs = listOf(
            mapOf("role" to "system", "content" to sys),
            mapOf("role" to "user", "content" to listOf(
                mapOf("type" to "text", "text" to user),
                mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$img64"))
            ))
        )
        return callApi(msgs, 6000)
    }

    private suspend fun callApi(messages: List<Map<String, Any>>, maxTokens: Int): String = suspendCoroutine { cont ->
        if (apiKey.isBlank()) { cont.resume("⚠️ No API key. Go to Settings."); return@suspendCoroutine }
        val body = gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to maxTokens, "temperature" to 0.3))
        val req = Request.Builder().url(URL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/rroot4546-a11y/EcgPro")
            .addHeader("X-Title", "ECG Pro").build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(c: Call, e: IOException) { cont.resume("❌ Network: ${e.message}") }
            override fun onResponse(c: Call, r: Response) {
                try {
                    val rb = r.body?.string() ?: ""
                    if (!r.isSuccessful) { cont.resume(when(r.code){401->"❌ Invalid key";402->"❌ No credits";429->"❌ Rate limited";else->"❌ Error ${r.code}"}); return }
                    val j = JsonParser.parseString(rb).asJsonObject
                    val ch = j.getAsJsonArray("choices")
                    if (ch != null && ch.size() > 0) cont.resume(ch.get(0).asJsonObject.getAsJsonObject("message").get("content").asString)
                    else cont.resume("No response.")
                } catch (e: Exception) { cont.resume("❌ ${e.message}") }
            }
        })
    }
}
