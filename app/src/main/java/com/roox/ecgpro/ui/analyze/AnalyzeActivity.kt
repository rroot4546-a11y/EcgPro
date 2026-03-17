package com.roox.ecgpro.ui.analyze

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.roox.ecgpro.R
import com.roox.ecgpro.ui.result.ResultActivity
import com.roox.ecgpro.viewmodel.EcgViewModel
import java.io.File

class AnalyzeActivity : AppCompatActivity() {
    private lateinit var vm: EcgViewModel
    private var imageUri: Uri? = null
    private var cameraPhotoFile: File? = null

    // ECG settings
    private var selectedLayout = "standard_3x2"
    private var selectedPaperSpeed = "25"
    private var selectedVoltageGain = "10"

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { setImage(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraPhotoFile?.let { setImage(Uri.fromFile(it)) }
    }

    private val reqPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze)

        vm = ViewModelProvider(this).get(EcgViewModel::class.java)

        val ivPreview = findViewById<ImageView>(R.id.ivPreview)
        val tvNoImage = findViewById<TextView>(R.id.tvNoImage)
        val etName = findViewById<EditText>(R.id.etPatientName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val etSymptoms = findViewById<EditText>(R.id.etSymptoms)
        val etHistory = findViewById<EditText>(R.id.etHistory)
        val btnAnalyze = findViewById<Button>(R.id.btnAnalyze)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // ECG Layout Spinner
        val spinnerLayout = findViewById<Spinner>(R.id.spinnerEcgLayout)
        val layoutOptions = arrayOf("Standard 3×2", "Standard 6×1", "Single Page", "Cabrera 6×1", "Cabrera 3×2")
        val layoutValues = arrayOf("standard_3x2", "standard_6x1", "single_page", "cabrera_6x1", "cabrera_3x2")
        spinnerLayout.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, layoutOptions)
        spinnerLayout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLayout = layoutValues.get(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Paper Speed toggle
        val rgPaperSpeed = findViewById<RadioGroup>(R.id.rgPaperSpeed)
        rgPaperSpeed.setOnCheckedChangeListener { _, checkedId ->
            selectedPaperSpeed = if (checkedId == R.id.rbSpeed50) "50" else "25"
        }

        // Voltage Gain toggle
        val rgVoltageGain = findViewById<RadioGroup>(R.id.rgVoltageGain)
        rgVoltageGain.setOnCheckedChangeListener { _, checkedId ->
            selectedVoltageGain = if (checkedId == R.id.rbGain5) "5" else "10"
        }

        // Camera & Gallery
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                reqPerm.launch(Manifest.permission.CAMERA)
            }
        }
        findViewById<Button>(R.id.btnGallery).setOnClickListener { pickImage.launch("image/*") }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnAnalyze.setOnClickListener {
            val uri = imageUri
            if (uri == null) { Toast.makeText(this, "Select an ECG image first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "Male"; R.id.rbFemale -> "Female"; else -> "Unknown"
            }
            val prefs = getSharedPreferences("ecg_pro_prefs", MODE_PRIVATE)
            vm.analyzeEcg(
                uri, etSymptoms.text.toString(), etAge.text.toString(), gender,
                etHistory.text.toString(), etName.text.toString(), prefs, contentResolver,
                selectedLayout, selectedPaperSpeed, selectedVoltageGain
            )
        }

        vm.isLoading.observe(this) { loading ->
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            tvStatus.visibility = if (loading) View.VISIBLE else View.GONE
            tvStatus.text = "🔍 Analyzing ECG with 38 diagnostic codes..."
            btnAnalyze.isEnabled = !loading
        }

        vm.analysisResult.observe(this) { record ->
            if (record != null) {
                startActivity(Intent(this, ResultActivity::class.java).putExtra("record_id", record.id))
                vm.clearResult()
            }
        }
    }

    private fun setImage(uri: Uri) {
        imageUri = uri
        findViewById<ImageView>(R.id.ivPreview).apply {
            setImageURI(uri)
            visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.tvNoImage).visibility = View.GONE
    }

    private fun launchCamera() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        cameraPhotoFile = File(dir, "ecg_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", cameraPhotoFile!!)
        takePhoto.launch(uri)
    }
}
