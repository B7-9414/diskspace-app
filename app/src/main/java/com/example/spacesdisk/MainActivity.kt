package com.example.spacesdisk

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_REQUEST_CODE = 1001
    private val FILL_SPACE_FOLDER_NAME = "BassamFiles5"

    private lateinit var etSpaceAmount: EditText
    private lateinit var tvSpaceInfo: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var noDataTextView: TextView
    private lateinit var btnReleaseSpace: Button
    private lateinit var progressDialog: ProgressDialog

    private var fileList = mutableListOf<FileItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etSpaceAmount = findViewById(R.id.etSpaceAmount)
        tvSpaceInfo = findViewById(R.id.tvSpaceInfo)
        recyclerView = findViewById(R.id.recyclerView)
        noDataTextView = findViewById(R.id.no_data_tv)
        btnReleaseSpace = findViewById(R.id.btnReleaseSpace)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Working...")
        progressDialog.setCancelable(false)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FileAdapter(fileList)

        requestStoragePermissions()
        updateRecyclerView()

        findViewById<Button>(R.id.btnFullSpace).setOnClickListener { fillSpace() }
        btnReleaseSpace.setOnClickListener { releaseSpace() }
    }

    // I have test it in real device its req a Permissions. Emulator do not have to give a Permissions
    private fun requestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )

        }

        updateSpaceInfo();
    }

    private fun updateSpaceInfo() {
        val storageDirectory = Environment.getExternalStorageDirectory()
        val totalSpace = storageDirectory.totalSpace / (1024 * 1024) // in MB
        val freeSpace = storageDirectory.freeSpace / (1024 * 1024) // in MB
        val usedSpace = totalSpace - freeSpace

        if(freeSpace>totalSpace){
            Toast.makeText(this,"Memory Full",Toast.LENGTH_SHORT).show()
        }

        val spaceInfoText = "Used Space: $usedSpace MB\nRemaining Space: $freeSpace MB"
        tvSpaceInfo.text = spaceInfoText
    }

    private fun fillSpace() {
        val spaceAmountText = etSpaceAmount.text.toString().trim()

        if (spaceAmountText.isEmpty()) {
            etSpaceAmount.error = "Enter space amount"
            return
        }

        val spaceAmount = spaceAmountText.toInt()
        val storageDirectory = Environment.getExternalStorageDirectory()
        val freeSpace = storageDirectory.freeSpace / (1024 * 1024) // in MB
        if(spaceAmount>freeSpace){
            etSpaceAmount.error = "Please put less amount"
            return
        }
        progressDialog.show()

        Thread {
            val fillSpaceFolder = File(filesDir, FILL_SPACE_FOLDER_NAME)

            try {
                if (!fillSpaceFolder.exists()) {
                    if (fillSpaceFolder.mkdirs()) {
                        Log.d("TAG", "Directory created successfully")
                    } else {
                        Log.d("TAG", "Failed to create directory")
                    }
                }

                val internalStorageDir = File(filesDir, FILL_SPACE_FOLDER_NAME)
                val filesInDir = internalStorageDir.listFiles()

                if (filesInDir != null) {
                    for (file in filesInDir) {
                        Log.d("TAG", "File found: ${file.absolutePath}")
                    }
                } else {
                    Log.d("TAG", "No files found in internal storage directory")
                }


                if (fillSpaceFolder.exists()) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "file_$timeStamp.txt"

                    val file = File(fillSpaceFolder, fileName)
                    val fileOutputStream = FileOutputStream(file)
                    val buffer = ByteArray(1024)
                    for (i in 0 until spaceAmount * 1024) {
                        fileOutputStream.write(buffer)
                    }
                    fileOutputStream.close()

                    runOnUiThread {
                        updateSpaceInfo()
                        progressDialog.dismiss()
                        updateRecyclerView()
                        etSpaceAmount.text.clear()
                        Log.d("TAG", "fillSpace: " + file)
                    }
                } else {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Log.d("TAG", "fillSpace: Failed to create directory")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("TAG", "fillSpace: " + e.message)
                runOnUiThread {
                    progressDialog.dismiss()
                }
            }
        }.start()
    }

    // Here I am clear the space and clear the chache.
    private fun releaseSpace() {
        progressDialog.show()

        Thread {
            val fillSpaceFolder = File(filesDir, FILL_SPACE_FOLDER_NAME)

            try {
                fillSpaceFolder.deleteRecursively()
                //clearing the chache
                clearCache()

                // Update space information
                runOnUiThread {
                    updateSpaceInfo()
                    progressDialog.dismiss()
                    updateRecyclerView()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    progressDialog.dismiss()
                }
            }
        }.start()
    }

    // I do not need to view it, just for fun :))
    private fun updateRecyclerView() {
        fileList.clear()

        val fillSpaceFolder = File(filesDir, FILL_SPACE_FOLDER_NAME)

        val files = fillSpaceFolder.listFiles()

        if (files != null && files.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
            noDataTextView.visibility = View.GONE

            for (file in files) {
                val sizeMB = (file.length() / (1024 * 1024)).toInt()
                fileList.add(FileItem(file.name, sizeMB))
            }

            recyclerView.adapter?.notifyDataSetChanged()
        } else {
            recyclerView.visibility = View.GONE
            noDataTextView.visibility = View.VISIBLE
            Log.d("TAG", "updateRecyclerView:No Files found " )
        }
    }
    // Just for testing purpose
    private fun clearCache() {
        try {
            val cacheDir = cacheDir
            if (cacheDir.isDirectory) {
                val files = cacheDir.listFiles()
                for (file in files) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}