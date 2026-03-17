package com.roox.ecgpro.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.roox.ecgpro.R
import com.roox.ecgpro.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {
    private var currentPage = 0
    private val pages = listOf(
        Triple("📷", "Capture ECG", "Take a photo of any 12-lead ECG or upload from gallery.\nSupports SinglePage, 6x1, 3x2, and Cabrera layouts."),
        Triple("🤖", "AI Analysis", "38 core diagnoses • ACS/OMI scoring • LVEF estimation\nCardiac axis • Lead importance heatmap\n6 medical AI models to choose from"),
        Triple("🌱", "Meet Root", "Your AI cardiologist assistant.\nAsk any ECG or cardiology question.\nSupports Arabic and English.\nPowered by Harrison's, Braunwald's & ACC/AHA guidelines.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val tvEmoji = findViewById<TextView>(R.id.tvEmoji)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDesc = findViewById<TextView>(R.id.tvDesc)
        val tvCounter = findViewById<TextView>(R.id.tvCounter)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnSkip = findViewById<Button>(R.id.btnSkip)

        fun showPage() {
            val (emoji, title, desc) = pages.get(currentPage)
            tvEmoji.text = emoji
            tvTitle.text = title
            tvDesc.text = desc
            tvCounter.text = "${currentPage + 1} / ${pages.size}"
            btnNext.text = if (currentPage == pages.size - 1) "Get Started 🚀" else "Next →"
        }

        showPage()

        btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) { currentPage++; showPage() }
            else finishOnboarding()
        }
        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        getSharedPreferences("ecg_pro_prefs", MODE_PRIVATE).edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
