package com.roox.ecgpro.service

import kotlin.math.*

data class EcgParams(
    val rhythm: String = "normal_sinus",
    val heartRate: Int = 75,
    val prInterval: Int = 160,   // ms
    val qrsDuration: Int = 90,   // ms
    val stDeviation: Float = 0f, // mV (+ elevation, - depression)
    val tWaveInversion: Boolean = false,
    val pWavePresent: Boolean = true,
    val irregularRR: Boolean = false,
    val wideQRS: Boolean = false,
    val bundleBranch: String = "" // "lbbb", "rbbb", ""
)

object EcgWaveformSynthesizer {

    fun parseFromAiText(text: String): EcgParams {
        val lower = text.lowercase()
        var rhythm = "normal_sinus"
        var hr = 75
        var pr = 160
        var qrs = 90
        var stDev = 0f
        var tInv = false
        var pWave = true
        var irregularRR = false
        var wideQRS = false
        var bundle = ""

        // Detect heart rate
        val hrMatch = Regex("""heart\s*rate[:\s]*(\d+)""").find(lower)
            ?: Regex("""(\d+)\s*bpm""").find(lower)
        if (hrMatch != null) {
            hr = hrMatch.groupValues.get(1).toIntOrNull() ?: 75
        }

        // Detect PR interval
        val prMatch = Regex("""pr[:\s]*(\d+)\s*ms""").find(lower)
        if (prMatch != null) pr = prMatch.groupValues.get(1).toIntOrNull() ?: 160

        // Detect QRS duration
        val qrsMatch = Regex("""qrs[:\s]*(\d+)\s*ms""").find(lower)
        if (qrsMatch != null) {
            qrs = qrsMatch.groupValues.get(1).toIntOrNull() ?: 90
            if (qrs > 120) wideQRS = true
        }

        // Detect rhythm types
        when {
            lower.contains("atrial fibrillation") || lower.contains("a-fib") || lower.contains("afib") -> {
                rhythm = "afib"; irregularRR = true; pWave = false
            }
            lower.contains("atrial flutter") || lower.contains("aflutter") -> {
                rhythm = "aflutter"; pWave = false
            }
            lower.contains("ventricular tachycardia") || lower.contains("v-tach") || lower.contains("vtach") -> {
                rhythm = "vtach"; wideQRS = true; hr = hr.coerceAtLeast(150)
            }
            lower.contains("ventricular fibrillation") || lower.contains("v-fib") || lower.contains("vfib") -> {
                rhythm = "vfib"
            }
            lower.contains("sinus tachycardia") -> {
                rhythm = "sinus_tach"; hr = hr.coerceAtLeast(100)
            }
            lower.contains("sinus bradycardia") -> {
                rhythm = "sinus_brady"; hr = hr.coerceAtMost(60)
            }
            lower.contains("normal sinus") || lower.contains("sinus rhythm") -> {
                rhythm = "normal_sinus"
            }
        }

        // Detect ST changes
        if (lower.contains("st elevation") || lower.contains("stemi") || lower.contains("st-elevation")) {
            stDev = 0.25f
            if (rhythm == "normal_sinus") rhythm = "stemi"
        } else if (lower.contains("st depression")) {
            stDev = -0.15f
        }

        // T wave inversion
        if (lower.contains("t wave inversion") || lower.contains("t-wave inversion") || lower.contains("inverted t")) {
            tInv = true
        }

        // Bundle branch blocks
        if (lower.contains("lbbb") || lower.contains("left bundle branch")) {
            bundle = "lbbb"; wideQRS = true; qrs = qrs.coerceAtLeast(140)
        } else if (lower.contains("rbbb") || lower.contains("right bundle branch")) {
            bundle = "rbbb"; wideQRS = true; qrs = qrs.coerceAtLeast(130)
        }

        return EcgParams(rhythm, hr, pr, qrs, stDev, tInv, pWave, irregularRR, wideQRS, bundle)
    }

    fun generateWaveform(params: EcgParams, samples: Int = 1000): FloatArray {
        return when (params.rhythm) {
            "afib" -> generateAfibWaveform(params, samples)
            "aflutter" -> generateAflutterWaveform(params, samples)
            "vtach" -> generateVtachWaveform(params, samples)
            "vfib" -> generateVfibWaveform(samples)
            "stemi" -> generateSinusWithST(params, samples)
            else -> generateSinusWaveform(params, samples)
        }
    }

