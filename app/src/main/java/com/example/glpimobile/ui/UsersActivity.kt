package com.example.glpimobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.R
import com.example.glpimobile.model.GlpiUser
import com.example.glpimobile.model.UserRepository
import com.google.android.material.textfield.TextInputEditText

class UsersActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvCount: TextView
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        val toolbar = findViewById<Toolbar>(R.id.toolbarUsers)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvUsers  = findViewById(R.id.rvUsers)
        etSearch = findViewById(R.id.etUserSearch)
        tvCount  = findViewById(R.id.tvUserCount)

        adapter = UserAdapter(UserRepository.allUsers) { user ->
            // clique no telefone → abre discador
            val phone = user.phone.replace(Regex("[^0-9+]"), "")
            if (phone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }

        rvUsers.adapter = adapter
        rvUsers.layoutManager = LinearLayoutManager(this)

        updateCount(UserRepository.allUsers.size)

        etSearch.doOnTextChanged { text, _, _, _ ->
            val results = UserRepository.search(text.toString())
            adapter.updateUsers(results)
            updateCount(results.size)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateCount(count: Int) {
        tvCount.text = "$count usuário(s)"
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────
class UserAdapter(
    private var users: List<GlpiUser>,
    private val onPhoneClick: (GlpiUser) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar:  TextView  = v.findViewById(R.id.tvAvatar)
        val name:    TextView  = v.findViewById(R.id.tvUserName)
        val title:   TextView  = v.findViewById(R.id.tvUserTitle)
        val dept:    TextView  = v.findViewById(R.id.tvUserDept)
        val phone:   ImageView = v.findViewById(R.id.ivPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.avatar.text = user.initials
        holder.name.text   = user.name
        holder.title.text  = user.title.ifEmpty { "" }
        holder.dept.text   = buildDeptCity(user)

        // Cores alternadas no avatar para distinguir visualmente
        val colors = listOf(
            0xFF1565C0.toInt(), // azul escuro
            0xFF2E7D32.toInt(), // verde escuro
            0xFF6A1B9A.toInt(), // roxo
            0xFFC62828.toInt(), // vermelho escuro
            0xFF00838F.toInt()  // ciano escuro
        )
        holder.avatar.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(colors[position % colors.size])
        )

        val hasPhone = user.phone.isNotEmpty()
        holder.phone.visibility = if (hasPhone) View.VISIBLE else View.INVISIBLE
        holder.phone.setOnClickListener {
            if (hasPhone) onPhoneClick(user)
        }
    }

    private fun buildDeptCity(user: GlpiUser): String {
        return listOfNotNull(
            user.department.takeIf { it.isNotEmpty() },
            user.city.takeIf { it.isNotEmpty() }
        ).joinToString(" · ")
    }

    fun updateUsers(newUsers: List<GlpiUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
