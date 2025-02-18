package com.amjsecurityfire.amjsecurityfire;

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurityfire.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import android.widget.EditText
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp

public class AdicionarEscopoActivity : AppCompatActivity(){
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBarContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private var ultimoNumeroEscopo: Int = 0
    private var pdfUri: Uri? = null
    private val PDF_REQUEST_CODE = 100

    private lateinit var salvarButton: Button
    private lateinit var cancelarButton: Button
    private lateinit var attachPdfButton: Button
    private lateinit var pdfStatusTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_escopo)

        db = FirebaseFirestore.getInstance()

        // Inicializar o Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Inicializar ProgressBar e seu container
        progressBarContainer = findViewById(R.id.progressBarContainer)
        progressBar = findViewById(R.id.progressBar)

        // Esconder a ProgressBar inicialmente
        progressBarContainer.visibility = View.GONE
        progressBar.visibility = View.GONE

        // Recuperar dados do Intent
        val editMode = intent.getBooleanExtra("editMode", false)
        val escopoId = intent.getStringExtra("escopoId")
        val empresaEdit = intent.getStringExtra("empresa") ?: ""
        val numeroEscopoEdit = intent.getStringExtra("numeroEscopo")
        val dataEstimativaEdit = intent.getStringExtra("dataEstimativa") ?: ""
        val tipoServicoEdit = intent.getStringExtra("tipoServico") ?: ""
        val statusEdit = intent.getStringExtra("status") ?: ""
        val resumoEscopoEdit = intent.getStringExtra("resumoEscopo") ?: ""
        val numeroPedidoCompraEdit = intent.getStringExtra("numeroPedidoCompra") ?: ""

        // Referenciar os campos do layout
        val empresaField = findViewById<EditText>(R.id.editTextText3)
                val tipoServicoSpinner = findViewById<Spinner>(R.id.spinnerTipoManutencao)
                val statusSpinner = findViewById<Spinner>(R.id.spinnerTipoManutencao2)
                val resumoField = findViewById<EditText>(R.id.textInputEditText)
                val numeroPedidoField = findViewById<EditText>(R.id.editTextNumber2)

                salvarButton = findViewById(R.id.button3)
        cancelarButton = findViewById(R.id.button5)
        attachPdfButton = findViewById(R.id.buttonAttachPdf)
        pdfStatusTextView = findViewById(R.id.textViewPdfStatus)


        // Dados para os Spinners
        val tiposManutencao = listOf("Preventiva", "Corretiva", "Obra")
        val statusManutencao = listOf("Pendente", "Em Andamento", "Concluído")

        // Configurar o DatePicker

        val dataEstimativaField = findViewById<EditText>(R.id.editTextDate)
                val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

// Forçar o Locale para português
        val locale = Locale("pt", "BR")
        Locale.setDefault(locale)