    private fun generateSinusWaveform(params: EcgParams, samples: Int): FloatArray {
        val data = FloatArray(samples)
        val cycleLen = (60.0 / params.heartRate * (samples / 2.5)).toInt().coerceAtLeast(1)
        for (i in 0 until samples) {
            val phase = (i % cycleLen).toDouble() / cycleLen
            data[i] = singleBeat(phase, params)
        }
        return data
    }

    private fun generateSinusWithST(params: EcgParams, samples: Int): FloatArray {
        val p = params.copy(stDeviation = if (params.stDeviation == 0f) 0.25f else params.stDeviation)
        return generateSinusWaveform(p, samples)
    }

    private fun singleBeat(phase: Double, params: EcgParams): Float {
        val pAmp = if (params.pWavePresent) 0.15 else 0.0
        val tSign = if (params.tWaveInversion) -1.0 else 1.0
        val stOff = params.stDeviation.toDouble()
        val qrsW = if (params.wideQRS) 1.6 else 1.0

        return when {
            // P wave (0.0 - 0.10)
            phase < 0.10 -> (pAmp * sin(phase / 0.10 * PI)).toFloat()
            // PR segment
            phase < 0.14 -> 0f
            // Q wave
            phase < 0.16 -> (-0.08 * sin((phase - 0.14) / 0.02 * PI)).toFloat()
            // R wave
            phase < 0.16 + 0.06 * qrsW -> {
                val rLen = 0.06 * qrsW
                (0.9 * sin((phase - 0.16) / rLen * PI)).toFloat()
            }
            // S wave
            phase < 0.16 + 0.08 * qrsW -> {
                val sStart = 0.16 + 0.06 * qrsW
                val sLen = 0.02 * qrsW
                (-0.12 * sin((phase - sStart) / sLen * PI) + stOff).toFloat()
            }
            // ST segment
            phase < 0.40 -> stOff.toFloat()
            // T wave
            phase < 0.55 -> (tSign * 0.2 * sin((phase - 0.40) / 0.15 * PI) + stOff * (1 - (phase - 0.40) / 0.15)).toFloat()
            // baseline
            else -> 0f
        }
    }

    private fun generateAfibWaveform(params: EcgParams, samples: Int): FloatArray {
        val data = FloatArray(samples)
        val random = java.util.Random(42)
        val avgCycle = (60.0 / params.heartRate * (samples / 2.5)).toInt().coerceAtLeast(30)
        var pos = 0
        while (pos < samples) {
            val rrJitter = (avgCycle * (0.6 + random.nextDouble() * 0.8)).toInt().coerceAtLeast(20)
            for (j in 0 until rrJitter.coerceAtMost(samples - pos)) {
                val phase = j.toDouble() / rrJitter
                data[pos + j] = singleBeat(phase, params.copy(pWavePresent = false)) + (random.nextGaussian() * 0.025).toFloat()
            }
            pos += rrJitter
        }
        return data
    }

    private fun generateAflutterWaveform(params: EcgParams, samples: Int): FloatArray {
        val data = FloatArray(samples)
        val flutterPeriod = 30
        val conductionRatio = 4
        val beatPeriod = flutterPeriod * conductionRatio
        for (i in 0 until samples) {
            // Sawtooth flutter waves
            val flutterPhase = (i % flutterPeriod).toDouble() / flutterPeriod
            data[i] = (-0.18 * (1.0 - flutterPhase) + 0.04).toFloat()
            // QRS every conductionRatio flutter waves
            val beatPhase = (i % beatPeriod).toDouble() / beatPeriod
            if (beatPhase < 0.15) {
                data[i] += singleBeat(beatPhase / 0.6, params.copy(pWavePresent = false)) * 0.8f
            }
        }
        return data
    }

    private fun generateVtachWaveform(params: EcgParams, samples: Int): FloatArray {
        val data = FloatArray(samples)
        val period = (60.0 / params.heartRate.coerceAtLeast(150) * (samples / 2.5)).toInt().coerceAtLeast(20)
        for (i in 0 until samples) {
            val phase = (i % period).toDouble() / period
            data[i] = (0.7 * sin(phase * 2 * PI) + 0.15 * sin(phase * 4 * PI) + 0.08 * sin(phase * 6 * PI)).toFloat()
        }
        return data
    }

    private fun generateVfibWaveform(samples: Int): FloatArray {
        val data = FloatArray(samples)
        val random = java.util.Random(99)
        for (i in 0 until samples) {
            data[i] = (random.nextGaussian() * 0.35 + 0.2 * sin(i * 0.25) + 0.15 * sin(i * 0.6)).toFloat()
        }
        return data
    }
}
