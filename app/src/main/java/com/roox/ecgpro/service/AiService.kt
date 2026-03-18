package com.roox.ecgpro.service

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
        Model("openai/gpt-4o", "GPT-4o ⭐", "Best for ECG images — highest accuracy"),
        Model("google/gemini-2.5-pro-preview", "Gemini 2.5 Pro", "Best medical reasoning"),
        Model("anthropic/claude-sonnet-4", "Claude Sonnet 4", "Detailed clinical analysis"),
        Model("google/gemini-2.0-flash-001", "Gemini 2.0 Flash", "Fast & free", true),
        Model("meta-llama/llama-4-maverick", "Llama 4 Maverick", "Open-source medical AI"),
        Model("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3", "Free tier", true),
    )
    fun default() = list.get(0)  // GPT-4o as default for best accuracy
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

    suspend fun analyzeEcg(img64: String, symptoms: String, age: String, gender: String,
                           history: String = "", layout: String = "SinglePage",
                           paperSpeed: String = "25", voltageGain: String = "10"): String {
        val sys = EcgExpertPrompt.buildAnalysisPrompt(layout, paperSpeed, voltageGain)
        val user = buildString {
            append("Patient: Age $age, $gender\n")
            if (symptoms.isNotBlank()) append("Symptoms: $symptoms\n")
            if (history.isNotBlank()) append("Clinical History: $history\n")
            append("\nAnalyze this 12-lead ECG image with FULL systematic approach.")
            append("\nBe extremely precise with measurements and diagnoses.")
            append("\nReport EVERY abnormality you see, no matter how subtle.")
        }
        return callVision(sys, user, img64)
    }

    suspend fun chat(message: String, img64: String? = null, chatHistory: List<Map<String, Any>> = emptyList()): String {
        val sys = EcgExpertPrompt.buildChatPrompt()
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
        return callApi(messages, 3000)
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
        return callApi(msgs, 4096)
    }

    private suspend fun callApi(messages: List<Map<String, Any>>, maxTokens: Int): String = suspendCoroutine { cont ->
        if (apiKey.isBlank()) { cont.resume("⚠️ No API key configured. Go to Settings → enter your OpenRouter API key.\n\nGet a free key at: openrouter.ai/keys"); return@suspendCoroutine }
        val body = gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to maxTokens, "temperature" to 0.2))
        val req = Request.Builder().url(URL)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/rroot4546-a11y/EcgPro")
            .addHeader("X-Title", "ECG Pro").build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(c: Call, e: IOException) { cont.resume("❌ Network error: ${e.message}\n\nCheck your internet connection.") }
            override fun onResponse(c: Call, r: Response) {
                try {
                    val rb = r.body?.string() ?: ""
                    if (!r.isSuccessful) {
                        cont.resume(when(r.code) {
                            401 -> "❌ Invalid API key. Check Settings."
                            402 -> "❌ No credits. Add credits at openrouter.ai or switch to a free model."
                            429 -> "❌ Rate limited. Wait a moment and try again."
                            else -> "❌ Error ${r.code}: ${rb.take(200)}"
                        }); return
                    }
                    val j = JsonParser.parseString(rb).asJsonObject
                    val ch = j.getAsJsonArray("choices")
                    if (ch != null && ch.size() > 0) cont.resume(ch.get(0).asJsonObject.getAsJsonObject("message").get("content").asString)
                    else cont.resume("No response from AI model.")
                } catch (e: Exception) { cont.resume("❌ Parse error: ${e.message}") }
            }
        })
    }
}
