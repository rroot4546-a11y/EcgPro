package com.roox.ecgpro.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Custom view showing 12-lead importance as colored horizontal bars.
 * Importance levels: critical (red), high (orange), moderate (yellow), low (light green), normal (gray)
 */
class LeadImportanceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val leads = listOf("I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6")
    private var importanceMap: Map<String, String> = emptyMap()

    private val labelPaint = Paint().apply {
        color = Color.parseColor("#333333")
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val barPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bgBarPaint = Paint().apply {
        color = Color.parseColor("#F0F0F0")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val levelColors = mapOf(
        "critical" to Color.parseColor("#D32F2F"),
        "high" to Color.parseColor("#F57C00"),
        "moderate" to Color.parseColor("#FBC02D"),
        "low" to Color.parseColor("#81C784"),
        "normal" to Color.parseColor("#BDBDBD")
    )

    private val levelWidths = mapOf(
        "critical" to 1.0f,
        "high" to 0.8f,
        "moderate" to 0.6f,
        "low" to 0.4f,
        "normal" to 0.2f
    )

    fun setImportance(data: Map<String, String>) {
        importanceMap = data
        invalidate()
    }

    /** Parse JSON like {"I":"normal","II":"high",...} */
    fun setImportanceFromJson(json: String) {
        if (json.isBlank() || json == "{}") return
        try {
            val map = mutableMapOf<String, String>()
            val cleaned = json.trim().removeSurrounding("{", "}")
            val pairs = cleaned.split(",")
            for (pair in pairs) {
                val kv = pair.split(":")
                if (kv.size == 2) {
                    val key = kv.get(0).trim().removeSurrounding("\"")
                    val value = kv.get(1).trim().removeSurrounding("\"")
                    map.put(key, value)
                }
            }
            setImportance(map)
        } catch (_: Exception) { }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (leads.size * 44 + 20)  // 44px per lead + padding
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val labelWidth = 70f
        val barLeft = labelWidth + 16f
        val barMaxWidth = w - barLeft - 16f
        val barHeight = 28f
        val rowHeight = 44f

        for (i in leads.indices) {
            val lead = leads.get(i)
            val y = i * rowHeight + 10f
            val level = importanceMap.getOrDefault(lead, "normal")

            // Draw lead label
            canvas.drawText(lead, 8f, y + barHeight - 4f, labelPaint)

            // Draw background bar
            val rect = RectF(barLeft, y, barLeft + barMaxWidth, y + barHeight)
            canvas.drawRoundRect(rect, 6f, 6f, bgBarPaint)

            // Draw importance bar
            val barWidth = barMaxWidth * (levelWidths.getOrDefault(level, 0.2f))
            val barColor = levelColors.getOrDefault(level, Color.GRAY)
            barPaint.color = barColor
            val barRect = RectF(barLeft, y, barLeft + barWidth, y + barHeight)
            canvas.drawRoundRect(barRect, 6f, 6f, barPaint)
        }
    }
}
