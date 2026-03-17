package com.roox.ecgpro.ui.training

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.roox.ecgpro.R
import com.roox.ecgpro.data.model.TrainingRecord
import com.roox.ecgpro.viewmodel.EcgViewModel
import java.text.SimpleDateFormat
import java.util.*

class TrainingActivity : AppCompatActivity() {
    private lateinit var vm: EcgViewModel
    private var selectedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri = it; findViewById<ImageView>(R.id.ivTrainPreview).apply { setImageURI(it); visibility = View.VISIBLE } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        vm = ViewModelProvider(this).get(EcgViewModel::class.java)

        val etDiagnosis = findViewById<EditText>(R.id.etDiagnosis)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val rv = findViewById<RecyclerView>(R.id.rvTraining)

        val adapter = TrainingAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnSaveTraining).setOnClickListener {
            val uri = selectedUri
            if (uri == null || etDiagnosis.text.isBlank()) {
                Toast.makeText(this, "Select image and enter diagnosis", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            vm.addTraining(uri, etDiagnosis.text.toString(), etNotes.text.toString(), contentResolver)
            etDiagnosis.text.clear(); etNotes.text.clear()
            selectedUri = null; findViewById<ImageView>(R.id.ivTrainPreview).visibility = View.GONE
            Toast.makeText(this, "Training record saved!", Toast.LENGTH_SHORT).show()
        }

        vm.allTraining.observe(this) { adapter.submitList(it) }
    }

    class TrainingAdapter : ListAdapter<TrainingRecord, TrainingAdapter.VH>(object : DiffUtil.ItemCallback<TrainingRecord>() {
        override fun areItemsTheSame(a: TrainingRecord, b: TrainingRecord) = a.id == b.id
        override fun areContentsTheSame(a: TrainingRecord, b: TrainingRecord) = a == b
    }) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvDiag: TextView = v.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        override fun onBindViewHolder(h: VH, pos: Int) {
            val t = getItem(pos)
            h.tvDiag.text = "${t.knownDiagnosis} — ${sdf.format(Date(t.timestamp))}"
        }
    }
}
