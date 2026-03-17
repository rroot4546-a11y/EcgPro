package com.roox.ecgpro.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LeadImportanceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class LeadScore(val name: String, val level: String)

    private var leads: List<LeadScore> = emptyList()
    private val barPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val textPaint = Paint().apply { isAntiAlias = true; color = Color.DKGRAY; textSize = 28f; textAlign = Paint.Align.CENTER }
    private val labelPaint = Paint().apply { isAntiAlias = true; color = Color.DKGRAY; textSize = 22f; textAlign = Paint.Align.CENTER }

    fun setLeadImportance(data: List<LeadScore>) { leads = data; invalidate() }

    fun parseFromText(text: String) {
        val leadNames = listOf("I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6")
        val result = mutableListOf<LeadScore>()
        for (name in leadNames) {
            val pattern = Regex("""${Regex.escape(name)}\s*\(\s*(critical|high|moderate|low|normal)\s*\)""", RegexOption.IGNORE_CASE)
            val match = pattern.find(text)
            result.add(LeadScore(name, match?.groupValues?.get(1)?.lowercase() ?: "normal"))
        }
        setLeadImportance(result)
    }

    private fun levelToColor(level: String): Int = when (level) {
        "critical" -> Color.parseColor("#D32F2F")
        "high" -> Color.parseColor("#FF5722")
        "moderate" -> Color.parseColor("#FF9800")
        "low" -> Color.parseColor("#4CAF50")
        else -> Color.parseColor("#E0E0E0")
    }

    private fun levelToHeight(level: String): Float = when (level) {
        "critical" -> 1.0f; "high" -> 0.8f; "moderate" -> 0.6f; "low" -> 0.35f; else -> 0.15f
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        setMeasuredDimension(width, 200)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (leads.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val barW = (w - 40f) / leads.size
        val maxBarH = h - 60f

        for ((i, lead) in leads.withIndex()) {
            val x = 20f + i * barW
            val barH = maxBarH * levelToHeight(lead.level)
            barPaint.color = levelToColor(lead.level)

            val rect = RectF(x + 4f, h - 30f - barH, x + barW - 4f, h - 30f)
            canvas.drawRoundRect(rect, 4f, 4f, barPaint)

            labelPaint.textSize = if (leads.size > 8) 18f else 22f
            canvas.drawText(lead.name, x + barW / 2f, h - 8f, labelPaint)
        }
    }

    companion object {
        val DEFAULT_LEADS = listOf("I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6")
            .map { LeadScore(it, "normal") }
    }
}
