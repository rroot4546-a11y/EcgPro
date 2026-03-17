package com.roox.ecgpro.ui.settings

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roox.ecgpro.R
import com.roox.ecgpro.service.AiModels
import com.roox.ecgpro.service.AiService
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("ecg_pro_prefs", MODE_PRIVATE)
        val etKey = findViewById<EditText>(R.id.etApiKey)
        val spinModel = findViewById<Spinner>(R.id.spinModel)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Load saved
        etKey.setText(prefs.getString("api_key", ""))

        // Model spinner
        val modelNames = AiModels.list.map { "${it.name}${if (it.free) " ⭐FREE" else ""}" }
        spinModel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modelNames)
        val savedModel = prefs.getString("model", AiModels.default().id)
        val idx = AiModels.list.indexOfFirst { it.id == savedModel }
        if (idx >= 0) spinModel.setSelection(idx)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnTest.setOnClickListener {
            val key = etKey.text.toString().trim()
            if (key.isBlank()) { tvStatus.text = "⚠️ Enter an API key"; return@setOnClickListener }
            val model = AiModels.list[spinModel.selectedItemPosition].id
            tvStatus.text = "🔄 Testing..."
            btnTest.isEnabled = false
            lifecycleScope.launch {
                val ok = AiService(key, model).test()
                tvStatus.text = if (ok) "✅ Connection successful!" else "❌ Connection failed"
                btnTest.isEnabled = true
            }
        }

        btnSave.setOnClickListener {
            val key = etKey.text.toString().trim()
            val model = AiModels.list[spinModel.selectedItemPosition].id
            prefs.edit().putString("api_key", key).putString("model", model).apply()
            tvStatus.text = "✅ Settings saved!"
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}
