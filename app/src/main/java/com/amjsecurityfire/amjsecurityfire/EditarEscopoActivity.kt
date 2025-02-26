package com.amjsecurityfire.amjsecurityfire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurityfire.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class EditarEscopoActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_escopo)

        db = FirebaseFirestore.getInstance()

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

        // Configurar o Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposServicos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoServicoSpinner.adapter = adapter

        // Função para carregar os dados mais recentes do Firestore
        fun carregarDadosDoFirestore() {
            // Carregar dados de escoposPendentes
            db.collection("escoposPendentes").document(escopoId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val empresa = document.getString("empresa") ?: ""
                        val dataEstimativa = document.getString("dataEstimativa") ?: ""
                        val resumoEscopo = document.getString("resumoEscopo") ?: ""
                        val tipoServico = document.getString("tipoServico") ?: ""
                        val numeroPedidoCompra = document.getString("numeroPedidoCompra") ?: ""

                        // Preencher os campos com os dados do Firestore
                        empresaEditText.setText(empresa)
                        dataEstimativaEditText.setText(dataEstimativa)
                        resumoEditText.setText(resumoEscopo)
                        numeroPedidoCompraEditText.setText(numeroPedidoCompra)

                        // Configurar o Spinner para selecionar o valor correto
                        val tipoServicoIndex = tiposServicos.indexOf(tipoServico)
                        if (tipoServicoIndex != -1) {
                            tipoServicoSpinner.setSelection(tipoServicoIndex)
                        }
                    } else {
                        // Tentar carregar dados de escoposConcluidos se não encontrado em escoposPendentes
                        db.collection("escoposConcluidos").document(escopoId).get()
                            .addOnSuccessListener { documentConcluido ->
                                if (documentConcluido.exists()) {
                                    val empresa = documentConcluido.getString("empresa") ?: ""
                                    val dataEstimativa = documentConcluido.getString("dataEstimativa") ?: ""
                                    val resumoEscopo = documentConcluido.getString("resumoEscopo") ?: ""
                                    val tipoServico = documentConcluido.getString("tipoServico") ?: ""
                                    val numeroPedidoCompra = documentConcluido.getString("numeroPedidoCompra") ?: ""

                                    // Preencher os campos com os dados de escoposConcluidos
                                    empresaEditText.setText(empresa)
                                    dataEstimativaEditText.setText(dataEstimativa)
                                    resumoEditText.setText(resumoEscopo)
                                    numeroPedidoCompraEditText.setText(numeroPedidoCompra)

                                    // Configurar o Spinner para selecionar o valor correto
                                    val tipoServicoIndex = tiposServicos.indexOf(tipoServico)
                                    if (tipoServicoIndex != -1) {
                                        tipoServicoSpinner.setSelection(tipoServicoIndex)
                                    }
                                } else {
                                    Toast.makeText(this, "Erro: Documento não encontrado nas duas coleções!", Toast.LENGTH_SHORT).show()
                                    finish() // Aqui, se o documento não existir, fechamos a activity
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

        // Carrega os dados ao abrir a tela
        carregarDadosDoFirestore()

        salvarButton.setOnClickListener {
            val dadosAtualizados = hashMapOf(
                "empresa" to empresaEditText.text.toString(),
                "dataEstimativa" to dataEstimativaEditText.text.toString(),
                "resumoEscopo" to resumoEditText.text.toString(),
                "tipoServico" to tipoServicoSpinner.selectedItem.toString(),
                "numeroPedidoCompra" to numeroPedidoCompraEditText.text.toString()
            )

            // Atualizar escopo nas coleções escoposPendentes e escoposConcluidos
            db.collection("escoposPendentes").document(escopoId)
                .set(dadosAtualizados, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("EditarEscopo", "Escopo em Pendentes atualizado com sucesso: $dadosAtualizados")

                    db.collection("escoposConcluidos").document(escopoId)
                        .set(dadosAtualizados, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("EditarEscopo", "Escopo em Concluídos atualizado com sucesso: $dadosAtualizados")
                            Toast.makeText(this, "Escopo atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                            // Obter o nome do usuário logado
                            val usuario = FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email ?: "Usuário desconhecido"

                            // Adicionar log para verificar o valor do nome de usuário
                            Log.d("EditarEscopo", "Usuário logado: $usuario")

                            // Obter a data sem a hora (apenas a data)
                            val dataAtual = System.currentTimeMillis()
                            val dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataAtual).toString()
                            val numeroEscopo = intent.getStringExtra("numeroEscopo")?.toLongOrNull() ?: 0L

                            // Gravar o histórico de edição
                            val historicoDados = hashMapOf(
                                "ação" to "Edição",
                                "data" to dataFormatada,  // A data agora sem a hora
                                "número do Escopo" to numeroEscopo,  // Número do escopo como número
                                "usuário" to usuario  // Nome do usuário logado
                            )

                            // Gravar no histórico
                            db.collection("historicoEscopos").add(historicoDados)
                                .addOnSuccessListener {
                                    Log.d("HistoricoEscopo", "Histórico de edição registrado com sucesso!")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HistoricoEscopo", "Erro ao registrar histórico de edição", e)
                                }

                            // Enviar os dados atualizados para a DetalhesEscopoActivity
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
                            finish() // Volta para a tela anterior (DetalhesEscopoActivity)
                        }
                        .addOnFailureListener { e ->
                            Log.e("EditarEscopo", "Erro ao atualizar escopo em Concluídos", e)
                            Toast.makeText(this, "Erro ao atualizar escopo em Concluídos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("EditarEscopo", "Erro ao atualizar escopo em Pendentes", e)
                    Toast.makeText(this, "Erro ao atualizar escopo em Pendentes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelarButton.setOnClickListener {
            finish() // Volta à tela anterior (DetalhesEscopoActivity)
        }
    }
}
