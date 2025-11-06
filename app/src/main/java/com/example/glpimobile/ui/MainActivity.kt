package com.example.glpimobile.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.R
import com.example.glpimobile.auth.TokenManager
import com.example.glpimobile.network.ApiClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var ticketsRecyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TokenManager.getAccessToken(this) == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBarMain)
        ticketsRecyclerView = findViewById(R.id.rvTickets)
        setupRecyclerView()
        fetchTickets()
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter(emptyList())
        ticketsRecyclerView.adapter = ticketAdapter
        ticketsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchTickets() {
        showLoading(true)
        val serverUrl = getSharedPreferences("glpi_prefs", MODE_PRIVATE).getString("server_url", null)
        if (serverUrl == null) {
            Toast.makeText(this, "Server URL not found. Please log in again.", Toast.LENGTH_LONG).show()
            TokenManager.clearTokens(this)
            navigateToLogin()
            return
        }

        val apiService = ApiClient.getApiService(this, serverUrl)

        lifecycleScope.launch {
            try {
                val response = apiService.getTickets()
                if (response.isSuccessful) {
                    val tickets = response.body()
                    if (tickets != null) {
                        ticketAdapter.updateTickets(tickets)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    if (response.code() == 401) { // Unauthorized
                        TokenManager.clearTokens(this@MainActivity)
                        navigateToLogin()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(it)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
