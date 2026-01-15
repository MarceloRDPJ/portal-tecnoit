package com.example.glpimobile.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.model.Consumable
import com.example.glpimobile.network.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsumableDialog(private val context: Context, private val onConsumableSelected: (Consumable, Int) -> Unit) {

    fun show() {
        val prefs = context.getSharedPreferences("glpi_prefs", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null)
        val appToken = prefs.getString("app_token", null)
        val sessionToken = SessionManager.getSessionToken(context)

        if (serverUrl == null || appToken == null || sessionToken == null) {
            Toast.makeText(context, "Offline or not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = ApiClient.getApiService(context, serverUrl, appToken)

        // Show a loading dialog
        val loadingDialog = AlertDialog.Builder(context)
            .setMessage("Loading consumables...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Fetch consumables from API
                val fetchedConsumables = withContext(Dispatchers.IO) {
                    try {
                        val response = apiService.getConsumables(sessionToken, appToken, "0-200")
                        if (response.isSuccessful && response.body() != null) {
                             response.body()!!.map { obj ->
                                 Consumable(
                                     id = if (obj.has("id")) obj.get("id").asInt else 0,
                                     name = if (obj.has("name")) obj.get("name").asString else "Unknown",
                                     ref = if (obj.has("ref") && !obj.get("ref").isJsonNull) obj.get("ref").asString else null,
                                     stock = 0 // Not provided in basic view
                                 )
                             }.sortedBy { it.name }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                loadingDialog.dismiss()
                showSelectionDialog(fetchedConsumables)

            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(context, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
                showManualEntryDialog()
            }
        }
    }

    private fun showSelectionDialog(consumables: List<Consumable>) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val spinner = Spinner(context)
        val items = consumables.toMutableList()
        // Add "Other" option
        items.add(Consumable(0, "Outro (Cadastrar Novo)", null, 0))

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items.map { it.name })
        spinner.adapter = adapter
        layout.addView(spinner)

        val qtyInput = EditText(context)
        qtyInput.hint = "Quantity"
        qtyInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        qtyInput.setText("1")
        layout.addView(qtyInput)

        AlertDialog.Builder(context)
            .setTitle("Add Consumable")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val selectedIndex = spinner.selectedItemPosition
                val qty = qtyInput.text.toString().toIntOrNull() ?: 1

                if (selectedIndex >= 0 && selectedIndex < items.size) {
                    val selected = items[selectedIndex]
                    if (selected.id == 0 && selected.name.startsWith("Outro")) {
                        showManualEntryDialog()
                    } else {
                        onConsumableSelected(selected, qty)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManualEntryDialog() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val nameInput = EditText(context)
        nameInput.hint = "Consumable Name / ID"
        layout.addView(nameInput)

        val qtyInput = EditText(context)
        qtyInput.hint = "Quantity"
        qtyInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(qtyInput)

        AlertDialog.Builder(context)
            .setTitle("Add Consumable")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                val qty = qtyInput.text.toString().toIntOrNull() ?: 1
                if (name.isNotEmpty()) {
                    // Create a dummy consumable object
                    val consumable = Consumable(0, name, null, 0)
                    onConsumableSelected(consumable, qty)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
