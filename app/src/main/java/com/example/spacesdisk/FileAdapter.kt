package com.example.spacesdisk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(private val fileList: List<FileItem>) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val fileSize: TextView = itemView.findViewById(R.id.fileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item_layout, parent, false)
        return FileViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val currentItem = fileList[position]

        holder.fileName.text = currentItem.name
        holder.fileSize.text = "Size: ${currentItem.sizeMB} MB"
    }

    override fun getItemCount(): Int {
        return fileList.size
    }
}