package com.example.glpimobile.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.network.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class AddDeviceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "device_type"
    }

    private lateinit var etName: TextInputEditText
    private lateinit var etSerial: TextInputEditText
    private lateinit var etInventoryNumber: TextInputEditText
    private lateinit var etManufacturer: TextInputEditText
    private lateinit var etModel: TextInputEditText
    private lateinit var etComment: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var progress: ProgressBar

    private var deviceType = "Computer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        deviceType = intent.getStringExtra(EXTRA_TYPE) ?: "Computer"

        val label = when (deviceType) {
            "NetworkEquipment" -> "Equipamento de Rede"
            "Printer"          -> "Impressora"
            "Peripheral"       -> "Periférico"
            else               -> "Computador"
        }
        supportActionBar?.title = "Novo $label"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etName            = findViewById(R.id.etDeviceName)
        etSerial          = findViewById(R.id.etSerial)
        etInventoryNumber = findViewById(R.id.etInventoryNumber)
        etManufacturer    = findViewById(R.id.etManufacturer)
        etModel           = findViewById(R.id.etModel)
        etComment         = findViewById(R.id.etComment)
        btnSave           = findViewById(R.id.btnSaveDevice)
        progress          = findViewById(R.id.progressAddDevice)

        btnSave.setOnClickListener { saveDevice() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun saveDevice() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            etName.error = "Nome obrigatório"
            return
        }

        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null) ?: run {
            Toast.makeText(this, "Configuração ausente. Faça login novamente.", Toast.LENGTH_LONG).show()
            return
        }
        val appToken = prefs.getString("app_token", null) ?: return

        val payload = JsonObject().apply {
            addProperty("name",             name)
            addProperty("serial",           etSerial.text.toString().trim())
            addProperty("otherserial",      etInventoryNumber.text.toString().trim())
            addProperty("comment",          etComment.text.toString().trim())
            // manufacturer e model serão IDs no GLPI; enviamos como string para simplificar
            // (o servidor pode ignorar se não existir o ID)
        }

        val api = ApiClient.getApiService(this, serverUrl, appToken)
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = when (deviceType) {
                    "NetworkEquipment" -> api.createNetworkEquipment(payload)
                    "Printer"          -> api.createPrinter(payload)
                    "Peripheral"       -> api.createPeripheral(payload)
                    else               -> api.createComputer(payload)
                }

                if (response.isSuccessful) {
                    val id = response.body()?.getAsJsonPrimitive("id")?.asInt
                    Toast.makeText(this@AddDeviceActivity,
                        "Dispositivo criado! ID: $id", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: response.code().toString()
                    Toast.makeText(this@AddDeviceActivity,
                        "Erro ao criar dispositivo: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddDeviceActivity,
                    "Erro de rede: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
    }
}
