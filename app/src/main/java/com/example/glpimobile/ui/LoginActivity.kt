package com.example.glpimobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.auth.TokenManager
import com.example.glpimobile.network.ApiClient
import com.example.glpimobile.network.TokenRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    // IMPORTANT: These are placeholder values.
    // You must register your app as an OAuth Client in your GLPI instance
    // under Setup > OAuth Clients to get a real client_id and client_secret.
    private val CLIENT_ID = "your_client_id"
    private val CLIENT_SECRET = "your_client_secret" // WARNING: Storing the secret in the app is not secure for production.
    private val REDIRECT_URI = "glpimobile://callback"

    private lateinit var serverUrlEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        serverUrlEditText = findViewById(R.id.etGlpiServerUrl)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        loginButton.setOnClickListener {
            startAuthenticationFlow()
        }
    }

    private fun startAuthenticationFlow() {
        val serverUrl = serverUrlEditText.text.toString().trim()
        if (serverUrl.isEmpty()) {
            serverUrlEditText.error = "Please enter your GLPI server URL"
            return
        }
        // Save server URL for later use
        getSharedPreferences("glpi_prefs", MODE_PRIVATE).edit().putString("server_url", serverUrl).apply()


        showLoading(true)

        val authUri = Uri.parse("$serverUrl/api.php/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "api")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        try {
            customTabsIntent.launchUrl(this, authUri)
        } catch (e: Exception) {
            serverUrlEditText.error = "Could not open browser for login."
            showLoading(false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        showLoading(false)
        val uri = intent.data
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                exchangeCodeForToken(code)
                // Clear the intent data to prevent re-triggering
                intent.data = null
            } else {
                val error = uri.getQueryParameter("error")
                Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        showLoading(true)
        val serverUrl = getSharedPreferences("glpi_prefs", MODE_PRIVATE).getString("server_url", null)
        if (serverUrl == null) {
            Toast.makeText(this, "Server URL not found.", Toast.LENGTH_LONG).show()
            showLoading(false)
            return
        }

        // CORRECTED: Pass the context 'this' to the getApiService method.
        val apiService = ApiClient.getApiService(this, serverUrl)
        val tokenRequest = TokenRequest(
            client_id = CLIENT_ID,
            client_secret = CLIENT_SECRET,
            code = code
        )

        lifecycleScope.launch {
            try {
                val response = apiService.getAccessToken(tokenRequest)
                if (response.isSuccessful && response.body() != null) {
                    val tokenResponse = response.body()!!
                    TokenManager.saveTokens(
                        this@LoginActivity,
                        tokenResponse.access_token,
                        tokenResponse.refresh_token
                    )
                    navigateToMainApp()
                } else {
                    Toast.makeText(this@LoginActivity, "Failed to get access token: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
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
    }
}
