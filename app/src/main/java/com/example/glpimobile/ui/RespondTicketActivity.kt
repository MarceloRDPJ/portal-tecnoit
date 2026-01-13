package com.example.glpimobile.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.glpimobile.R
import com.example.glpimobile.db.AppDatabase
import com.example.glpimobile.db.OfflineAction
import com.example.glpimobile.model.Solution
import com.example.glpimobile.workers.SyncWorker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RespondTicketActivity : AppCompatActivity() {

    private var ticketId: Int = 0
    private lateinit var etSolution: TextInputEditText
    private lateinit var tilFiberLength: TextInputLayout
    private lateinit var etFiberLength: TextInputEditText
    private lateinit var btnSubmit: Button
    private lateinit var llConsumablesList: LinearLayout
    private lateinit var btnAddConsumable: Button
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var llEvidenceList: LinearLayout

    private val cameraLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Save bitmap to file and queue
            saveImageEvidence(bitmap)
        }
    }

    private val galleryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Copy URI content to local file
            val path = copyUriToCache(uri)
            if (path != null) {
                queueEvidenceAction(path)
                addEvidenceToView("Gallery Image")
            } else {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hardcoded Fiber Optic terms
    private val fiberTerms = listOf("troca de fibra", "rompimento de fibra", "emenda de fibra")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_respond_ticket)

        ticketId = intent.getIntExtra("TICKET_ID", 0)
        if (ticketId == 0) {
            finish()
            return
        }

        etSolution = findViewById(R.id.etSolution)
        tilFiberLength = findViewById(R.id.tilFiberLength)
        etFiberLength = findViewById(R.id.etFiberLength)
        btnSubmit = findViewById(R.id.btnSubmitResponse)
        llConsumablesList = findViewById(R.id.llConsumablesList)
        btnAddConsumable = findViewById(R.id.btnAddConsumable)
        btnCamera = findViewById(R.id.btnCapturePhoto)
        btnGallery = findViewById(R.id.btnGallery)
        llEvidenceList = findViewById(R.id.llEvidenceList)

        setupFiberLogic()
        setupSubmit()
        setupConsumables()
        setupEvidence()
    }

    private fun setupFiberLogic() {
        etSolution.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString().lowercase(Locale.getDefault())
                val isFiberRelated = fiberTerms.any { text.contains(it) }
                tilFiberLength.visibility = if (isFiberRelated) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSubmit() {
        btnSubmit.setOnClickListener {
            val solutionText = etSolution.text.toString()
            if (solutionText.isBlank()) {
                etSolution.error = "Solution cannot be empty"
                return@setOnClickListener
            }

            if (tilFiberLength.visibility == View.VISIBLE) {
                val fiberLength = etFiberLength.text.toString()
                if (fiberLength.isBlank()) {
                    etFiberLength.error = "Fiber length is required for this operation"
                    return@setOnClickListener
                }
                // Append fiber info to solution text or handle separately
                // For now, appending to text as per standard practice unless a specific field exists
                // The prompt says "Se a resolução contiver... O sistema deverá exigir: Quantidade de fibra".
                // It doesn't specify where to save it. Appending to description is safest.
                // solutionText += "\n\nFiber Used: ${fiberLength}m" (Variable needs to be mutable)
            }

            val finalSolutionText = if (tilFiberLength.visibility == View.VISIBLE) {
                "$solutionText\n\nFiber Used: ${etFiberLength.text}m"
            } else {
                solutionText
            }

            saveSolution(finalSolutionText)
        }
    }

    private fun saveSolution(content: String) {
        lifecycleScope.launch {
            val solution = Solution(
                ticketId = ticketId,
                content = content
            )
            val jsonPayload = Gson().toJson(solution)

            val action = OfflineAction(
                type = "SOLUTION",
                ticketId = ticketId,
                payloadJson = jsonPayload
            )

            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@RespondTicketActivity).offlineActionDao().insert(action)
            }

            // Trigger Sync
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this@RespondTicketActivity).enqueue(syncRequest)

            Toast.makeText(this@RespondTicketActivity, "Solution queued for sync", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupConsumables() {
        btnAddConsumable.setOnClickListener {
            ConsumableDialog(this) { consumable, qty ->
                addConsumableToView(consumable, qty)
            }.show()
        }
    }

    private fun addConsumableToView(consumable: com.example.glpimobile.model.Consumable, qty: Int) {
        val textView = android.widget.TextView(this)
        textView.text = "${consumable.name} (x$qty)"
        llConsumablesList.addView(textView)

        // Also save this action to DB immediately or store in a list to save on Submit
        // For simplicity, let's just toast. In real app, we queue "ADD_CONSUMABLE" action.
        queueConsumableAction(consumable, qty)
    }

    private fun queueConsumableAction(consumable: com.example.glpimobile.model.Consumable, qty: Int) {
         lifecycleScope.launch {
            // Mock payload
            val payload = mapOf("item_id" to consumable.id, "name" to consumable.name, "qty" to qty)
            val jsonPayload = Gson().toJson(payload)

            val action = OfflineAction(
                type = "CONSUMABLE",
                ticketId = ticketId,
                payloadJson = jsonPayload
            )

            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@RespondTicketActivity).offlineActionDao().insert(action)
            }
        }
    }

    private fun setupEvidence() {
        btnCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }
        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun saveImageEvidence(bitmap: android.graphics.Bitmap) {
        // Save bitmap to a file in cache
        try {
            val filename = "evidence_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(cacheDir, filename)
            val out = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out) // Compression
            out.flush()
            out.close()

            queueEvidenceAction(file.absolutePath)
            addEvidenceToView("Camera Photo")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyUriToCache(uri: android.net.Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val filename = "gallery_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(cacheDir, filename)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun queueEvidenceAction(path: String) {
        lifecycleScope.launch {
            val payload = mapOf("path" to path)
            val jsonPayload = Gson().toJson(payload)

            val action = OfflineAction(
                type = "DOCUMENT",
                ticketId = ticketId,
                payloadJson = jsonPayload
            )

            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@RespondTicketActivity).offlineActionDao().insert(action)
            }
        }
    }

    private fun addEvidenceToView(label: String) {
        val textView = android.widget.TextView(this)
        textView.text = label
        llEvidenceList.addView(textView)
    }
}
