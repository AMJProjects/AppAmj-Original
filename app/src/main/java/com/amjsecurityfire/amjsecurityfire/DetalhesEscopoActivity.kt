package com.amjsecurityfire.amjsecurityfire;

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amjsecurityfire.amjsecurityfire.R
import android.Manifest
import android.content.pm.PackageManager

public class DetalhesEscopoActivity : AppCompatActivity(){
    private val PERMISSION_REQUEST_CODE = 1
    private val PICK_PDF_REQUEST_CODE = 2

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalhe_escopos)

        val voltarMenuButton = findViewById<Button>(R.id.btnVoltarMenu)
        val voltarEscopo = findViewById<ImageButton>(R.id.voltarEscopo)
        val textViewDetalhes = findViewById<TextView>(R.id.textViewDetalhes)
        val editBtn: ImageButton = findViewById(R.id.editBtn)
        val pdfDownloadButton: Button = findViewById(R.id.btnDownloadPdf)

        var escopoId = intent.getStringExtra("escopoId") ?: ""
        var numeroEscopo = intent.getStringExtra("numeroEscopo") ?: "N/A"
        var empresa = intent.getStringExtra("empresa") ?: "N/A"
        var dataEstimativa = intent.getStringExtra("dataEstimativa") ?: "N/A"
        var tipoServico = intent.getStringExtra("tipoServico") ?: "N/A"
        var status = intent.getStringExtra("status") ?: "N/A"
        var resumoEscopo = intent.getStringExtra("resumoEscopo") ?: "N/A"
        var numeroPedidoCompra = intent.getStringExtra("numeroPedidoCompra") ?: "N/A"
        val pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        var criadorNome = intent.getStringExtra("criadorNome") ?: "N/A"
        var dataCriacao = intent.getStringExtra("dataCriacao") ?: "N/A"

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

        // Botão para editar escopo
        editBtn.setOnClickListener {
            val intent = Intent(this, EditarEscopoActivity::class.java).apply {
                putExtra("editMode", true)
                putExtra("escopoId", escopoId)
                putExtra("numeroEscopo", numeroEscopo)
                putExtra("empresa", empresa)
                putExtra("dataEstimativa", dataEstimativa)
                putExtra("tipoServico", tipoServico)
                putExtra("status", status)
                putExtra("resumoEscopo", resumoEscopo)
                putExtra("numeroPedidoCompra", numeroPedidoCompra)
                putExtra("pdfUrl", pdfUrl)
            }
            startActivityForResult(intent, 100) // Código de requisição 100
        }

        // Botão para abrir PDF
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

    private fun abrirPdf(pdfUrl: String) {
        try {
            if (pdfUrl.isEmpty()) {
                Toast.makeText(this, "PDF não disponível para visualização.", Toast.LENGTH_SHORT).show()
                return
            }

            // Cria uma Uri a partir da URL do PDF
            val uri = Uri.parse(pdfUrl)

            // Cria a Intent para abrir o PDF com um visualizador padrão de PDF (sem download)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Verifica se há algum aplicativo que pode abrir o PDF diretamente
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Se não houver visualizador de PDF disponível, abre no navegador como fallback
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("DetalhesEscopo", "Erro ao tentar abrir o PDF: ${e.message}")
            Toast.makeText(this, "Erro ao tentar abrir o PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para verificar e solicitar permissões para acessar arquivos
    private fun checkAndRequestPermissions(): Boolean {
        val currentApiVersion = Build.VERSION.SDK_INT
        if (currentApiVersion >= Build.VERSION_CODES.Q) { // Android 10 e superior
            // Usar o Storage Access Framework (SAF) para abrir o arquivo
            openFilePicker()
            return true
        } else {
            val readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // Se alguma das permissões não estiver concedida, solicita
            if (readStoragePermission != PackageManager.PERMISSION_GRANTED || writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
                // Verifica se o usuário já negou anteriormente
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    Toast.makeText(this, "Você precisa permitir o acesso ao armazenamento para visualizar o PDF.", Toast.LENGTH_LONG).show()
                }

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
            return true
        }
    }

    // Função para lidar com a resposta das permissões solicitadas
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissões concedidas
                val pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
                if (pdfUrl.isNotEmpty()) {
                    abrirPdf(pdfUrl)
                }
            } else {
                Toast.makeText(this, "Permissão negada para acessar arquivos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para abrir o seletor de arquivos (Storage Access Framework - SAF)
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_PDF_REQUEST_CODE)
    }

    // Função para lidar com o resultado do seletor de arquivos
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

        // Atualizando os dados após editar
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val escopoId = data?.getStringExtra("escopoId") ?: ""
            val numeroEscopo = data?.getStringExtra("numeroEscopo") ?: "N/A"
            val empresa = data?.getStringExtra("empresa") ?: "N/A"
            val dataEstimativa = data?.getStringExtra("dataEstimativa") ?: "N/A"
            val tipoServico = data?.getStringExtra("tipoServico") ?: "N/A"
            val status = data?.getStringExtra("status") ?: "N/A"
            val resumoEscopo = data?.getStringExtra("resumoEscopo") ?: "N/A"
            val numeroPedidoCompra = data?.getStringExtra("numeroPedidoCompra") ?: "N/A"
            val pdfUrl = data?.getStringExtra("pdfUrl") ?: ""

            val textViewDetalhes = findViewById<TextView>(R.id.textViewDetalhes)
            textViewDetalhes.text = """
                Número: $numeroEscopo
                Empresa: $empresa
                Data Estimada: $dataEstimativa
                Tipo de Serviço: $tipoServico
                Status: $status
                Resumo: $resumoEscopo
                Número do Pedido de Compra: $numeroPedidoCompra
            """.trimIndent()
        }
    }
}
