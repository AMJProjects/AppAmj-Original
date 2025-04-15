package com.amjsecurityfire.amjsecurity;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
import android.database.Cursor
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.EditText
import androidx.core.content.ContextCompat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdicionarEscopoActivity : AppCompatActivity(){
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBarContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private var ultimoNumeroEscopo: Int = 0
    private lateinit var usuarioNome: String
    private var pdfUri: Uri? = null

    private lateinit var salvarButton: Button
    private lateinit var cancelarButton: Button
    private lateinit var attachPdfButton: Button
    private lateinit var pdfStatusTextView: TextView

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

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId")

            databaseReference.child("nome").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    usuarioNome = snapshot.getValue(String::class.java) ?: "Usuário Desconhecido"
                }

                override fun onCancelled(error: DatabaseError) {
                    usuarioNome = "Usuário Desconhecido"
                }
            })
        } else {
            usuarioNome = "Usuário Desconhecido"
        }

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

        // Obter o usuário autenticado
        val usuarioNome = currentUser?.displayName ?: "Usuário Desconhecido"

        // Formatar a data no formato "dd/MM/yyyy"
        val timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val calendario = Calendar.getInstance(timeZone)
        val dataCriacao = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendario.time)

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
        val getPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    // Com MediaStore, você já tem a permissão para acessar o arquivo
                    if (isPdfFile(it)) {
                        pdfUri = it
                        val fileName = getFileNameFromUri(it)
                        pdfStatusTextView.text = fileName
                        pdfStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
                    } else {
                        Toast.makeText(this, "Selecione um arquivo PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        attachPdfButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            pickPdfLauncher.launch(intent)
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
                toggleProgress(false)
                return@setOnClickListener
            }

            // Verificar se o PDF foi anexado
            if (pdfUri == null) {
                Toast.makeText(this, "Por favor, anexe um arquivo PDF antes de salvar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar conexão com a internet
            if (!isInternetAvailable()) {
                Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_SHORT).show()
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

                    // Obter o usuário autenticado
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.let {
                        // Buscar nome do usuário no Realtime Database
                        buscarNomeUsuario(it.uid) { usuarioNome ->
                            // Criar o mapa de dados para o Firestore
                            val novoEscopo = mapOf(
                                "numeroEscopo" to numeroEscopoAtual,
                                "empresa" to empresa,
                                "dataEstimativa" to dataEstimativa,
                                "tipoServico" to tipoServico,
                                "status" to status,
                                "resumoEscopo" to resumo,
                                "numeroPedidoCompra" to numeroPedidoCompra,
                                "pdfUrl" to finalPdfDownloadUrl,
                                "criadorNome" to usuarioNome,
                                "dataCriacao" to dataCriacao
                            )

                            // Salvar no Firestore
                            salvarNoFirestore(status, novoEscopo, editMode, escopoId)

                            // Esconder ProgressBar e a tela embaçada
                            toggleProgress(false)
                        }
                    } ?: run {
                        Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                        toggleProgress(false)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(this, "Erro ao fazer upload do PDF: ${exception.message}", Toast.LENGTH_SHORT).show()
                    toggleProgress(false)
                }
            )
        }

        // Botão de cancelar
        cancelarButton.setOnClickListener {
            finish()
        }
    }

    private fun isPdfFile(uri: Uri): Boolean {
        return contentResolver.getType(uri) == "application/pdf"
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    it.getString(columnIndex) ?: "Arquivo PDF"
                } else {
                    "Arquivo PDF"
                }
            } ?: "Arquivo PDF"
        } catch (e: Exception) {
            "Arquivo PDF"
        } finally {
            cursor?.close()
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

    // Dentro da classe AdicionarEscopoActivity

    private fun salvarNoFirestore(status: String, novoEscopo: Map<String, Any>, editMode: Boolean, escopoId: String?) {
        val escoposCollection = if (status == "Concluído") {
            db.collection("escoposConcluidos")
        } else {
            db.collection("escoposPendentes")
        }

        // Formatar data atual para o padrão dd/MM/yy
        val timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val calendar = Calendar.getInstance(timeZone)
        val sdf = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
        val dataAcao = sdf.format(calendar.time)

        // Obter número do escopo e ação
        val numeroEscopo = novoEscopo["numeroEscopo"] as Int
        val acao = if (editMode) "edição" else "criação"

        // Salvar/Atualizar escopo principal
        if (editMode && escopoId != null) {
            escoposCollection.document(escopoId)
                .update(novoEscopo)
                .addOnSuccessListener {
                    registrarHistoricoEscopo(numeroEscopo, usuarioNome, acao, dataAcao, status) // Passar o status
                    Toast.makeText(this, "Escopo atualizado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            escoposCollection.add(novoEscopo)
                .addOnSuccessListener {
                    registrarHistoricoEscopo(numeroEscopo, usuarioNome, acao, dataAcao, status) // Passar o status
                    Toast.makeText(this, "Escopo criado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao criar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Função para registrar histórico atualizada
    private fun registrarHistoricoEscopo(
        numeroEscopo: Int,
        usuario: String,
        acao: String,
        data: String,
        status: String // Adicionar o parâmetro status
    ) {
        val historico = hashMapOf(
            "número do escopo" to numeroEscopo,
            "ação" to acao,
            "usuário" to usuario,
            "data" to data,
            "status" to status // Incluir o status no mapa
        )

        db.collection("historicoEscopos")
            .add(historico)
            .addOnSuccessListener {
                Log.d("HISTORICO", "Registro histórico salvo")
            }
            .addOnFailureListener { e ->
                Log.e("HISTORICO", "Erro ao salvar histórico", e)
            }
    }

    private fun uploadPdfToStorage(onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        pdfUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference
            val pdfRef = storageRef.child("pdfs/${System.currentTimeMillis()}.pdf")

            try {
                val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Não foi possível abrir o arquivo")
                val uploadTask = pdfRef.putStream(inputStream)

                uploadTask.addOnSuccessListener {
                    pdfRef.downloadUrl.addOnSuccessListener { url ->
                        onSuccess(url.toString())
                    }
                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        } ?: onFailure(Exception("Arquivo PDF não selecionado"))
    }

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (isPdfFile(uri)) {
                    pdfUri = uri
                    pdfStatusTextView.text = getFileNameFromUri(uri)
                    pdfStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
                } else {
                    Toast.makeText(this, "Selecione um arquivo PDF válido.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickPdfLauncher.launch(intent)
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

    private fun buscarNomeUsuario(uid: String, onComplete: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("users") // Ajuste o caminho conforme a estrutura do seu Realtime Database
        database.child(uid).child("nome").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nome = snapshot.getValue(String::class.java) ?: "Usuário Desconhecido"
                onComplete(nome)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao buscar nome do usuário: ${error.message}")
                onComplete("Usuário Desconhecido")
            }
        })
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
