package com.amjsecurityfire.amjsecurityfire

import android.annotation.SuppressLint
import android.content.Intent
import kotlinx.coroutines.*
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EscoposExcluidosActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var containerExcluidos: LinearLayout
    private lateinit var buttonVoltarMenu: Button
    private lateinit var progressBarContainer: FrameLayout
    private val escoposList = mutableListOf<Map<String, String>>()
    private var nomeUsuario: String = "Desconhecido"


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.escopos_excluidos)

        db = FirebaseFirestore.getInstance()
        containerExcluidos = findViewById(R.id.layoutDinamico)
        buttonVoltarMenu = findViewById(R.id.btnVoltarMenu)
        progressBarContainer = findViewById(R.id.progressBarContainer)

        buttonVoltarMenu.setOnClickListener {
            finish()
        }
        carregarNomeUsuario()
        carregarEscoposExcluidos()
    }

    private fun carregarNomeUsuario() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    nomeUsuario = snapshot.child("nome").getValue(String::class.java) ?: "Desconhecido"
                    carregarEscoposExcluidos()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar nome do usuário", Toast.LENGTH_SHORT).show()
                    carregarEscoposExcluidos()
                }
            })
        } else {
            carregarEscoposExcluidos()
        }
    }

    private fun carregarEscoposExcluidos() {
        progressBarContainer.visibility = View.VISIBLE // Mostra a ProgressBar

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshots = db.collection("escoposExcluidos")
                    .orderBy("numeroEscopo", Query.Direction.ASCENDING)
                    .get()
                    .await()

                withContext(Dispatchers.Main) {
                    escoposList.clear()
                    containerExcluidos.removeAllViews()

                    for (document in snapshots) {
                        val numeroEscopo = when (val numero = document.get("numeroEscopo")) {
                            is String -> numero
                            is Long -> numero.toString()
                            else -> ""
                        }

                        val escopo = mapOf(
                            "numeroEscopo" to numeroEscopo,
                            "tipoServico" to document.getString("tipoServico").orEmpty(),
                            "empresa" to document.getString("empresa").orEmpty(),
                            "dataEstimativa" to document.getString("dataEstimativa").orEmpty(),
                            "status" to document.getString("status").orEmpty(),
                            "resumoEscopo" to document.getString("resumoEscopo").orEmpty(),
                            "numeroPedidoCompra" to document.getString("numeroPedidoCompra").orEmpty(),
                            "motivoExclusao" to document.getString("motivoExclusao").orEmpty(),
                            "excluidoPor" to (document.getString("excluidoPor") ?: nomeUsuario),
                            "escopoId" to document.id
                        )

                        escoposList.add(escopo)
                        adicionarTextoDinamico(escopo)
                    }

                    progressBarContainer.visibility = View.GONE // Esconde a ProgressBar
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarContainer.visibility = View.GONE // Esconde a ProgressBar em caso de erro
                    Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar escopos.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun adicionarTextoDinamico(escopo: Map<String, String>) {
        val layoutEscopo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.botaoredondo)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        val textoEscopo = """
        Número: ${escopo["numeroEscopo"]}
        Empresa: ${escopo["empresa"]}
        Data Estimada: ${escopo["dataEstimativa"]}
        Status: ${escopo["status"]}
        Motivo da Exclusão: ${escopo["motivoExclusao"]}
        Excluído Por: ${escopo["excluidoPor"]}
        """.trimIndent()

        val textView = TextView(this).apply {
            text = textoEscopo
            textSize = 16f
        }

        val buttonVisualizar = Button(this).apply {
            text = "Visualizar Detalhes"
            setOnClickListener {
                val intent = Intent(this@EscoposExcluidosActivity, DetalhesEscopoActivity::class.java).apply {
                    putExtra("numeroEscopo", escopo["numeroEscopo"])
                    putExtra("empresa", escopo["empresa"])
                    putExtra("dataEstimativa", escopo["dataEstimativa"])
                    putExtra("tipoServico", escopo["tipoServico"])
                    putExtra("status", escopo["status"])
                    putExtra("resumoEscopo", escopo["resumoEscopo"])
                    putExtra("numeroPedidoCompra", escopo["numeroPedidoCompra"])
                    putExtra("motivoExclusao", escopo["motivoExclusao"])
                    putExtra("excluidoPor", escopo["excluidoPor"])
                    putExtra("escopoId", escopo["escopoId"])
                }
                startActivity(intent)
            }
        }

        layoutEscopo.addView(textView)
        layoutEscopo.addView(buttonVisualizar)
        containerExcluidos.addView(layoutEscopo)
    }
}