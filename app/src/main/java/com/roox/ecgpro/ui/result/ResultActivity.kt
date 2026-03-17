package com.roox.ecgpro.ui.result

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.roox.ecgpro.R
import com.roox.ecgpro.service.EcgWaveformSynthesizer
import com.roox.ecgpro.util.PdfReportGenerator
import com.roox.ecgpro.view.EcgGraphView
import com.roox.ecgpro.view.LeadImportanceView
import com.roox.ecgpro.viewmodel.EcgViewModel
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // v2.0 views
        val acsCard = findViewById<View>(R.id.cardAcsRisk)
        val tvAcsRisk = findViewById<TextView>(R.id.tvAcsRisk)
        val tvAcsDetail = findViewById<TextView>(R.id.tvAcsDetail)
        val leadImportanceView = findViewById<LeadImportanceView>(R.id.leadImportanceView)
        val btnPdf = findViewById<Button>(R.id.btnGeneratePdf)
        val tvEcgSettings = findViewById<TextView>(R.id.tvEcgSettings)

        // Tabs: Visualization vs Original
        val tabLayout = findViewById<TabLayout>(R.id.tabEcgView)
        val visualizationContainer = findViewById<View>(R.id.layoutVisualization)
        val originalContainer = findViewById<View>(R.id.layoutOriginal)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        visualizationContainer.visibility = View.VISIBLE
                        originalContainer.visibility = View.GONE
                    }
                    1 -> {
                        visualizationContainer.visibility = View.GONE
                        originalContainer.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        lifecycleScope.launch {
            val r = vm.getRecord(recordId) ?: return@launch

            // Patient info
            tvPatient.text = buildString {
                append("👤 ")
                if (r.patientName.isNotBlank()) append(r.patientName)
                if (r.patientAge.isNotBlank()) append(", ${r.patientAge}y")
                if (r.patientGender.isNotBlank()) append(" (${r.patientGender})")
            }

            // ECG Settings
            tvEcgSettings.text = "📐 ${r.ecgLayout.replace("_", " ")} | ${r.paperSpeed}mm/s | ${r.voltageGain}mm/mV"

            // Original image
            if (r.imagePath.isNotBlank()) ivEcg.load(File(r.imagePath)) { crossfade(true) }

            // AI-synthesized graph
            try {
                val params = EcgWaveformSynthesizer.parseFromAiText(r.fullAnalysis)
                val waveform = EcgWaveformSynthesizer.generateWaveform(params)
                graphView.setEcgData(waveform, "${params.rhythm} — ${params.heartRate} bpm")
                tvGraphLabel.text = "🫀 ${params.rhythm} — ${params.heartRate} bpm"
            } catch (_: Exception) {
                tvGraphLabel.text = "ECG Waveform"
                graphView.setEcgData(EcgGraphView.generateNormalSinus(75, 500), "Normal Sinus")
            }

            // Lead importance heatmap
            if (r.leadImportance.isNotBlank() && r.leadImportance != "{}") {
                leadImportanceView.setImportanceFromJson(r.leadImportance)
                leadImportanceView.visibility = View.VISIBLE
            }

            // ACS Risk Card
            if (r.acsRisk.isNotBlank() && r.acsRisk != "unknown") {
                acsCard.visibility = View.VISIBLE
                val riskDisplay = r.acsRisk.uppercase().replace("_", " ")
                tvAcsRisk.text = riskDisplay

                when (r.acsRisk.lowercase()) {
                    "confirmed" -> {
                        tvAcsRisk.setTextColor(0xFFFFFFFF.toInt())
                        acsCard.setBackgroundColor(0xFFD32F2F.toInt())
                        tvAcsDetail.text = "🔴 ACS CONFIRMED — Immediate intervention needed"
                        tvAcsDetail.setTextColor(0xFFFFFFFF.toInt())
                    }
                    "indeterminate" -> {
                        tvAcsRisk.setTextColor(0xFF333333.toInt())
                        acsCard.setBackgroundColor(0xFFFFF176.toInt())
                        tvAcsDetail.text = "🟡 Indeterminate — Serial ECGs and troponin recommended"
                        tvAcsDetail.setTextColor(0xFF333333.toInt())
                    }
                    "not_omi" -> {
                        tvAcsRisk.setTextColor(0xFFFFFFFF.toInt())
                        acsCard.setBackgroundColor(0xFF388E3C.toInt())
                        tvAcsDetail.text = "🟢 Not OMI — Low ACS risk based on ECG pattern"
                        tvAcsDetail.setTextColor(0xFFFFFFFF.toInt())
                    }
                    "outside_population" -> {
                        tvAcsRisk.setTextColor(0xFF333333.toInt())
                        acsCard.setBackgroundColor(0xFFBDBDBD.toInt())
                        tvAcsDetail.text = "⚪ Outside population — Algorithm may not apply"
                        tvAcsDetail.setTextColor(0xFF333333.toInt())
                    }
                    "reperfused" -> {
                        tvAcsRisk.setTextColor(0xFFFFFFFF.toInt())
                        acsCard.setBackgroundColor(0xFF1565C0.toInt())
                        tvAcsDetail.text = "🔵 Reperfused — Signs of successful reperfusion"
                        tvAcsDetail.setTextColor(0xFFFFFFFF.toInt())
                    }
                    else -> {
                        tvAcsRisk.setTextColor(0xFF333333.toInt())
                        acsCard.setBackgroundColor(0xFFE0E0E0.toInt())
                        tvAcsDetail.text = "Presentation data may be incomplete"
                        tvAcsDetail.setTextColor(0xFF666666.toInt())
                    }
                }
            } else {
                acsCard.visibility = View.GONE
            }

            // Diagnosis
            tvDiagnosis.text = r.diagnosis.ifBlank { "Analysis pending" }
            tvConfidence.text = if (r.confidence > 0) "Confidence: ${r.confidence}%" else ""
            tvUrgency.text = when (r.urgencyLevel.lowercase()) {
                "emergent" -> "🔴 EMERGENT — Immediate action required"
                "urgent" -> "🟡 URGENT — Prompt evaluation needed"
                else -> "🟢 ROUTINE"
            }
            tvUrgency.setTextColor(when (r.urgencyLevel.lowercase()) {
                "emergent" -> 0xFFD32F2F.toInt()
                "urgent" -> 0xFFF57C00.toInt()
                else -> 0xFF388E3C.toInt()
            })

            // Measurements
            tvMeasurements.text = buildString {
                if (r.heartRate > 0) appendLine("❤️ Heart Rate: ${r.heartRate} bpm")
                if (r.rhythm.isNotBlank()) appendLine("〰️ Rhythm: ${r.rhythm}")
                if (r.axis.isNotBlank()) appendLine("🧭 Axis: ${r.axis}")
                if (r.prInterval.isNotBlank()) appendLine("📏 PR Interval: ${r.prInterval}")
                if (r.qrsDuration.isNotBlank()) appendLine("📐 QRS Duration: ${r.qrsDuration}")
                if (r.qtInterval.isNotBlank()) appendLine("⏱️ QT/QTc: ${r.qtInterval}")
                if (r.stChanges.isNotBlank()) appendLine("📊 ST Changes: ${r.stChanges}")
            }

            // Full analysis
            tvAnalysis.text = r.fullAnalysis

            // Model
            tvModel.text = "🤖 ${r.aiModel}"

            // PDF button
            btnPdf.setOnClickListener {
                btnPdf.isEnabled = false
                btnPdf.text = "Generating..."
                lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            PdfReportGenerator.generate(this@ResultActivity, r)
                        }
                        Toast.makeText(this@ResultActivity, "✅ PDF saved to Downloads!", Toast.LENGTH_LONG).show()

                        // Share intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            if (result.uri != null) {
                                putExtra(Intent.EXTRA_STREAM, result.uri)
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
                    } catch (e: Exception) {
                        Toast.makeText(this@ResultActivity, "❌ PDF generation failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        btnPdf.isEnabled = true
                        btnPdf.text = "📄 Generate PDF Report"
                    }
                }
            }
        }
    }
}
