package com.example.glpimobile.ui

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.glpimobile.R

class TicketDetailActivity : AppCompatActivity() {

    private var ticketId: Int = 0
    private var ticketName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        ticketId = intent.getIntExtra("TICKET_ID", 0)
        ticketName = intent.getStringExtra("TICKET_NAME")
        val ticketContent = intent.getStringExtra("TICKET_CONTENT")
        val ticketStatus = intent.getIntExtra("TICKET_STATUS", 0)
        val ticketDate = intent.getStringExtra("TICKET_DATE")

        if (ticketId == 0) {
            finish()
            return
        }

        val tvTitle: TextView = findViewById(R.id.tvDetailTitle)
        val tvStatus: TextView = findViewById(R.id.tvDetailStatus)
        val tvDate: TextView = findViewById(R.id.tvDetailDate)
        val tvDesc: TextView = findViewById(R.id.tvDetailDescription)
        val btnRespond: Button = findViewById(R.id.btnRespond)

        tvTitle.text = "[#$ticketId] $ticketName"
        tvStatus.text = getStatusLabel(ticketStatus)
        tvDate.text = "Created: $ticketDate"

        // GLPI content is often HTML
        tvDesc.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(ticketContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(ticketContent)
        }

        btnRespond.setOnClickListener {
            val intent = Intent(this, RespondTicketActivity::class.java)
            intent.putExtra("TICKET_ID", ticketId)
            startActivity(intent)
        }
    }

    private fun getStatusLabel(status: Int): String {
        return when (status) {
            1 -> "New"
            2 -> "Assigned"
            3 -> "Planned"
            4 -> "Pending"
            5 -> "Solved"
            6 -> "Closed"
            else -> "Unknown ($status)"
        }
    }
}
