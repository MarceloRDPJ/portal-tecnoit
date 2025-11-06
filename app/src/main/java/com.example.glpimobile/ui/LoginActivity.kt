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
import com.example.glpimobile.network.ApiClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    // This is the App-Token for your GLPI instance.
    private val APP_TOKEN = "wqiKYM7TOVHVgj1cW9lmzmvn8jwZQY59xSKSXLkx"

    private lateinit var serverUrlEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        serverUrlEditText = findViewById(R.id.etGlpiServerUrl)
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
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences("glpi_prefs", MODE_PRIVATE).edit().putString("server_url", serverUrl).apply()
        showLoading(true)

        val apiService = ApiClient.getApiService(this, serverUrl, APP_TOKEN)
        val basicAuth = "Basic " + Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)

        lifecycleScope.launch {
            try {
                val response = apiService.initSession(basicAuth, APP_TOKEN)
                if (response.isSuccessful && response.body() != null) {
                    val sessionToken = response.body()!!.sessionToken
                    SessionManager.saveSessionToken(this@LoginActivity, sessionToken)
                    navigateToMainApp()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LoginActivity, "Login failed: $errorBody", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                showLoading(false)
            }
        }
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
        usernameEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
    }
}
