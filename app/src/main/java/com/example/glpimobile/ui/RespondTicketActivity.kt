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

    // New Fields
    private lateinit var rgResponseType: android.widget.RadioGroup
    private lateinit var llTechnicalData: LinearLayout
    private lateinit var etPonto: TextInputEditText
    private lateinit var etDrop: TextInputEditText
    private lateinit var etAlcas: TextInputEditText
    private lateinit var etEsticador: TextInputEditText
    private lateinit var etConector: TextInputEditText
    private lateinit var etMetragemInicial: TextInputEditText
    private lateinit var etMetragemFinal: TextInputEditText
    private lateinit var tvMetragemGasta: android.widget.TextView
    private lateinit var etExtras: TextInputEditText

    private var hasEvidence = false

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
        tilFiberLength = findViewById(R.id.tilFiberLength) // Kept for backward compat/legacy
        etFiberLength = findViewById(R.id.etFiberLength)
        btnSubmit = findViewById(R.id.btnSubmitResponse)
        llConsumablesList = findViewById(R.id.llConsumablesList)
        btnAddConsumable = findViewById(R.id.btnAddConsumable)
        btnCamera = findViewById(R.id.btnCapturePhoto)
        btnGallery = findViewById(R.id.btnGallery)
        llEvidenceList = findViewById(R.id.llEvidenceList)

        // Bind New Views
        rgResponseType = findViewById(R.id.rgResponseType)
        llTechnicalData = findViewById(R.id.llTechnicalData)
        etPonto = findViewById(R.id.etPonto)
        etDrop = findViewById(R.id.etDrop)
        etAlcas = findViewById(R.id.etAlcas)
        etEsticador = findViewById(R.id.etEsticador)
        etConector = findViewById(R.id.etConector)
        etMetragemInicial = findViewById(R.id.etMetragemInicial)
        etMetragemFinal = findViewById(R.id.etMetragemFinal)
        tvMetragemGasta = findViewById(R.id.tvMetragemGasta)
        etExtras = findViewById(R.id.etExtras)

        setupResponseType()
        setupFiberLogic()
        setupSubmit()
        setupConsumables()
        setupEvidence()
        setupMetragemCalculation()
    }

    private fun setupResponseType() {
        rgResponseType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSolution) {
                llTechnicalData.visibility = View.VISIBLE
                btnSubmit.text = "FINALIZAR CHAMADO"
            } else {
                llTechnicalData.visibility = View.GONE
                btnSubmit.text = "ENVIAR RESPOSTA"
            }
        }
    }

    private fun setupMetragemCalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateMetragem()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etMetragemInicial.addTextChangedListener(watcher)
        etMetragemFinal.addTextChangedListener(watcher)
    }

    private fun calculateMetragem() {
        val initial = etMetragemInicial.text.toString().toDoubleOrNull() ?: 0.0
        val final = etMetragemFinal.text.toString().toDoubleOrNull() ?: 0.0
        val spent = (final - initial).coerceAtLeast(0.0)
        tvMetragemGasta.text = "Gasto: ${spent}m"
    }

    private fun setupFiberLogic() {
        etSolution.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Legacy logic, kept if needed, but Technical Data form supersedes this
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSubmit() {
        btnSubmit.setOnClickListener {
            val isSolution = (rgResponseType.checkedRadioButtonId == R.id.rbSolution)
            val solutionText = etSolution.text.toString()

            if (solutionText.isBlank()) {
                etSolution.error = "Descrição obrigatória"
                return@setOnClickListener
            }

            var finalContent = solutionText
            var status: Int? = null

            if (isSolution) {
                // Validation
                if (etPonto.text.isNullOrBlank()) { etPonto.error = "Obrigatório"; return@setOnClickListener }
                if (etDrop.text.isNullOrBlank()) { etDrop.error = "Obrigatório"; return@setOnClickListener }
                if (!hasEvidence) {
                    Toast.makeText(this, "É obrigatório anexar uma evidência (foto)", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Construct HTML Table
                val initial = etMetragemInicial.text.toString()
                val final = etMetragemFinal.text.toString()
                val spent = (initial.toDoubleOrNull() ?: 0.0).let { i -> (final.toDoubleOrNull() ?: 0.0) - i }.coerceAtLeast(0.0)

                val alcas = etAlcas.text.toString().ifBlank { "0" }
                val esticador = etEsticador.text.toString().ifBlank { "0" }
                val conector = etConector.text.toString().ifBlank { "0" }

                finalContent = """
                    <div style="margin-bottom: 15px; border: 1px solid #ddd; padding: 10px; border-radius: 8px; background-color: #f9f9f9;">
                        <h3>📋 Dados Técnicos</h3>
                        <table style="width: 100%;">
                            <tr><td><b>Ponto:</b></td><td>${etPonto.text}</td></tr>
                            <tr><td><b>Drop:</b></td><td>${etDrop.text}</td></tr>
                            <tr><td><b>Alças:</b></td><td>${alcas}</td></tr>
                            <tr><td><b>Esticador:</b></td><td>${esticador}</td></tr>
                            <tr><td><b>Conector:</b></td><td>${conector}</td></tr>
                            <tr><td><b>Metragem Inicial:</b></td><td>${initial}m</td></tr>
                            <tr><td><b>Metragem Final:</b></td><td>${final}m</td></tr>
                            <tr><td><b>Metragem Gasta:</b></td><td><b>${spent}m</b></td></tr>
                            <tr><td><b>Extras:</b></td><td>${etExtras.text}</td></tr>
                        </table>
                    </div>
                    <div>
                        <strong>Ação Realizada:</strong><br/>
                        ${solutionText.replace("\n", "<br>")}
                    </div>
                """.trimIndent()

                status = 5 // Solved
            } else {
                finalContent = solutionText.replace("\n", "<br>")
                status = 2 // Processing
            }

            saveSolution(finalContent, status)
        }
    }

    private fun saveSolution(content: String, status: Int?) {
        lifecycleScope.launch {
            val solution = Solution(
                ticketId = ticketId,
                content = content,
                status = status
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
        hasEvidence = true
    }
}