// Atualizar as configurações de recursos com a localidade
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        dataEstimativaField.setOnClickListener {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"))
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                    this,
                    R.style.DatePickerCustomStyle,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                            // Configura o calendário com a data selecionada
                            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)

                            // Ajusta o fuso horário para Brasília
                            calendar.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")

                            // Formatar a data no formato dd/MM/yy
                            val sdf = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
                            val formattedDate = sdf.format(calendar.time)

                            dataEstimativaField.setText(formattedDate)
                    },
                    year, month, dayOfMonth
            )
            datePickerDialog.show()
        }



        // Configurar Spinners
        setupSpinner(tipoServicoSpinner, tiposManutencao, tipoServicoEdit)
        setupSpinner(statusSpinner, statusManutencao, statusEdit)

        // Preencher campos no modo de edição
        if (editMode) {
            empresaField.setText(empresaEdit)
            dataEstimativaField.setText(dataEstimativaEdit)
            resumoField.setText(resumoEscopoEdit)
            numeroPedidoField.setText(numeroPedidoCompraEdit)
        } else {
            buscarUltimoNumeroEscopo(statusSpinner.selectedItem.toString())
        }

        // Atualizar último número de escopo ao mudar o status
        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                buscarUltimoNumeroEscopo(statusSpinner.selectedItem.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Botão de anexar PDF com ActivityResultContracts
        val getPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                pdfUri = uri
                val fileName = uri.lastPathSegment ?: "PDF selecionado"
                pdfStatusTextView.text = fileName
                pdfStatusTextView.setTextColor(getColor(R.color.teal_700))
            }
        }

        attachPdfButton.setOnClickListener {
            getPdfLauncher.launch("application/pdf")
        }

        // Botão de salvar
        salvarButton.setOnClickListener {
            // Mostrar ProgressBar e a tela embaçada
            toggleProgress(true)

            val empresa = empresaField.text.toString().trim()
            val dataEstimativa = dataEstimativaField.text.toString().trim()
            val tipoServico = tipoServicoSpinner.selectedItem.toString()
            val status = statusSpinner.selectedItem.toString()
            val resumo = resumoField.text.toString().trim()
            val numeroPedidoCompra = numeroPedidoField.text.toString().trim()

            // Verificar campos obrigatórios
            if (empresa.isEmpty() || dataEstimativa.isEmpty() || resumo.isEmpty() || numeroPedidoCompra.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()

                // Esconder ProgressBar e a tela embaçada
                toggleProgress(false)
                return@setOnClickListener
            }

            // Verificar se o PDF foi anexado
            if (pdfUri == null) {
                Toast.makeText(this, "Por favor, anexe um arquivo PDF antes de salvar.", Toast.LENGTH_SHORT).show()

                // Esconder ProgressBar e a tela embaçada
                toggleProgress(false)
                return@setOnClickListener
            }

            // Verificar conexão com a internet
            if (!isInternetAvailable()) {
                Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_SHORT).show()

                // Esconder ProgressBar e a tela embaçada
                toggleProgress(false)
                return@setOnClickListener
            }

            // Iniciar o upload do PDF
            uploadPdfToStorage(
                    onSuccess = { pdfDownloadUrl ->
                            val finalPdfDownloadUrl = pdfDownloadUrl ?: ""

            // Determinar o número do escopo
            val numeroEscopoAtual = if (editMode && numeroEscopoEdit != null) {
                numeroEscopoEdit.toInt()
            } else {
                ultimoNumeroEscopo + 1
            }

            // Criar o mapa de dados para o Firestore
            val novoEscopo = mapOf(
                    "numeroEscopo" to numeroEscopoAtual,
                    "empresa" to empresa,
                    "dataEstimativa" to dataEstimativa,
                    "tipoServico" to tipoServico,
                    "status" to status,
                    "resumoEscopo" to resumo,
                    "numeroPedidoCompra" to numeroPedidoCompra,
                    "pdfUrl" to finalPdfDownloadUrl
            )

            // Salvar no Firestore
            salvarNoFirestore(status, novoEscopo, editMode, escopoId)

            // Esconder ProgressBar e a tela embaçada
            toggleProgress(false)
                },
            onFailure = { exception ->
                    Toast.makeText(this, "Erro ao fazer upload do PDF: ${exception.message}", Toast.LENGTH_SHORT).show()

                    // Esconder ProgressBar e a tela embaçada
                    toggleProgress(false)
            }
            )
        }


        // Botão de cancelar
        cancelarButton.setOnClickListener {
            finish()
        }
    }

    private fun toggleProgress(isLoading: Boolean) {
        if (isLoading) {
            progressBarContainer.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            progressBarContainer.visibility = View.GONE
            progressBar.visibility = View.GONE
        }

        // Desabilitar interação com os botões e outros elementos enquanto carrega
        salvarButton.isEnabled = !isLoading
        cancelarButton.isEnabled = !isLoading
        attachPdfButton.isEnabled = !isLoading
        findViewById<EditText>(R.id.editTextText3).isEnabled = !isLoading
        findViewById<EditText>(R.id.textInputEditText).isEnabled = !isLoading
        findViewById<EditText>(R.id.editTextNumber2).isEnabled = !isLoading
        findViewById<EditText>(R.id.editTextDate).isEnabled = !isLoading
        findViewById<Spinner>(R.id.spinnerTipoManutencao).isEnabled = !isLoading
        findViewById<Spinner>(R.id.spinnerTipoManutencao2).isEnabled = !isLoading
    }

    private fun salvarNoFirestore(status: String, novoEscopo: Map<String, Any>, editMode: Boolean, escopoId: String?) {
        val escoposCollection = if (status == "Concluído") {
            db.collection("escoposConcluidos")
        } else {
            db.collection("escoposPendentes")
        }

        // Obter o usuário autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        val usuarioNome = currentUser?.displayName ?: "Usuário Desconhecido" // Caso o usuário não tenha nome configurado

        // Definir o fuso horário para horário de Brasília
        val timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val calendar = Calendar.getInstance(timeZone)
        val dataCriacao = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)

        // Se editMode é verdadeiro, edita o escopo existente, caso contrário cria um novo escopo
        if (editMode && escopoId != null) {
            escoposCollection.document(escopoId)
                    .update(novoEscopo)
                    .addOnSuccessListener {
                Toast.makeText(this, "Escopo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                registrarHistoricoEscopo(escopoId, usuarioNome, "Editado", dataCriacao) // Registrar histórico de edição
                finish()
            }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar o escopo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            escoposCollection.add(novoEscopo)
                    .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Escopo salvo com sucesso!", Toast.LENGTH_SHORT).show()
                registrarHistoricoEscopo(documentReference.id, usuarioNome, "Criado", dataCriacao) // Registrar histórico de criação
                finish()
            }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao salvar o escopo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Função para registrar histórico de escopo
    private fun registrarHistoricoEscopo(escopoId: String, usuario: String, acao: String, data: String) {
        val historico = mapOf(
                "escopoId" to escopoId,
                "usuario" to usuario,
                "acao" to acao,
                "data" to data
        )

        db.collection("historicoEscopos")
                .add(historico)
                .addOnSuccessListener {
            Log.d("Historico", "Histórico registrado com sucesso.")
        }
            .addOnFailureListener { e ->
                Log.w("Historico", "Erro ao registrar o histórico: ${e.message}")
        }
    }


    private fun uploadPdfToStorage(onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        if (pdfUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val pdfRef = storageRef.child("pdfs/${System.currentTimeMillis()}.pdf")

            pdfRef.putFile(pdfUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    pdfRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
            }
            }
                .addOnFailureListener { exception ->
                    onFailure(exception)
            }
        } else {
            onSuccess(null)
        }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>, selectedItem: String) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val selectedPosition = items.indexOf(selectedItem)
        if (selectedPosition != -1) {
            spinner.setSelection(selectedPosition)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun buscarUltimoNumeroEscopo(status: String) {
        val collection = if (status == "Concluído") {
            db.collection("escoposConcluidos")
        } else {
            db.collection("escoposPendentes")
        }

        collection.orderBy("numeroEscopo", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val ultimoEscopo = querySnapshot.documents[0]
                ultimoNumeroEscopo = (ultimoEscopo.getLong("numeroEscopo") ?: 0).toInt()
            } else {
                ultimoNumeroEscopo = 0
            }
        }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar o último número de escopo: ${exception.message}")
        }
    }
    private fun voltarParaLista(status: String) {
        val intent = if (status == "Concluído") {
            Intent(this, EscoposConcluidosActivity::class.java)
        } else {
            Intent(this, EscoposPendentesActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
