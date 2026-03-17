package com.roox.ecgpro.ui.training

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roox.ecgpro.R
import com.roox.ecgpro.data.model.TrainingRecord
import com.roox.ecgpro.viewmodel.EcgViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingActivity : AppCompatActivity() {
    private lateinit var vm: EcgViewModel
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            findViewById<ImageView>(R.id.ivTrainingPreview).apply {
                setImageURI(it)
                visibility = View.VISIBLE
            }
            findViewById<TextView>(R.id.tvTrainingNoImage).visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        vm = ViewModelProvider(this).get(EcgViewModel::class.java)

        val etDiagnosis = findViewById<EditText>(R.id.etKnownDiagnosis)
        val etNotes = findViewById<EditText>(R.id.etTrainingNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveTraining)
        val rv = findViewById<RecyclerView>(R.id.rvTrainingRecords)
        val tvEmpty = findViewById<TextView>(R.id.tvTrainingEmpty)
        val tvCount = findViewById<TextView>(R.id.tvTrainingCount)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnPickTrainingImage).setOnClickListener { pickImage.launch("image/*") }

        val adapter = TrainingAdapter { record -> vm.deleteTraining(record) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnSave.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null) {
                Toast.makeText(this, "Select an ECG image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val diagnosis = etDiagnosis.text.toString().trim()
            if (diagnosis.isBlank()) {
                Toast.makeText(this, "Enter the known diagnosis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagePath = vm.saveImage(uri, contentResolver)
            val record = TrainingRecord(
                imagePath = imagePath,
                knownDiagnosis = diagnosis,
                notes = etNotes.text.toString().trim()
            )
            vm.insertTraining(record)

            // Reset form
            selectedImageUri = null
            findViewById<ImageView>(R.id.ivTrainingPreview).visibility = View.GONE
            findViewById<TextView>(R.id.tvTrainingNoImage).visibility = View.VISIBLE
            etDiagnosis.setText("")
            etNotes.setText("")
            Toast.makeText(this, "✅ Training case saved!", Toast.LENGTH_SHORT).show()
        }

        vm.allTraining.observe(this) { records ->
            adapter.submitList(records)
            tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
            tvCount.text = "${records.size} training cases"
        }
    }

    private class TrainingAdapter(private val onDelete: (TrainingRecord) -> Unit) :
        RecyclerView.Adapter<TrainingAdapter.VH>() {

        private var items: List<TrainingRecord> = emptyList()

        fun submitList(list: List<TrainingRecord>) {
            items = list
            notifyDataSetChanged()
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvDiagnosis: TextView = view.findViewById(android.R.id.text1)
            val tvInfo: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val record = items.get(position)
            holder.tvDiagnosis.text = "📋 ${record.knownDiagnosis}"
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(record.timestamp))
            holder.tvInfo.text = "$dateStr${if (record.notes.isNotBlank()) " — ${record.notes}" else ""}"
            holder.itemView.setOnLongClickListener {
                onDelete(record)
                true
            }
        }

        override fun getItemCount() = items.size
    }
}
