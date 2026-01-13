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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.glpimobile.R
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.network.ApiClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var ticketsRecyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var fabCreateTicket: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.getSessionToken(this) == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBarMain)
        ticketsRecyclerView = findViewById(R.id.rvTickets)
        fabCreateTicket = findViewById(R.id.fabCreateTicket)

        setupRecyclerView()

        fabCreateTicket.setOnClickListener {
            startActivity(Intent(this, CreateTicketActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh tickets when returning from creation
        fetchTickets()
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter(emptyList())
        ticketsRecyclerView.adapter = ticketAdapter
        ticketsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchTickets() {
        showLoading(true)
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null)
        val appToken = prefs.getString("app_token", null)

        if (serverUrl == null || appToken == null) {
            Toast.makeText(this, "Configuration missing. Please log in again.", Toast.LENGTH_LONG).show()
            SessionManager.clearSession(this)
            navigateToLogin()
            return
        }

        val apiService = ApiClient.getApiService(this, serverUrl, appToken)

        lifecycleScope.launch {
            try {
                val response = apiService.getTickets()
                if (response.isSuccessful) {
                    val tickets = response.body()
                    if (tickets != null) {
                        ticketAdapter.updateTickets(tickets)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error fetching tickets: ${response.code()}", Toast.LENGTH_SHORT).show()
                    if (response.code() == 401) { // Unauthorized, session might be expired
                        SessionManager.clearSession(this@MainActivity)
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
