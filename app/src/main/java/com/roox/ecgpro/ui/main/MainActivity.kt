package com.roox.ecgpro.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roox.ecgpro.R
import com.roox.ecgpro.ui.analyze.AnalyzeActivity
import com.roox.ecgpro.ui.chat.ChatActivity
import com.roox.ecgpro.ui.result.ResultActivity
import com.roox.ecgpro.ui.settings.SettingsActivity
import com.roox.ecgpro.viewmodel.EcgViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var vm: EcgViewModel
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vm = ViewModelProvider(this).get(EcgViewModel::class.java)

        val rv = findViewById<RecyclerView>(R.id.rvRecords)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvCount = findViewById<TextView>(R.id.tvRecordCount)

        adapter = RecordAdapter { record ->
            startActivity(Intent(this, ResultActivity::class.java).putExtra("record_id", record.id))
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        vm.allRecords.observe(this) { records ->
            adapter.submitList(records)
            tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
            tvCount.text = "${records.size} records"
        }

        findViewById<View>(R.id.btnAnalyze).setOnClickListener {
            startActivity(Intent(this, AnalyzeActivity::class.java))
        }
        findViewById<View>(R.id.btnChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
