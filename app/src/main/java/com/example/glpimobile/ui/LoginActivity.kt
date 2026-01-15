package com.example.glpimobile.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rememberMeCheckBox: CheckBox

    companion object {
        private const val SERVER_URL = "https://suporte.tecnoit.com.br/"
        private const val APP_TOKEN = "Xj0RxV34GlelfYzuuPR9wXyrcw1cHnSHfThr7Yfy"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.etUsername)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        rememberMeCheckBox = findViewById(R.id.cbRememberMe)

        // Load saved credentials
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("remember_me", false)) {
            usernameEditText.setText(prefs.getString("saved_username", ""))
            passwordEditText.setText(prefs.getString("saved_password", ""))
            rememberMeCheckBox.isChecked = true
        }

        loginButton.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Save server URL and App-Token for later use
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("server_url", SERVER_URL)
            .putString("app_token", APP_TOKEN)
            .remove("glpi_user_id") // Clear old ID

        if (rememberMeCheckBox.isChecked) {
            editor.putBoolean("remember_me", true)
            editor.putString("saved_username", username)
            editor.putString("saved_password", password)
        } else {
            editor.remove("remember_me")
            editor.remove("saved_username")
            editor.remove("saved_password")
        }
        editor.apply()

        showLoading(true)

        val apiService = ApiClient.getApiService(this, SERVER_URL, APP_TOKEN)

        lifecycleScope.launch {
            try {
                // Pass null for Authorization header and use query parameters for login/password
                val response = apiService.initSession(null, APP_TOKEN, true, username, password)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val sessionToken = body.sessionToken
                    SessionManager.saveSessionToken(this@LoginActivity, sessionToken)

                    // Parse Entities
                    val entities = parseEntities(body.session?.myEntities)
                    val activeEntityId = body.session?.activeEntityId
                    val glpiID = body.session?.glpiID?.toIntOrNull() ?: 0
                    saveEntities(entities, activeEntityId, glpiID)

                    if (entities.size > 1) {
                         showEntitySelectionDialog(entities, apiService, sessionToken)
                    } else {
                         navigateToMainApp()
                    }
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

    private fun showEntitySelectionDialog(entities: List<Entity>, apiService: com.example.glpimobile.network.GlpiApiService, sessionToken: String) {
        val entityNames = entities.map { it.name }.toTypedArray()

        // Find current selection index if possible, else 0
        var checkedItem = 0

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Entity")
        builder.setSingleChoiceItems(entityNames, checkedItem) { dialog, which ->
             val selectedEntity = entities[which]

             showLoading(true)
             dialog.dismiss()

             lifecycleScope.launch {
                 try {
                     val jsonBody = com.google.gson.JsonObject()
                     jsonBody.addProperty("entities_id", selectedEntity.id)

                     val response = apiService.changeActiveEntity(sessionToken, APP_TOKEN, jsonBody)

                     if (response.isSuccessful) {
                         saveEntities(entities, selectedEntity.id.toString())
                         navigateToMainApp()
                     } else {
                         Toast.makeText(this@LoginActivity, "Failed to switch entity", Toast.LENGTH_SHORT).show()
                         // Navigate anyway or retry? Navigate anyway for now, defaulting to what initSession gave
                         navigateToMainApp()
                     }
                 } catch (e: Exception) {
                     Toast.makeText(this@LoginActivity, "Error switching entity: ${e.message}", Toast.LENGTH_SHORT).show()
                     navigateToMainApp()
                 } finally {
                     showLoading(false)
                 }
             }
        }
        // Prevent cancelling without selection if strict, or allow default
        builder.setCancelable(false)
        builder.show()
    }

    private fun saveEntities(entities: List<Entity>, activeEntityId: String?, glpiID: Int = 0) {
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(entities)
        val editor = prefs.edit().putString("saved_entities", json)
        if (activeEntityId != null) {
            editor.putString("active_entity_id", activeEntityId)
        }
        if (glpiID != 0) {
            editor.putInt("glpi_user_id", glpiID)
        }
        editor.apply()
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
        usernameEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
    }
}
