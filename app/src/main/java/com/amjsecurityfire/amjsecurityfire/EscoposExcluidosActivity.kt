package com.amjsecurityfire.amjsecurityfire;

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurityfire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


public class EscoposExcluidosActivity : AppCompatActivity(){
    private lateinit var db: FirebaseFirestore
    private lateinit var containerExcluidos: LinearLayout
    private lateinit var buttonVoltarMenu: Button
    private val escoposList = mutableListOf<Map<String, String>>() // Lista para armazenar os escopos

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.escopos_excluidos)

        db = FirebaseFirestore.getInstance()
        containerExcluidos = findViewById(R.id.layoutDinamico)
        buttonVoltarMenu = findViewById(R.id.btnVoltarMenu)

        carregarEscoposExcluidos()

        buttonVoltarMenu.setOnClickListener {
            finish() // Voltar ao menu anterior
        }
    }

    private fun carregarEscoposExcluidos() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

                // Busca o nome do usuário no Realtime Database
                val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nomeUsuario = snapshot.child("nome").getValue(String::class.java) ?: "Usuário desconhecido"

                db.collection("escoposExcluidos")
                        .orderBy("numeroEscopo", Query.Direction.ASCENDING)
                        .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar escopos.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    escoposList.clear()
                    containerExcluidos.removeAllViews()

                    snapshots?.let {
                        for (document in it) {
                            val escopo = mapOf(
                                    "numeroEscopo" to (document.getLong("numeroEscopo")?.toString() ?: ""),
                            "empresa" to document.getString("empresa").orEmpty(),
                                    "dataEstimativa" to document.getString("dataEstimativa").orEmpty(),
                                    "tipoServico" to document.getString("tipoServico").orEmpty(),
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
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
        })
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
                val intent = Intent(this@EscoposExcluidosActivity, DetalhesEscopoActivity::class.java)
                intent.putExtra("numeroEscopo", escopo["numeroEscopo"])
                intent.putExtra("empresa", escopo["empresa"])
                intent.putExtra("dataEstimativa", escopo["dataEstimativa"])
                intent.putExtra("tipoServico", escopo["tipoServico"])
                intent.putExtra("status", escopo["status"])
                intent.putExtra("resumoEscopo", escopo["resumoEscopo"])
                intent.putExtra("numeroPedidoCompra", escopo["numeroPedidoCompra"])
                intent.putExtra("motivoExclusao", escopo["motivoExclusao"])
                intent.putExtra("excluidoPor", escopo["excluidoPor"])
                intent.putExtra("escopoId", escopo["escopoId"])
                startActivity(intent)
            }
        }

        layoutEscopo.addView(textView)
        layoutEscopo.addView(buttonVisualizar)
        containerExcluidos.addView(layoutEscopo)
    }
}
