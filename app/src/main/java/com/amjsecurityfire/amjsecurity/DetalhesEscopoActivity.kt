package com.amjsecurityfire.amjsecurity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class DetalhesEscopoActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private val PICK_PDF_REQUEST_CODE = 2
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewDetalhes: TextView
    private lateinit var editBtn: ImageButton
    private lateinit var pdfDownloadButton: Button

    private var escopoId: String = ""
    private var pdfUrl: String = ""

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalhe_escopos)

        db = FirebaseFirestore.getInstance()

        val voltarMenuButton = findViewById<Button>(R.id.btnVoltarMenu)
        val voltarEscopo = findViewById<ImageButton>(R.id.voltarEscopo)
        textViewDetalhes = findViewById(R.id.textViewDetalhes)
        editBtn = findViewById(R.id.editBtn)
        pdfDownloadButton = findViewById(R.id.btnDownloadPdf)
        progressBar = findViewById(R.id.progressBar)

        escopoId = intent.getStringExtra("escopoId") ?: ""

        if (escopoId.isNotEmpty()) {
            buscarDadosDoFirestore(escopoId)
        } else if (intent.hasExtra("numeroEscopo") && intent.hasExtra("empresa")) {
            mostrarDadosIntent()
        } else {
            Toast.makeText(this, "Não foi possível carregar os dados do escopo.", Toast.LENGTH_LONG).show()
            finish()
        }

        editBtn.setOnClickListener {
            abrirTelaEdicao()
        }

        pdfDownloadButton.setOnClickListener {
            if (pdfUrl.isNotEmpty()) {
                if (checkAndRequestPermissions()) {
                    abrirPdf(pdfUrl)
                }
            } else {
                Toast.makeText(this, "PDF não disponível para visualização.", Toast.LENGTH_SHORT).show()
            }
        }

        voltarMenuButton.setOnClickListener { finish() }
        voltarEscopo.setOnClickListener { finish() }
    }

    private fun mostrarDadosIntent() {
        val numeroEscopo = intent.getStringExtra("numeroEscopo") ?: "N/A"
        val empresa = intent.getStringExtra("empresa") ?: "N/A"
        val dataEstimativa = intent.getStringExtra("dataEstimativa") ?: "N/A"
        val tipoServico = intent.getStringExtra("tipoServico") ?: "N/A"
        val status = intent.getStringExtra("status") ?: "N/A"
        val resumoEscopo = intent.getStringExtra("resumoEscopo") ?: "N/A"
        val numeroPedidoCompra = intent.getStringExtra("numeroPedidoCompra") ?: "N/A"
        val criadorNome = intent.getStringExtra("criadorNome") ?: "N/A"
        val dataCriacao = intent.getStringExtra("dataCriacao") ?: "N/A"
        pdfUrl = intent.getStringExtra("pdfUrl") ?: ""

        textViewDetalhes.text = """
            Número: $numeroEscopo
            Empresa: $empresa
            Data Estimada: $dataEstimativa
            Tipo de Serviço: $tipoServico
            Status: $status
            Resumo: $resumoEscopo
            Número do Pedido de Compra: $numeroPedidoCompra
            Criado por: $criadorNome
            Data de Criação: $dataCriacao
        """.trimIndent()
    }

    private fun buscarDadosDoFirestore(escopoId: String) {
        progressBar.visibility = View.VISIBLE
        textViewDetalhes.visibility = View.GONE

        db.collection("escoposPendentes").document(escopoId)
            .get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE
                textViewDetalhes.visibility = View.VISIBLE

                if (doc != null && doc.exists()) {
                    val numeroEscopo = doc["numeroEscopo"]?.toString() ?: "N/A"
                    val empresa = doc["empresa"]?.toString() ?: "N/A"
                    val dataEstimativa = doc["dataEstimativa"]?.toString() ?: "N/A"
                    val tipoServico = doc["tipoServico"]?.toString() ?: "N/A"
                    val status = doc["status"]?.toString() ?: "N/A"
                    val resumoEscopo = doc["resumoEscopo"]?.toString() ?: "N/A"
                    val numeroPedidoCompra = doc["numeroPedidoCompra"]?.toString() ?: "N/A"
                    val criadorNome = doc["criadorNome"]?.toString() ?: "N/A"
                    val dataCriacao = doc["dataCriacao"]?.toString() ?: "N/A"
                    pdfUrl = doc["pdfUrl"]?.toString() ?: ""

                    val detalhes = """
                        Número: $numeroEscopo
                        Empresa: $empresa
                        Data Estimada: $dataEstimativa
                        Tipo de Serviço: $tipoServico
                        Status: $status
                        Resumo: $resumoEscopo
                        Número do Pedido de Compra: $numeroPedidoCompra
                        Criado por: $criadorNome
                        Data de Criação: $dataCriacao
                    """.trimIndent()

                    textViewDetalhes.text = detalhes
                } else {
                    Toast.makeText(this, "Escopo não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                textViewDetalhes.visibility = View.VISIBLE
                Toast.makeText(this, "Erro ao buscar escopo: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun abrirTelaEdicao() {
        val intent = Intent(this, EditarEscopoActivity::class.java).apply {
            putExtra("editMode", true)
            putExtra("escopoId", escopoId)
        }
        startActivityForResult(intent, 100)
    }

    private fun abrirPdf(pdfUrl: String) {
        try {
            val uri = Uri.parse(pdfUrl)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("DetalhesEscopo", "Erro ao tentar abrir o PDF: ${e.message}")
            Toast.makeText(this, "Erro ao abrir o PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openFilePicker()
            return true
        } else {
            val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pdfUrl.isNotEmpty()) {
                    abrirPdf(pdfUrl)
                }
            } else {
                Toast.makeText(this, "Permissão negada para acessar arquivos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_PDF_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            }
        }

        if (requestCode == 100 && resultCode == RESULT_OK) {
            buscarDadosDoFirestore(escopoId)
        }
    }
}
