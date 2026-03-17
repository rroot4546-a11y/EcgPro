package com.roox.ecgpro.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.roox.ecgpro.R
import com.roox.ecgpro.data.model.EcgRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(private val onClick: (EcgRecord) -> Unit) :
    ListAdapter<EcgRecord, RecordAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EcgRecord>() {
            override fun areItemsTheSame(a: EcgRecord, b: EcgRecord) = a.id == b.id
            override fun areContentsTheSame(a: EcgRecord, b: EcgRecord) = a == b
        }
        private val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.ivThumb)
        val tvDiag: TextView = v.findViewById(R.id.tvDiagnosis)
        val tvPatient: TextView = v.findViewById(R.id.tvPatient)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvUrgency: TextView = v.findViewById(R.id.tvUrgency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = getItem(pos)
        h.tvDiag.text = r.diagnosis.ifBlank { "Pending..." }
        h.tvPatient.text = buildString {
            if (r.patientName.isNotBlank()) append(r.patientName)
            if (r.patientAge.isNotBlank()) append(", ${r.patientAge}y")
            if (r.patientGender.isNotBlank()) append(", ${r.patientGender}")
            if (isEmpty()) append("Unknown patient")
        }
        h.tvDate.text = sdf.format(Date(r.timestamp))
        h.tvUrgency.text = when (r.urgencyLevel.lowercase()) {
            "emergent" -> "🔴 EMERGENT"
            "urgent" -> "🟡 URGENT"
            else -> "🟢 Routine"
        }
        if (r.imagePath.isNotBlank()) {
            h.img.load(File(r.imagePath)) { crossfade(true) }
        }
        h.itemView.setOnClickListener { onClick(r) }
    }
}
