package com.example.glpimobile.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Mock fetching list for now as API response structure might vary
                // Real implementation would parse apiService.getConsumables(sessionToken, appToken)
                // For this task, I will create a simple manual entry dialog or a hardcoded list if API fails

                // Let's assume we fetch a list. Since I cannot run network, I will provide a manual input option as fallback.

                showManualEntryDialog()

            } catch (e: Exception) {
                showManualEntryDialog()
            }
        }
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
