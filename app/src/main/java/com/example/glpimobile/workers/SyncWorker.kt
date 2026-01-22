package com.example.glpimobile.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.glpimobile.auth.SessionManager
import com.example.glpimobile.db.AppDatabase
import com.example.glpimobile.network.ApiClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).offlineActionDao()
        val pendingActions = dao.getPendingActions()

        if (pendingActions.isEmpty()) {
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences("glpi_prefs", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null)
        val appToken = prefs.getString("app_token", null)
        val sessionToken = SessionManager.getSessionToken(applicationContext)

        if (serverUrl == null || appToken == null || sessionToken == null) {
            return Result.failure()
        }

        val apiService = ApiClient.getApiService(applicationContext, serverUrl, appToken)

        for (action in pendingActions) {
            try {
                var success = false
                when (action.type) {
                    "SOLUTION" -> {
                        val payload = JsonParser().parse(action.payloadJson).asJsonObject
                        val response = apiService.addSolution(sessionToken, appToken, payload)
                        if (response.isSuccessful) {
                            // Check if status update is required
                            if (payload.has("status")) {
                                val newStatus = payload.get("status").asInt
                                val updatePayload = JsonObject()
                                val input = JsonObject()
                                input.addProperty("status", newStatus)
                                updatePayload.add("input", input)

                                val updateResponse = apiService.updateTicket(action.ticketId, sessionToken, appToken, updatePayload)
                                if (updateResponse.isSuccessful) success = true
                                else success = false // Fail if update fails? Or partial success?
                            } else {
                                success = true
                            }
                        }
                    }
                    "CONSUMABLE" -> {
                        // Assuming payload has { "item_id": 123, "qty": 1, ... }
                        // And we map it to Item_Ticket logic
                        val payloadObj = JsonParser().parse(action.payloadJson).asJsonObject

                        var itemId = payloadObj.get("item_id").asInt
                        val name = payloadObj.get("name").asString

                        // If itemId is 0, we need to create it first
                        if (itemId == 0) {
                            val newConsumable = JsonObject()
                            val input = JsonObject()
                            input.addProperty("name", name)
                            // Ideally set entities_id, etc. Assuming defaults for now.
                            newConsumable.add("input", input)

                            val createResponse = apiService.createConsumable(sessionToken, appToken, newConsumable)
                            if (createResponse.isSuccessful) {
                                val body = createResponse.body()
                                // Response usually {"id": 123, "message": ...}
                                if (body != null && body.has("id")) {
                                    itemId = body.get("id").asInt
                                } else {
                                    throw Exception("Failed to create consumable")
                                }
                            } else {
                                throw Exception("Failed to create consumable: ${createResponse.code()}")
                            }
                        }

                        // Construct GLPI payload for Item_Ticket
                        // {"input": {"tickets_id": ..., "items_id": ..., "itemtype": "ConsumableItem", "amount": ...}}
                        val glpiPayload = JsonObject()
                        val input = JsonObject()
                        input.addProperty("tickets_id", action.ticketId as Number)
                        input.addProperty("items_id", itemId as Number)
                        input.addProperty("itemtype", "ConsumableItem")
                        if (payloadObj.has("qty")) {
                            input.addProperty("amount", payloadObj.get("qty").asInt as Number)
                        } else {
                            input.addProperty("amount", 1 as Number)
                        }

                        glpiPayload.add("input", input)

                        val response = apiService.linkItemToTicket(sessionToken, appToken, glpiPayload)
                        if (response.isSuccessful) success = true
                    }
                    "DOCUMENT" -> {
                        val payloadObj = JsonParser().parse(action.payloadJson).asJsonObject
                        val path = payloadObj.get("path").asString
                        val file = File(path)
                        if (file.exists()) {
                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("filename", file.name, requestFile)

                            // Manifest JSON required by GLPI
                            val manifest = JsonObject()
                            val input = JsonObject()
                            input.addProperty("name", "Evidence " + file.name)
                            input.addProperty("tickets_id", action.ticketId as Number)
                            manifest.add("input", input)

                            val response = apiService.uploadDocument(sessionToken, appToken, manifest.toString(), body)
                            if (response.isSuccessful) success = true
                        } else {
                            // File lost? Fail but mark as error to avoid loop?
                            dao.updateStatus(action.id, "FILE_NOT_FOUND")
                            continue
                        }
                    }
                }

                if (success) {
                    dao.updateStatus(action.id, "SYNCED")
                }
            } catch (e: Exception) {
                // Keep pending, retry later
                return Result.retry()
            }
        }

        return Result.success()
    }
}
