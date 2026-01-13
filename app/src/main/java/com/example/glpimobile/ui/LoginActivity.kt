package com.example.glpimobile.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.model.Entity
import com.example.glpimobile.network.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var serverUrlEditText: EditText
    private lateinit var appTokenEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        serverUrlEditText = findViewById(R.id.etGlpiServerUrl)
        appTokenEditText = findViewById(R.id.etAppToken)
        usernameEditText = findViewById(R.id.etUsername)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        loginButton.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val serverUrl = serverUrlEditText.text.toString().trim()
        val appToken = appTokenEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (serverUrl.isEmpty() || appToken.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Save server URL and App-Token for later use
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("server_url", serverUrl)
            .putString("app_token", appToken)
            .apply()

        showLoading(true)

        val apiService = ApiClient.getApiService(this, serverUrl, appToken)
        val basicAuth = "Basic " + Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)

        lifecycleScope.launch {
            try {
                val response = apiService.initSession(basicAuth, appToken, true)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val sessionToken = body.sessionToken
                    SessionManager.saveSessionToken(this@LoginActivity, sessionToken)

                    // Parse Entities
                    val entities = parseEntities(body.session?.myEntities)
                    saveEntities(entities)

                    navigateToMainApp()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LoginActivity, "Login failed: $errorBody", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
        }
    }

    private fun parseEntities(rawEntities: com.google.gson.JsonElement?): List<Entity> {
        val entities = mutableListOf<Entity>()
        if (rawEntities == null) return entities

        try {
            if (rawEntities.isJsonArray) {
                rawEntities.asJsonArray.forEach { element ->
                    if (element.isJsonObject) {
                        val obj = element.asJsonObject
                        val id = obj.get("id").asInt
                        val name = if (obj.has("completename")) obj.get("completename").asString else obj.get("name").asString
                        entities.add(Entity(id, name))
                    }
                }
            } else if (rawEntities.isJsonObject) {
                rawEntities.asJsonObject.entrySet().forEach { entry ->
                    val value = entry.value
                    val id = entry.key.toIntOrNull() ?: 0
                    if (value.isJsonObject) {
                        val obj = value.asJsonObject
                        val parsedId = if (obj.has("id")) obj.get("id").asInt else id
                        val name = if (obj.has("completename")) obj.get("completename").asString
                                   else if (obj.has("name")) obj.get("name").asString
                                   else "Entity $id"
                        entities.add(Entity(parsedId, name))
                    } else if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        entities.add(Entity(id, value.asString))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort alphabetically
        entities.sortBy { it.name }
        return entities
    }

    private fun saveEntities(entities: List<Entity>) {
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(entities)
        prefs.edit().putString("saved_entities", json).apply()
    }

    private fun navigateToMainApp() {
        Intent(this, MainActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(it)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        serverUrlEditText.isEnabled = !isLoading
        appTokenEditText.isEnabled = !isLoading
        usernameEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
    }
}
