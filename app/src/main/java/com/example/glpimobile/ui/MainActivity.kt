package com.example.glpimobile.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.R
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.model.Ticket
import com.example.glpimobile.network.ApiClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var ticketsRecyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var etSearch: TextInputEditText

    private var allTickets: List<Ticket> = emptyList()
    private var currentFilterStatus: Int? = null // null means all
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.getSessionToken(this) == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBarMain)
        ticketsRecyclerView = findViewById(R.id.rvTickets)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        etSearch = findViewById(R.id.etSearch)

        setupRecyclerView()
        setupFilters()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        fetchTickets()
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter(emptyList())
        ticketsRecyclerView.adapter = ticketAdapter
        ticketsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentFilterStatus = null
            } else {
                val chipId = checkedIds[0]
                currentFilterStatus = when (chipId) {
                    R.id.chipNew -> 1 // Assuming GLPI status 1 is New
                    R.id.chipAssigned -> 2 // Assuming GLPI status 2 is Assigned
                    R.id.chipPending -> 4 // Assuming GLPI status 4 is Pending
                    R.id.chipSolved -> 5 // Assuming GLPI status 5 is Solved
                    else -> null
                }
            }
            applyFilters()
        }
    }

    private fun setupSearch() {
        etSearch.doOnTextChanged { text, _, _, _ ->
            currentSearchQuery = text.toString()
            applyFilters()
        }
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
                        allTickets = tickets
                        applyFilters()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error fetching tickets: ${response.code()}", Toast.LENGTH_SHORT).show()
                    if (response.code() == 401) {
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

    private fun applyFilters() {
        var filteredList = allTickets

        // Status Filter
        if (currentFilterStatus != null) {
            filteredList = filteredList.filter { it.status == currentFilterStatus } // Need to make sure Ticket model has status
        }

        // Search Filter
        if (currentSearchQuery.isNotEmpty()) {
            val query = currentSearchQuery.lowercase(Locale.getDefault())
            filteredList = filteredList.filter {
                it.name.lowercase(Locale.getDefault()).contains(query) ||
                it.id.toString().contains(query)
            }
        }

        ticketAdapter.updateTickets(filteredList)
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
