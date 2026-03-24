package com.example.glpimobile.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.network.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvID: TextView
    private lateinit var tvSerial: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvComment: TextView
    private lateinit var etUserSearch: TextInputEditText
    private lateinit var cardSelectedUser: MaterialCardView
    private lateinit var tvSelectedUserName: TextView
    private lateinit var btnRemoveUser: ImageButton
    private lateinit var btnAttachTerm: MaterialButton
    private lateinit var btnConfirm: MaterialButton
    private lateinit var progress: ProgressBar

    private var device: JsonObject? = null
    private var deviceType: String = "Computer"
    private var selectedUser: JsonObject? = null
    private var currentPhotoPath: String? = null
    private var termPhotoFile: File? = null

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                termPhotoFile = File(path)
                btnAttachTerm.text = "Termo Anexado (OK)"
                btnAttachTerm.setIconResource(android.R.drawable.checkbox_on_background)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)

        val deviceJson = intent.getStringExtra("device_json")
        device = Gson().fromJson(deviceJson, JsonObject::class.java)
        deviceType = intent.getStringExtra("device_type") ?: "Computer"

        tvTitle = findViewById(R.id.tvDeviceTitle)
        tvID = findViewById(R.id.tvDeviceID)
        tvSerial = findViewById(R.id.tvDeviceSerialDetail)
        tvLocation = findViewById(R.id.tvDeviceLocationDetail)
        tvComment = findViewById(R.id.tvDeviceComment)
        etUserSearch = findViewById(R.id.etUserSearch)
        cardSelectedUser = findViewById(R.id.cardSelectedUser)
        tvSelectedUserName = findViewById(R.id.tvSelectedUserName)
        btnRemoveUser = findViewById(R.id.btnRemoveSelectedUser)
        btnAttachTerm = findViewById(R.id.btnAttachTerm)
        btnConfirm = findViewById(R.id.btnConfirmLink)
        progress = findViewById(R.id.progressDetail)

        setupData()

        btnAttachTerm.setOnClickListener { dispatchTakePictureIntent() }
        btnRemoveUser.setOnClickListener {
            selectedUser = null
            cardSelectedUser.visibility = View.GONE
        }

        // Simples busca de usuário (apenas para exemplo, ideal seria um adapter/autocomplete)
        btnConfirm.setOnClickListener { performLink() }
    }

    private fun setupData() {
        device?.let {
            tvTitle.text = it.get("name")?.asString ?: "Dispositivo"
            tvID.text = "ID GLPI: #${it.get("id")?.asInt}"
            tvSerial.text = "S/N: ${it.get("serial")?.asString ?: "—"}"
            tvLocation.text = "Local: ${it.get("locations_id")?.asString ?: "—"}"
            tvComment.text = it.get("comment")?.asString ?: ""
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.glpimobile.fileprovider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePhotoLauncher.launch(takePictureIntent)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("TERM_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun performLink() {
        // Para este MVP, vamos buscar o usuário digitado diretamente se não selecionado
        val userName = etUserSearch.text.toString().trim()
        if (userName.isEmpty() && selectedUser == null) {
            Toast.makeText(this, "Informe o responsável", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("glpi_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", null) ?: return
        val appToken = prefs.getString("app_token", null) ?: return
        val sessionToken = prefs.getString("session_token", null) ?: return

        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(this@DeviceDetailActivity, serverUrl, appToken)
                val deviceId = device?.get("id")?.asInt ?: 0

                // 1. Atualizar comentário do Ativo
                val newComment = (device?.get("comment")?.asString ?: "") + "\nResponsável: $userName"
                val updatePayload = JsonObject().apply { addProperty("comment", newComment) }
                val updateRes = api.updateAsset(deviceType, deviceId, sessionToken, appToken, JsonObject().apply { add("input", updatePayload) })

                if (updateRes.isSuccessful) {
                    // 2. Upload do Termo se houver
                    termPhotoFile?.let { file ->
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        val manifest = "{\"input\": {\"name\": \"Termo - $userName\", \"_filename\": \"${file.name}\"}}"

                        val docRes = api.uploadDocument(sessionToken, appToken, manifest, body)
                        if (docRes.isSuccessful) {
                            val docId = docRes.body()?.get("id")?.asInt ?: 0
                            val linkPayload = JsonObject().apply {
                                val input = JsonObject()
                                input.addProperty("documents_id", docId)
                                input.addProperty("items_id", deviceId)
                                input.addProperty("itemtype", deviceType)
                                add("input", input)
                            }
                            api.linkDocumentToItem(sessionToken, appToken, linkPayload)
                        }
                    }
                    Toast.makeText(this@DeviceDetailActivity, "Vínculo realizado com sucesso!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@DeviceDetailActivity, "Erro ao atualizar: ${updateRes.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnConfirm.isEnabled = !loading
    }
}
