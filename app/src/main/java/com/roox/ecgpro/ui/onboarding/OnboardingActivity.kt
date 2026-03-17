package com.roox.ecgpro.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.roox.ecgpro.R
import com.roox.ecgpro.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private data class OnboardingPage(val emoji: String, val title: String, val description: String)

    private val pages = listOf(
        OnboardingPage("📷", "Capture ECG", "Take a photo of any 12-lead ECG or upload from gallery.\nSupports all standard layouts."),
        OnboardingPage("🤖", "AI Analysis", "Instant interpretation with 38 diagnostic codes.\nACS risk assessment, LVEF estimation, and lead importance heatmap."),
        OnboardingPage("🌱", "Meet Root", "Your AI cardiologist assistant.\nPowered by the latest medical AI models.\nBuilt for Iraqi Board residents.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabIndicator)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)

        viewPager.adapter = OnboardingAdapter(pages)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnGetStarted.visibility = View.GONE

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == pages.size - 1) {
                    btnGetStarted.visibility = View.VISIBLE
                    btnSkip.visibility = View.GONE
                } else {
                    btnGetStarted.visibility = View.GONE
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnGetStarted.setOnClickListener { finishOnboarding() }
        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        getSharedPreferences("ecg_pro_prefs", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private class OnboardingAdapter(private val pages: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tvPageEmoji)
            val tvTitle: TextView = view.findViewById(R.id.tvPageTitle)
            val tvDesc: TextView = view.findViewById(R.id.tvPageDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_page, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages.get(position)
            holder.tvEmoji.text = page.emoji
            holder.tvTitle.text = page.title
            holder.tvDesc.text = page.description
        }

        override fun getItemCount() = pages.size
    }
}
