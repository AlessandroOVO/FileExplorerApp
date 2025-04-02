package com.ssandro.fileexplorerapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import java.io.File

class FileAdapter(context: Context, private val resource: Int, private val files: List<File>) :
    ArrayAdapter<File>(context, resource, files) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val file = files[position]
        val fileName = view.findViewById<TextView>(R.id.fileName)
        val fileIcon = view.findViewById<ImageView>(R.id.fileIcon)

        fileName.text = file.name

        if (file.isDirectory) {
            fileIcon.setImageResource(android.R.drawable.ic_menu_upload)
        } else {
            fileIcon.setImageResource(android.R.drawable.ic_menu_save)
        }

        return view
    }
}