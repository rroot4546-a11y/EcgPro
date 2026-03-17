package com.roox.ecgpro.ui.result

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.roox.ecgpro.R
import com.roox.ecgpro.service.EcgWaveformSynthesizer
import com.roox.ecgpro.util.PdfReportGenerator
import com.roox.ecgpro.view.EcgGraphView
import com.roox.ecgpro.view.LeadImportanceView
import com.roox.ecgpro.viewmodel.EcgViewModel
import kotlinx.coroutines.launch
import java.io.File

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val vm = ViewModelProvider(this).get(EcgViewModel::class.java)
        val recordId = intent.getIntExtra("record_id", -1)
        if (recordId == -1) { finish(); return }

        val ivEcg = findViewById<ImageView>(R.id.ivOriginalEcg)
        val graphView = findViewById<EcgGraphView>(R.id.ecgGraphView)
        val tvGraphLabel = findViewById<TextView>(R.id.tvGraphLabel)
        val tvDiagnosis = findViewById<TextView>(R.id.tvDiagnosis)
        val tvConfidence = findViewById<TextView>(R.id.tvConfidence)
        val tvUrgency = findViewById<TextView>(R.id.tvUrgency)
        val tvMeasurements = findViewById<TextView>(R.id.tvMeasurements)
        val tvAnalysis = findViewById<TextView>(R.id.tvAnalysis)
        val tvModel = findViewById<TextView>(R.id.tvModel)
        val tvPatient = findViewById<TextView>(R.id.tvPatient)
        val tvAcsRisk = findViewById<TextView>(R.id.tvAcsRisk)
        val cardAcs = findViewById<View>(R.id.cardAcs)
        val tvLvef = findViewById<TextView>(R.id.tvLvef)
        val leadView = findViewById<LeadImportanceView>(R.id.leadImportanceView)
        val btnPdf = findViewById<Button>(R.id.btnPdf)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            val r = vm.getRecord(recordId) ?: return@launch

            tvPatient.text = buildString {
                append("👤 ")
                if (r.patientName.isNotBlank()) append(r.patientName)
                if (r.patientAge.isNotBlank()) append(", ${r.patientAge}y")
                if (r.patientGender.isNotBlank()) append(" (${r.patientGender})")
            }

            if (r.imagePath.isNotBlank()) ivEcg.load(File(r.imagePath)) { crossfade(true) }

            // AI Graph
            try {
                val params = EcgWaveformSynthesizer.parseFromAiText(r.fullAnalysis)
                val waveform = EcgWaveformSynthesizer.generateWaveform(params)
                graphView.setEcgData(waveform, "${params.rhythm} — ${params.heartRate} bpm")
                tvGraphLabel.text = "🫀 ${params.rhythm} — ${params.heartRate} bpm"
            } catch (_: Exception) {
                graphView.setEcgData(EcgGraphView.generateNormalSinus(75, 500), "Normal Sinus")
                tvGraphLabel.text = "ECG Waveform"
            }

            // Diagnosis
            tvDiagnosis.text = r.diagnosis.ifBlank { "Analysis pending" }
            tvConfidence.text = if (r.confidence > 0) "Confidence: ${r.confidence}%" else ""
            tvUrgency.text = when (r.urgencyLevel.lowercase()) {
                "emergent" -> "🔴 EMERGENT"; "urgent" -> "🟡 URGENT"; else -> "🟢 ROUTINE"
            }
            tvUrgency.setTextColor(when (r.urgencyLevel.lowercase()) {
                "emergent" -> 0xFFD32F2F.toInt(); "urgent" -> 0xFFF57C00.toInt(); else -> 0xFF388E3C.toInt()
            })

            // ACS Card
            if (r.acsRisk.isNotBlank()) {
                cardAcs.visibility = View.VISIBLE
                tvAcsRisk.text = "🚨 ACS: ${r.acsRisk}"
                tvAcsRisk.setTextColor(when (r.acsRisk.lowercase()) {
                    "confirmed" -> 0xFFD32F2F.toInt()
                    "indeterminate" -> 0xFFF57C00.toInt()
                    "notomi" -> 0xFF388E3C.toInt()
                    else -> 0xFF757575.toInt()
                })
            }

            // LVEF
            if (r.lvefStatus.isNotBlank()) {
                tvLvef.visibility = View.VISIBLE
                tvLvef.text = "💓 LVEF: ${r.lvefStatus}"
            }

            // Lead Importance
            if (r.leadImportance.isNotBlank() || r.fullAnalysis.contains("LEAD IMPORTANCE", ignoreCase = true)) {
                leadView.visibility = View.VISIBLE
                leadView.parseFromText(r.leadImportance.ifBlank { r.fullAnalysis })
            }

            // Measurements
            tvMeasurements.text = buildString {
                if (r.heartRate > 0) appendLine("❤️ HR: ${r.heartRate} bpm")
                if (r.rhythm.isNotBlank()) appendLine("〰️ Rhythm: ${r.rhythm}")
                if (r.axis.isNotBlank()) appendLine("🧭 Axis: ${r.axis} ${if(r.axisClassification.isNotBlank()) "(${r.axisClassification})" else ""}")
                if (r.prInterval.isNotBlank()) appendLine("📏 PR: ${r.prInterval}")
                if (r.qrsDuration.isNotBlank()) appendLine("📐 QRS: ${r.qrsDuration}")
                if (r.qtInterval.isNotBlank()) appendLine("⏱️ QT: ${r.qtInterval}")
                appendLine("📐 Layout: ${r.ecgLayout}")
                appendLine("⏱️ Speed: ${r.paperSpeed} mm/s | Gain: ${r.voltageGain} mm/mV")
            }

            tvAnalysis.text = r.fullAnalysis
            tvModel.text = "🤖 ${r.aiModel}"

            btnPdf.setOnClickListener { PdfReportGenerator.generateAndShare(this@ResultActivity, r) }
        }
    }
}
