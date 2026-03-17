package com.roox.ecgpro.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class EcgGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var ecgData: FloatArray = floatArrayOf()
    private var label: String = ""

    private val bgPaint = Paint().apply { color = Color.parseColor("#FFF0F0"); style = Paint.Style.FILL }
    private val smallGridPaint = Paint().apply { color = Color.parseColor("#F5C6C6"); style = Paint.Style.STROKE; strokeWidth = 1f }
    private val largeGridPaint = Paint().apply { color = Color.parseColor("#E09090"); style = Paint.Style.STROKE; strokeWidth = 1.5f }
    private val waveformPaint = Paint().apply { color = Color.parseColor("#1A1A1A"); style = Paint.Style.STROKE; strokeWidth = 2.5f; isAntiAlias = true; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val labelPaint = Paint().apply { color = Color.parseColor("#D32F2F"); textSize = 36f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }

    fun setEcgData(data: FloatArray, rhythmLabel: String = "") {
        ecgData = data
        label = rhythmLabel
        invalidate()
    }

    fun setEcgDataFromList(data: List<Float>, rhythmLabel: String = "") {
        setEcgData(data.toFloatArray(), rhythmLabel)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (w * 0.4f).toInt().coerceAtLeast(300)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Small grid (1mm equivalent ~ 10px)
        val smallBox = 10f
        var x = 0f
        while (x <= w) { canvas.drawLine(x, 0f, x, h, smallGridPaint); x += smallBox }
        var y = 0f
        while (y <= h) { canvas.drawLine(0f, y, w, y, smallGridPaint); y += smallBox }

        // Large grid (5mm equivalent ~ 50px)
        val largeBox = smallBox * 5f
        x = 0f
        while (x <= w) { canvas.drawLine(x, 0f, x, h, largeGridPaint); x += largeBox }
        y = 0f
        while (y <= h) { canvas.drawLine(0f, y, w, y, largeGridPaint); y += largeBox }

        // Draw waveform
        if (ecgData.isNotEmpty()) {
            val midY = h / 2f
            val ampScale = h * 0.35f
            val path = Path()
            val step = w / ecgData.size.toFloat()
            for (i in ecgData.indices) {
                val px = i * step
                val py = midY - ecgData[i] * ampScale
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, waveformPaint)
        }

        // Label
        if (label.isNotBlank()) {
            canvas.drawText(label, 16f, 40f, labelPaint)
        }
    }

    companion object {
        fun generateNormalSinus(heartRate: Int = 75, samples: Int = 1000): FloatArray {
            val data = FloatArray(samples)
            val beatsPerSample = heartRate / 60.0 / (samples / 2.5)
            for (i in 0 until samples) {
                val t = i * beatsPerSample * 2.0 * PI
                val cycle = t % (2.0 * PI)
                data[i] = when {
                    cycle < 0.5 -> (0.15 * sin(cycle * PI / 0.5)).toFloat()                    // P wave
                    cycle < 0.7 -> 0f                                                            // PR segment
                    cycle < 0.75 -> (-0.1 * sin((cycle - 0.7) * PI / 0.05)).toFloat()           // Q
                    cycle < 0.95 -> (0.9 * sin((cycle - 0.75) * PI / 0.2)).toFloat()            // R
                    cycle < 1.0 -> (-0.15 * sin((cycle - 0.95) * PI / 0.05)).toFloat()          // S
                    cycle < 1.4 -> 0f                                                            // ST segment
                    cycle < 1.8 -> (0.2 * sin((cycle - 1.4) * PI / 0.4)).toFloat()             // T wave
                    else -> 0f                                                                   // baseline
                }
            }
            return data
        }

        fun generateAfib(samples: Int = 1000): FloatArray {
            val data = FloatArray(samples)
            val random = java.util.Random(42)
            var i = 0
            while (i < samples) {
                val rrInterval = (80 + random.nextInt(120)).coerceAtMost(samples - i)
                // Fibrillatory baseline
                for (j in 0 until rrInterval.coerceAtMost(samples - i)) {
                    data[i + j] = (random.nextGaussian() * 0.03).toFloat()
                }
                // QRS
                val qrsStart = (rrInterval * 0.3).toInt()
                if (i + qrsStart + 20 < samples) {
                    for (j in 0 until 20) {
                        val frac = j / 20.0
                        data[i + qrsStart + j] += when {
                            frac < 0.2 -> (-0.1 * sin(frac * PI / 0.2)).toFloat()
                            frac < 0.5 -> (0.85 * sin((frac - 0.2) * PI / 0.3)).toFloat()
                            frac < 0.7 -> (-0.12 * sin((frac - 0.5) * PI / 0.2)).toFloat()
                            else -> 0f
                        }
                    }
                    // T wave
                    val tStart = qrsStart + 30
                    if (i + tStart + 25 < samples) {
                        for (j in 0 until 25) {
                            data[i + tStart + j] += (0.18 * sin(j * PI / 25.0)).toFloat()
                        }
                    }
                }
                i += rrInterval
            }
            return data
        }

        fun generateStemi(samples: Int = 1000): FloatArray {
            val base = generateNormalSinus(80, samples)
            for (i in base.indices) {
                val t = i.toDouble() / samples * 25.0
                val cycle = t % (2.0 * PI)
                if (cycle > 1.0 && cycle < 1.8) {
                    base[i] += 0.25f   // ST elevation
                }
            }
            return base
        }

        fun generateVtach(samples: Int = 1000): FloatArray {
            val data = FloatArray(samples)
            val period = 30
            for (i in 0 until samples) {
                val phase = (i % period).toDouble() / period
                data[i] = (0.7 * sin(phase * 2 * PI) + 0.15 * sin(phase * 4 * PI)).toFloat()
            }
            return data
        }

        fun generateVfib(samples: Int = 1000): FloatArray {
            val data = FloatArray(samples)
            val random = java.util.Random(99)
            for (i in 0 until samples) {
                data[i] = ((random.nextGaussian() * 0.4) + 0.2 * sin(i * 0.3) + 0.15 * sin(i * 0.7)).toFloat()
            }
            return data
        }

        fun generateAflutter(samples: Int = 1000): FloatArray {
            val data = FloatArray(samples)
            val sawPeriod = 40
            for (i in 0 until samples) {
                val sawPhase = (i % sawPeriod).toDouble() / sawPeriod
                val flutter = -0.2 * (1.0 - sawPhase) + 0.05
                data[i] = flutter.toFloat()
                // QRS every 4th flutter
                if (i % (sawPeriod * 4) in 0..15) {
                    val qrsPhase = (i % (sawPeriod * 4)).toDouble() / 15.0
                    if (qrsPhase < 0.3) data[i] += (-0.1 * sin(qrsPhase * PI / 0.3)).toFloat()
                    else if (qrsPhase < 0.7) data[i] += (0.8 * sin((qrsPhase - 0.3) * PI / 0.4)).toFloat()
                    else data[i] += (-0.1 * sin((qrsPhase - 0.7) * PI / 0.3)).toFloat()
                }
            }
            return data
        }
    }
}
