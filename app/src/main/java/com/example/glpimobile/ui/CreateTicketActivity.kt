package com.example.glpimobile.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.model.Entity
import com.example.glpimobile.network.ApiClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class CreateTicketActivity : AppCompatActivity() {

    private lateinit var spinnerEntities: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private var entities: List<Entity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_ticket)

        spinnerEntities = findViewById(R.id.spinnerEntities)
        etTitle = findViewById(R.id.etTicketTitle)
        etContent = findViewById(R.id.etTicketContent)
        btnSubmit = findViewById(R.id.btnCreateTicket)
        progressBar = findViewById(R.id.progressBarCreate)

        loadEntities()
        setupSpinner()

        btnSubmit.setOnClickListener {
            submitTicket()
        }
    }

    private fun loadEntities() {
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val json = prefs.getString("saved_entities", null)
        if (json != null) {
            val type = object : TypeToken<List<Entity>>() {}.type
            entities = Gson().fromJson(json, type)
        }
    }

    private fun setupSpinner() {
        if (entities.isEmpty()) {
            Toast.makeText(this, "No entities found. Please re-login.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            entities.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEntities.adapter = adapter
    }

    private fun submitTicket() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedEntityIndex = spinnerEntities.selectedItemPosition
        if (selectedEntityIndex < 0 || selectedEntityIndex >= entities.size) {
            Toast.makeText(this, "Please select an entity", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedEntityId = entities[selectedEntityIndex].id

        showLoading(true)

        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: ""
        val appToken = prefs.getString("app_token", "") ?: ""
        val sessionToken = SessionManager.getSessionToken(this)

        if (sessionToken == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val apiService = ApiClient.getApiService(this, serverUrl, appToken)

        lifecycleScope.launch {
            try {
                // Construct Payload
                val input = JsonObject()
                input.addProperty("name", title)
                input.addProperty("content", content)
                input.addProperty("entities_id", selectedEntityId)
                input.addProperty("status", 2) // Processing (assigned) or New

                val payload = JsonObject()
                payload.add("input", input)

                val response = apiService.createTicket(payload)
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateTicketActivity, "Ticket created successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()
                    Toast.makeText(this@CreateTicketActivity, "Error: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@CreateTicketActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !isLoading
        etTitle.isEnabled = !isLoading
        etContent.isEnabled = !isLoading
        spinnerEntities.isEnabled = !isLoading
    }
}
