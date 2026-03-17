package com.roox.ecgpro.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.roox.ecgpro.data.model.EcgRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class PdfResult(val filePath: String, val uri: android.net.Uri?)

    fun generate(context: Context, record: EcgRecord): PdfResult {
        val doc = PdfDocument()
        val pageWidth = 595  // A4
        val pageHeight = 842

        // === PAGE 1 ===
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = doc.startPage(pageInfo1)
        val canvas1 = page1.canvas
        drawPage1(canvas1, record, pageWidth, context)
        doc.finishPage(page1)

        // === PAGE 2 (Full Analysis) ===
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = doc.startPage(pageInfo2)
        val canvas2 = page2.canvas
        drawPage2(canvas2, record, pageWidth, pageHeight)
        doc.finishPage(page2)

        // Save to Downloads
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "ECG_Report_${record.patientName.replace(" ", "_")}_$timestamp.pdf"

        var resultUri: android.net.Uri? = null
        val filePath: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                resultUri = uri
            }
            filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { doc.writeTo(it) }
            filePath = file.absolutePath
        }

        doc.close()
        return PdfResult(filePath, resultUri)
    }

    private fun drawPage1(canvas: Canvas, record: EcgRecord, pageWidth: Int, context: Context) {
        val margin = 40f

        // Red header
        val headerPaint = Paint().apply { color = Color.parseColor("#D32F2F"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 80f, headerPaint)

        val titlePaint = Paint().apply { color = Color.WHITE; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText("🫀 ECG Pro — AI Analysis Report", margin, 52f, titlePaint)

        var y = 100f
        val textPaint = Paint().apply { color = Color.parseColor("#333333"); textSize = 12f; isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.parseColor("#333333"); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val smallPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = 10f; isAntiAlias = true }
        val sectionPaint = Paint().apply { color = Color.parseColor("#D32F2F"); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Date
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(record.timestamp))
        canvas.drawText("Date: $dateStr", margin, y, textPaint)
        y += 20f
        canvas.drawText("AI Model: ${record.aiModel}", margin, y, smallPaint)
        y += 25f

        // Patient Info section
        canvas.drawText("PATIENT INFORMATION", margin, y, sectionPaint)
        y += 5f
        val linePaint = Paint().apply { color = Color.parseColor("#D32F2F"); strokeWidth = 1.5f }
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        canvas.drawText("Name: ${record.patientName.ifBlank { "N/A" }}", margin, y, boldPaint)
        canvas.drawText("Age: ${record.patientAge.ifBlank { "N/A" }}", 300f, y, textPaint)
        canvas.drawText("Gender: ${record.patientGender}", 420f, y, textPaint)
        y += 18f
        canvas.drawText("Symptoms: ${record.symptoms.ifBlank { "None reported" }}", margin, y, textPaint)
        y += 18f
        if (record.clinicalHistory.isNotBlank()) {
            canvas.drawText("History: ${record.clinicalHistory}", margin, y, textPaint)
            y += 18f
        }

        // ECG Image
        y += 10f
        canvas.drawText("ECG IMAGE", margin, y, sectionPaint)
        y += 5f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 10f

        if (record.imagePath.isNotBlank()) {
            try {
                val ecgBitmap = BitmapFactory.decodeFile(record.imagePath)
                if (ecgBitmap != null) {
                    val imgWidth = pageWidth - 2 * margin.toInt()
                    val imgHeight = (ecgBitmap.height.toFloat() / ecgBitmap.width * imgWidth).toInt().coerceAtMost(200)
                    val dest = Rect(margin.toInt(), y.toInt(), (margin + imgWidth).toInt(), (y + imgHeight).toInt())
                    canvas.drawBitmap(ecgBitmap, null, dest, null)
                    y += imgHeight + 10f
                }
            } catch (_: Exception) { }
        }

        // Settings
        y += 10f
        canvas.drawText("ECG Settings: Layout=${record.ecgLayout}, Speed=${record.paperSpeed}mm/s, Gain=${record.voltageGain}mm/mV", margin, y, smallPaint)
        y += 25f

        // Diagnosis
        canvas.drawText("DIAGNOSIS", margin, y, sectionPaint)
        y += 5f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        canvas.drawText(record.diagnosis, margin, y, boldPaint)
        y += 18f
        if (record.confidence > 0) {
            canvas.drawText("Confidence: ${record.confidence}%", margin, y, textPaint)
            y += 18f
        }
        val urgencyText = when (record.urgencyLevel.lowercase()) {
            "emergent" -> "🔴 EMERGENT"
            "urgent" -> "🟡 URGENT"
            else -> "🟢 ROUTINE"
        }
        canvas.drawText("Urgency: $urgencyText", margin, y, boldPaint)
        y += 25f

        // ACS Risk
        if (record.acsRisk.isNotBlank() && record.acsRisk != "unknown") {
            canvas.drawText("ACS RISK ASSESSMENT", margin, y, sectionPaint)
            y += 5f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 18f
            canvas.drawText("ACS Risk: ${record.acsRisk.uppercase().replace("_", " ")}", margin, y, boldPaint)
            y += 25f
        }

        // Measurements
        canvas.drawText("MEASUREMENTS", margin, y, sectionPaint)
        y += 5f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        if (record.heartRate > 0) { canvas.drawText("Heart Rate: ${record.heartRate} bpm", margin, y, textPaint); y += 16f }
        if (record.rhythm.isNotBlank()) { canvas.drawText("Rhythm: ${record.rhythm}", margin, y, textPaint); y += 16f }
        if (record.axis.isNotBlank()) { canvas.drawText("Axis: ${record.axis}", margin, y, textPaint); y += 16f }
        if (record.prInterval.isNotBlank()) { canvas.drawText("PR Interval: ${record.prInterval}", margin, y, textPaint); y += 16f }
        if (record.qrsDuration.isNotBlank()) { canvas.drawText("QRS Duration: ${record.qrsDuration}", margin, y, textPaint); y += 16f }
        if (record.qtInterval.isNotBlank()) { canvas.drawText("QT/QTc: ${record.qtInterval}", margin, y, textPaint); y += 16f }

        // Disclaimer at bottom
        val disclaimerPaint = Paint().apply { color = Color.parseColor("#999999"); textSize = 8f; isAntiAlias = true }
        canvas.drawText("⚠️ AI-assisted interpretation only. Always verify with a qualified cardiologist. Generated by ECG Pro v2.0", margin, 820f, disclaimerPaint)
    }

    private fun drawPage2(canvas: Canvas, record: EcgRecord, pageWidth: Int, pageHeight: Int) {
        val margin = 40f

        // Red header
        val headerPaint = Paint().apply { color = Color.parseColor("#D32F2F"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, headerPaint)

        val titlePaint = Paint().apply { color = Color.WHITE; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText("Full Analysis — ${record.patientName}", margin, 40f, titlePaint)

        var y = 80f
        val textPaint = Paint().apply { color = Color.parseColor("#333333"); textSize = 10f; isAntiAlias = true }

        // Word-wrap the full analysis
        val lines = wrapText(record.fullAnalysis, textPaint, pageWidth - 2 * margin.toInt())
        for (line in lines) {
            if (y > pageHeight - 40f) break
            canvas.drawText(line, margin, y, textPaint)
            y += 14f
        }

        // Disclaimer
        val disclaimerPaint = Paint().apply { color = Color.parseColor("#999999"); textSize = 8f; isAntiAlias = true }
        canvas.drawText("⚠️ AI-assisted interpretation only. Always verify with a qualified cardiologist.", margin, (pageHeight - 20).toFloat(), disclaimerPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val result = mutableListOf<String>()
        val lines = text.split("\n")
        for (line in lines) {
            if (line.isBlank()) { result.add(""); continue }
            val words = line.split(" ")
            val current = StringBuilder()
            for (word in words) {
                val test = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(test) <= maxWidth) {
                    current.clear()
                    current.append(test)
                } else {
                    if (current.isNotEmpty()) result.add(current.toString())
                    current.clear()
                    current.append(word)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString())
        }
        return result
    }
}
