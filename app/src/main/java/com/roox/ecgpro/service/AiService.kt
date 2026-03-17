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
        Model("openai/gpt-4o", "GPT-4o", "Strong vision + medical"),
        Model("google/gemini-2.0-flash-001", "Gemini 2.0 Flash", "Fast & free", true),
        Model("meta-llama/llama-4-maverick", "Llama 4 Maverick", "Open-source medical AI"),
        Model("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3", "Free tier", true),
    )
    fun default() = list.get(3)
}

class AiService(private val apiKey: String, private val model: String) {
    companion object {
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

    suspend fun analyzeEcg(img64: String, symptoms: String, age: String, gender: String, history: String = "", layout: String = "SinglePage", paperSpeed: String = "25", voltageGain: String = "10"): String {
        val sys = buildString {
            append("You are Root (روت), a senior cardiologist AI with 20+ years ECG experience.\n")
            append("Use AHA/ACC/HRS guidelines, Braunwald's, Harrison's, Marriott's Practical ECG.\n")
            append("If symptoms are in Arabic, respond in Arabic. Otherwise English.\n\n")
            append("ECG Settings: Layout=$layout, Paper Speed=${paperSpeed}mm/s, Voltage Gain=${voltageGain}mm/mV\n\n")
            append("Analyze the 12-lead ECG. Output EXACTLY in this structured format:\n\n")
            append("━━━ 📊 TECHNICAL PARAMETERS ━━━\n")
            append("Heart Rate: ___ bpm\nRhythm: ___\nAxis: ___° (Classification: Normal/Left/Right/Extreme)\n")
            append("P Wave: ___ ms, ___ mV\nPR Interval: ___ ms\nQRS Duration: ___ ms\nQT/QTc: ___ ms\n")
            append("Paper Speed: ${paperSpeed} mm/s\nVoltage: ${voltageGain} mm/mV\n\n")
            append("━━━ 🔍 SYSTEMATIC ANALYSIS ━━━\nP Waves:\nPR Interval:\nQRS Complex:\nST Segment:\nT Waves:\nU Waves:\n\n")
            append("━━━ 🫀 DIAGNOSES (use codes) ━━━\n")
            append("Group each diagnosis with confidence (High/Medium/Low):\n")
            append("RHYTHM: [code](confidence) e.g. sinrhy(High), afib(Medium)\n")
            append("CONDUCTION: [code](confidence) e.g. rbbb(High), avblock1(Low)\n")
            append("OTHER: [code](confidence) e.g. stemia(High), venhyp(Medium)\n\n")
            append("Available codes: sinbrad, sinrhy, sintach, pacerhy, afib, afibrapid, afibslow, ")
            append("aflut, aflutrapid, aflutslow, svt, junrhy, junbrad, accjunrhy, wqrsrhy, ")
            append("idiovenrhy, wqrstach, pcom, avblock1, avblock2w, avblockhd, rbbb, irbbb, ")
            append("lbbb, ilbbb, ivcondelay, lafb, lpfb, bifasblocka, bifasblockp, ")
            append("trifasblocka, trifasblockp, longqtsyn, shortqtsyn, atrenl, venhyp, stemia, nstemi\n\n")
            append("━━━ 🚨 ACS ASSESSMENT ━━━\n")
            append("ACS Suspicion: [YesWithSymptoms/YesWithoutSymptoms/No/Unknown]\n")
            append("STEMI Presentation: [CONFIRMED/OUTSIDE_POPULATION/UNKNOWN]\n")
            append("ACS Risk: [Confirmed/Indeterminate/NotOMI/OutsidePopulation/Reperfused/PresentationMissing]\n\n")
            append("━━━ 💓 LVEF ESTIMATION ━━━\n")
            append("LVEF Status: [Reduced(<40%)/MildlyReduced(40-49%)/Negative(>=50%)/Inconclusive]\n\n")
            append("━━━ 🔥 LEAD IMPORTANCE ━━━\n")
            append("Rate each lead: I(), II(), III(), aVR(), aVL(), aVF(), V1(), V2(), V3(), V4(), V5(), V6()\n")
            append("Levels: critical/high/moderate/low/normal\n\n")
            append("━━━ 🏥 CLINICAL CORRELATION ━━━\n\n")
            append("━━━ 🚨 URGENCY ━━━\nLevel: [🟢 Routine / 🟡 Urgent / 🔴 Emergent]\nAction:\n\n")
            append("━━━ 💊 DIFFERENTIALS ━━━\n1.\n2.\n3.\n\n")
            append("━━━ 📚 REFERENCES ━━━\n")
        }
        val user = buildString {
            append("Patient: Age $age, $gender\n")
            if (symptoms.isNotBlank()) append("Symptoms: $symptoms\n")
            if (history.isNotBlank()) append("History: $history\n")
            append("\nAnalyze this 12-lead ECG.")
        }
        return callVision(sys, user, img64)
    }

    suspend fun chat(message: String, img64: String? = null, chatHistory: List<Map<String, Any>> = emptyList()): String {
        val sys = buildString {
            append("You are Root (روت 🌱), an AI cardiology assistant inside ECG Pro.\n")
            append("You help Iraqi Board Internal Medicine residents interpret ECGs and learn cardiology.\n")
            append("You're knowledgeable, direct, concise. Reference Harrison's, Braunwald's, ACC/AHA guidelines.\n")
            append("If user speaks Arabic, respond in Arabic. If English, respond in English.\n")
            append("If they send an ECG image, analyze it fully.\n")
            append("Always remind: AI interpretation should be verified by a cardiologist.\n")
            append("You know 38 core ECG diagnoses, ACS/OMI assessment, LVEF estimation, and axis classification.")
        }
        val messages = mutableListOf<Map<String, Any>>(mapOf("role" to "system", "content" to sys))
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
        return callApi(msgs, 4000)
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
