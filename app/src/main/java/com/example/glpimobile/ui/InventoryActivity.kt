package com.example.glpimobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import com.example.glpimobile.R
import com.example.glpimobile.network.ApiClient
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class InventoryActivity : AppCompatActivity() {

    private lateinit var rvInventory: RecyclerView
    private lateinit var progressInventory: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var chipGroupType: ChipGroup
    private lateinit var fabAddDevice: FloatingActionButton
    private lateinit var adapter: DeviceAdapter

    // Tipo atual selecionado: "Computer", "NetworkEquipment", "Printer", "Peripheral"
    private var currentType = "Computer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        val toolbar = findViewById<Toolbar>(R.id.toolbarInventory)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvInventory = findViewById(R.id.rvInventory)
        progressInventory = findViewById(R.id.progressInventory)
        tvEmpty = findViewById(R.id.tvEmpty)
        chipGroupType = findViewById(R.id.chipGroupType)
        fabAddDevice = findViewById(R.id.fabAddDevice)

        adapter = DeviceAdapter(emptyList(), { currentType })
        rvInventory.adapter = adapter
        rvInventory.layoutManager = LinearLayoutManager(this)

        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentType = when (checkedIds[0]) {
                    R.id.chipComputer   -> "Computer"
                    R.id.chipNetwork    -> "NetworkEquipment"
                    R.id.chipPrinter    -> "Printer"
                    R.id.chipPeripheral -> "Peripheral"
                    else -> "Computer"
                }
                fetchDevices()
            }
        }

        fabAddDevice.setOnClickListener {
            val intent = Intent(this, AddDeviceActivity::class.java)
            intent.putExtra(AddDeviceActivity.EXTRA_TYPE, currentType)
            startActivity(intent)
        }

        fetchDevices()
    }

    override fun onResume() {
        super.onResume()
        fetchDevices()
    }

    private fun fetchDevices() {
        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null) ?: return
        val appToken  = prefs.getString("app_token", null)  ?: return

        val api = ApiClient.getApiService(this, serverUrl, appToken)
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = when (currentType) {
                    "NetworkEquipment" -> api.getNetworkEquipment()
                    "Printer"          -> api.getPrinters()
                    "Peripheral"       -> api.getPeripherals()
                    else               -> api.getComputers()
                }

                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateItems(items)
                    tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@InventoryActivity,
                        "Erro ao buscar inventário: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InventoryActivity,
                    "Erro de rede: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressInventory.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

// ── Adapter simples inline ────────────────────────────────────────────────────
class DeviceAdapter(private var items: List<JsonObject>, private val typeProvider: () -> String) :
    RecyclerView.Adapter<DeviceAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name:     TextView = v.findViewById(R.id.tvDeviceName)
        val serial:   TextView = v.findViewById(R.id.tvDeviceSerial)
        val location: TextView = v.findViewById(R.id.tvDeviceLocation)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text     = item.getAsJsonPrimitive("name")?.asString ?: "Sem nome"
        val serial           = item.getAsJsonPrimitive("serial")?.asString ?: ""
        holder.serial.text   = if (serial.isNotEmpty()) "Serial: $serial" else "Sem serial"
        val location         = item.getAsJsonPrimitive("locations_id")?.asString ?: ""
        holder.location.text = if (location.isNotEmpty()) "Local: $location" else ""

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DeviceDetailActivity::class.java)
            intent.putExtra("device_json", com.google.gson.Gson().toJson(item))
            intent.putExtra("device_type", typeProvider())
            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateItems(newItems: List<JsonObject>) {
        items = newItems
        notifyDataSetChanged()
    }
}
