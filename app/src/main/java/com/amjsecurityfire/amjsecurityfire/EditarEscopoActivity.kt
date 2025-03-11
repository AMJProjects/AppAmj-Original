package com.amjsecurityfire.amjsecurityfire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditarEscopoActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    // 1) Variável para guardar o nome do usuário obtido do Realtime Database
    private lateinit var usuarioNome: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_escopo)

        db = FirebaseFirestore.getInstance()

        // -- Início da lógica para buscar o nome do usuário do Realtime Database --
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId")

            // Aqui buscamos o campo "nome" dentro de "users/<uid>/nome"
            databaseReference.child("nome").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Se existir, usamos o valor. Senão, "Usuário Desconhecido"
                    usuarioNome = snapshot.getValue(String::class.java) ?: "Usuário Desconhecido"
                    Log.d("EditarEscopo", "Nome do usuário carregado do Realtime Database: $usuarioNome")
                }
                override fun onCancelled(error: DatabaseError) {
                    usuarioNome = "Usuário Desconhecido"
                    Log.e("EditarEscopo", "Erro ao buscar nome do usuário: ${error.message}")
                }
            })
        } else {
            // Se não estiver logado, usar "Usuário Desconhecido"
            usuarioNome = "Usuário Desconhecido"
        }
        // -- Fim da lógica para buscar o nome do usuário do Realtime Database --

        // Recebe o ID do escopo pela Intent
        val escopoId = intent.getStringExtra("escopoId")

        // Verifica se o escopoId é nulo ou vazio
        if (escopoId.isNullOrEmpty()) {
            Toast.makeText(this, "Erro: ID do escopo não encontrado!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Referências para os campos de entrada
        val empresaEditText = findViewById<EditText>(R.id.editTextText3)
        val dataEstimativaEditText = findViewById<EditText>(R.id.editTextDate)
        val resumoEditText = findViewById<EditText>(R.id.textInputEditText)
        val numeroPedidoCompraEditText = findViewById<EditText>(R.id.editTextNumber2)
        val tipoServicoSpinner = findViewById<Spinner>(R.id.spinnerTipoManutencao)
        val salvarButton = findViewById<Button>(R.id.button3)
        val cancelarButton = findViewById<Button>(R.id.button5)

        // Dados para o Spinner
        val tiposServicos = listOf("Preventiva", "Corretiva", "Obra")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposServicos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoServicoSpinner.adapter = adapter

        // Função para carregar os dados do Firestore
        fun carregarDadosDoFirestore() {
            // Tenta primeiro em escoposPendentes
            db.collection("escoposPendentes").document(escopoId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val empresa = document.getString("empresa") ?: ""
                        val dataEstimativa = document.getString("dataEstimativa") ?: ""
                        val resumoEscopo = document.getString("resumoEscopo") ?: ""
                        val tipoServico = document.getString("tipoServico") ?: ""
                        val numeroPedidoCompra = document.getString("numeroPedidoCompra") ?: ""
                        val status = document.getString("status") ?: "Pendente" // Recupera o status

                        empresaEditText.setText(empresa)
                        dataEstimativaEditText.setText(dataEstimativa)
                        resumoEditText.setText(resumoEscopo)
                        numeroPedidoCompraEditText.setText(numeroPedidoCompra)

                        val tipoServicoIndex = tiposServicos.indexOf(tipoServico)
                        if (tipoServicoIndex != -1) {
                            tipoServicoSpinner.setSelection(tipoServicoIndex)
                        }
                    } else {
                        // Se não achar, tenta em escoposConcluidos
                        db.collection("escoposConcluidos").document(escopoId).get()
                            .addOnSuccessListener { docConcluido ->
                                if (docConcluido.exists()) {
                                    val empresa = docConcluido.getString("empresa") ?: ""
                                    val dataEstimativa = docConcluido.getString("dataEstimativa") ?: ""
                                    val resumoEscopo = docConcluido.getString("resumoEscopo") ?: ""
                                    val tipoServico = docConcluido.getString("tipoServico") ?: ""
                                    val numeroPedidoCompra = docConcluido.getString("numeroPedidoCompra") ?: ""
                                    val status = docConcluido.getString("status") ?: "Concluído" // Recupera o status

                                    empresaEditText.setText(empresa)
                                    dataEstimativaEditText.setText(dataEstimativa)
                                    resumoEditText.setText(resumoEscopo)
                                    numeroPedidoCompraEditText.setText(numeroPedidoCompra)

                                    val tipoServicoIndex = tiposServicos.indexOf(tipoServico)
                                    if (tipoServicoIndex != -1) {
                                        tipoServicoSpinner.setSelection(tipoServicoIndex)
                                    }
                                } else {
                                    Toast.makeText(this, "Erro: Documento não encontrado nas duas coleções!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Carrega os dados
        carregarDadosDoFirestore()

        // Botão salvar
        salvarButton.setOnClickListener {
            val dadosAtualizados = hashMapOf(
                "empresa" to empresaEditText.text.toString(),
                "dataEstimativa" to dataEstimativaEditText.text.toString(),
                "resumoEscopo" to resumoEditText.text.toString(),
                "tipoServico" to tipoServicoSpinner.selectedItem.toString(),
                "numeroPedidoCompra" to numeroPedidoCompraEditText.text.toString()
            )

            // Atualiza nas coleções
            db.collection("escoposPendentes").document(escopoId)
                .set(dadosAtualizados, SetOptions.merge())
                .addOnSuccessListener {
                    db.collection("escoposConcluidos").document(escopoId)
                        .set(dadosAtualizados, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Escopo atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                            // Registrar histórico
                            val dataAtual = System.currentTimeMillis()
                            val dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataAtual).toString()
                            val dataFormatadaAjustada = dataFormatada.replace("2025", "25")

                            val numeroEscopo = intent.getStringExtra("numeroEscopo")?.toLongOrNull() ?: 0L
                            val status = intent.getStringExtra("status") ?: "Pendente" // Recupera o status da Intent

                            // 2) Ao salvar no histórico, use a variável 'usuarioNome' e inclua o status
                            val historicoDados = hashMapOf(
                                "ação" to "Edição",
                                "data" to dataFormatadaAjustada,
                                "número do escopo" to numeroEscopo,
                                "usuário" to usuarioNome, // Aqui usamos o valor vindo do Realtime DB
                                "status" to status // Inclui o status no histórico
                            )

                            db.collection("historicoEscopos").add(historicoDados)
                                .addOnSuccessListener {
                                    Log.d("EditarEscopo", "Histórico de edição registrado com sucesso! (usuário=$usuarioNome, status=$status)")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EditarEscopo", "Erro ao registrar histórico de edição: ${e.message}")
                                }

                            // Retornar para a tela anterior com os dados atualizados
                            val resultIntent = Intent()
                            resultIntent.putExtra("escopoId", escopoId)
                            resultIntent.putExtra("numeroEscopo", intent.getStringExtra("numeroEscopo"))
                            resultIntent.putExtra("status", intent.getStringExtra("status"))
                            resultIntent.putExtra("empresa", empresaEditText.text.toString())
                            resultIntent.putExtra("dataEstimativa", dataEstimativaEditText.text.toString())
                            resultIntent.putExtra("resumoEscopo", resumoEditText.text.toString())
                            resultIntent.putExtra("tipoServico", tipoServicoSpinner.selectedItem.toString())
                            resultIntent.putExtra("numeroPedidoCompra", numeroPedidoCompraEditText.text.toString())

                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao atualizar escopo em Concluídos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar escopo em Pendentes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelarButton.setOnClickListener {
            finish()
        }
    }
}