package com.ssandro.fileexplorerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var filesListView: ListView
    private lateinit var currentPathTextView: TextView
    private var currentPath: File = Environment.getExternalStorageDirectory()
    private var fileList: List<File> = emptyList()

    // Contracts para manejo de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            listFiles()
        } else {
            showPermissionExplanation()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()) {
            listFiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filesListView = findViewById(R.id.filesListView)
        currentPathTextView = findViewById(R.id.currentPathTextView)

        filesListView.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = fileList[position]
            if (selectedFile.isDirectory) {
                currentPath = selectedFile
                listFiles()
            } else {
                Toast.makeText(this, "Archivo seleccionado: ${selectedFile.name}", Toast.LENGTH_SHORT).show()
            }
        }

        checkStoragePermissions()
    }

    private fun listFiles() {
        currentPathTextView.text = "Ruta: ${currentPath.absolutePath}"

        fileList = currentPath.listFiles()?.toList() ?: emptyList()

        val directories = fileList
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }

        val files = fileList
            .filter { !it.isDirectory }
            .sortedBy { it.name.lowercase() }

        val allItems = directories + files

        filesListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            allItems.map { it.name }
        )
    }

    private fun checkStoragePermissions() {
        when {
            hasRequiredPermissions() -> {
                listFiles()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                requestManageStoragePermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Método showPermissionExplanation() implementado
    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Esta aplicación necesita acceso al almacenamiento para mostrar tus archivos. Por favor, concede los permisos requeridos.")
            .setPositiveButton("Aceptar") { _, _ ->
                checkStoragePermissions()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "La funcionalidad estará limitada sin permisos", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent().apply {
                    action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    listFiles()
                } else {
                    showPermissionExplanation()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (currentPath != Environment.getExternalStorageDirectory() &&
            currentPath.parentFile != null) {
            currentPath = currentPath.parentFile!!
            listFiles()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }
}