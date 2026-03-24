package com.example.glpimobile.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import com.example.glpimobile.R
import com.example.glpimobile.network.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var btnScan: MaterialButton
    private lateinit var progress: ProgressBar

    private var deviceType = "Computer"
    private var currentPhotoPath: String? = null

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val bitmap = BitmapFactory.decodeFile(path)
                bitmap?.let { processImageOCR(it) }
            }
        }
    }

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
        btnScan           = findViewById(R.id.btnScanDevice)
        progress          = findViewById(R.id.progressAddDevice)

        btnSave.setOnClickListener { saveDevice() }
        btnScan.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.glpimobile.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePhotoLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
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
                    Snackbar.make(btnSave, "Dispositivo criado! ID: $id", Snackbar.LENGTH_LONG).show()
                    btnSave.postDelayed({ finish() }, 1500)
                } else {
                    val err = response.errorBody()?.string() ?: response.code().toString()
                    Snackbar.make(btnSave, "Erro ao criar: $err", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(btnSave, "Erro de rede: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun processImageOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        setLoading(true)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isNotEmpty()) {
                    val serialMatch = Regex("(?:S/N|SERIAL|SERIE|S/N:)\\s*([A-Z0-9]{5,})", RegexOption.IGNORE_CASE).find(text)
                    val modelMatch = Regex("(?:MODEL|MODELO|MOD:)\\s*([A-Z0-9\\-\\/]{3,})", RegexOption.IGNORE_CASE).find(text)

                    serialMatch?.let { etSerial.setText(it.groupValues[1].trim()) }
                    modelMatch?.let { etName.setText(it.groupValues[1].trim()) }

                    val currentComment = etComment.text.toString()
                    etComment.setText("${currentComment}${if (currentComment.isNotEmpty()) "\n" else ""}[OCR]: ${text.take(200)}...")

                    Toast.makeText(this, "OCR finalizado! Verifique os campos preenchidos.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Nenhum texto encontrado na etiqueta", Toast.LENGTH_SHORT).show()
                }
                setLoading(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha no OCR: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
        btnScan?.isEnabled = !loading
    }
}
