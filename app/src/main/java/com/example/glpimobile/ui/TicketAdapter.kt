package com.example.glpimobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.R
import com.example.glpimobile.model.Ticket

class TicketAdapter(private var tickets: List<Ticket>) :
    RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int = tickets.size

    fun updateTickets(newTickets: List<Ticket>) {
        tickets = newTickets
        notifyDataSetChanged()
    }

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvTicketTitle)
        private val dateTextView: TextView = itemView.findViewById(R.id.tvTicketCreationDate)
        private val statusTextView: TextView = itemView.findViewById(R.id.tvTicketStatus)

        fun bind(ticket: Ticket) {
            titleTextView.text = "[#${ticket.id}] ${ticket.name}"
            dateTextView.text = "Created on: ${ticket.creationDate}"
            statusTextView.text = getStatusLabel(ticket.status)

            itemView.setOnClickListener {
                val intent = android.content.Intent(itemView.context, TicketDetailActivity::class.java).apply {
                    putExtra("TICKET_ID", ticket.id)
                    putExtra("TICKET_NAME", ticket.name)
                    putExtra("TICKET_CONTENT", ticket.content)
                    putExtra("TICKET_STATUS", ticket.status)
                    putExtra("TICKET_DATE", ticket.creationDate)
                }
                itemView.context.startActivity(intent)
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
                else -> "Unknown"
            }
        }
    }
}
