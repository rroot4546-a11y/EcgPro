package com.roox.ecgpro.ui.chat

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roox.ecgpro.R
import com.roox.ecgpro.viewmodel.EcgViewModel

class ChatActivity : AppCompatActivity() {
    private lateinit var vm: EcgViewModel
    private lateinit var adapter: ChatAdapter
    private var attachedImage: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            attachedImage = it
            findViewById<ImageView>(R.id.ivAttached).apply {
                setImageURI(it)
                visibility = View.VISIBLE
            }
            findViewById<ImageButton>(R.id.btnClearAttach).visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        vm = ViewModelProvider(this).get(EcgViewModel::class.java)

        val rv = findViewById<RecyclerView>(R.id.rvChat)
        val etMsg = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)
        val progress = findViewById<ProgressBar>(R.id.progress)

        adapter = ChatAdapter()
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnClearAttach).setOnClickListener { clearAttach() }
        btnAttach.setOnClickListener { pickImage.launch("image/*") }

        btnSend.setOnClickListener {
            val msg = etMsg.text.toString().trim()
            if (msg.isBlank() && attachedImage == null) return@setOnClickListener

            val prefs = getSharedPreferences("ecg_pro_prefs", MODE_PRIVATE)
            vm.sendChat(msg.ifBlank { "Analyze this ECG" }, attachedImage, prefs, contentResolver)
            etMsg.text.clear()
            clearAttach()
        }

        vm.allChats.observe(this) { chats ->
            adapter.submitList(chats) {
                if (chats.isNotEmpty()) rv.smoothScrollToPosition(chats.size - 1)
            }
        }

        vm.isLoading.observe(this) { loading ->
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            btnSend.isEnabled = !loading
        }
    }

    private fun clearAttach() {
        attachedImage = null
        findViewById<ImageView>(R.id.ivAttached).visibility = View.GONE
        findViewById<ImageButton>(R.id.btnClearAttach).visibility = View.GONE
    }
}
