package com.amjsecurityfire.amjsecurityfire;

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
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
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.concurrent.TimeUnit


class EscoposExcluidosActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var containerExcluidos: LinearLayout
    private lateinit var textDiasRestantes: TextView
    private lateinit var buttonVoltarMenu: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarContainer: FrameLayout
    private val escoposList = mutableListOf<Map<String, String>>() // Lista para armazenar os escopos

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.escopos_excluidos)

        db = FirebaseFirestore.getInstance()
        containerExcluidos = findViewById(R.id.layoutDinamico)
        textDiasRestantes = findViewById(R.id.textDiasRestantes)
        buttonVoltarMenu = findViewById(R.id.btnVoltarMenu)
        progressBarContainer = findViewById(R.id.progressBarContainer)
        progressBar = findViewById(R.id.progressBar)

        // Exibe a ProgressBar ao iniciar a tela
        progressBarContainer.visibility = View.VISIBLE
        buttonVoltarMenu.isEnabled = false // Desabilitar o botão

        excluirEscoposAntigos {
            carregarEscoposExcluidos()
        }

        buttonVoltarMenu.setOnClickListener {
            finish()
        }
    }

    private fun excluirEscoposAntigos(onComplete: () -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -90) // Define a data limite para exclusão
        val dataLimite = Timestamp(calendar.time)

        db.collection("escoposExcluidos")
            .whereLessThan("dataExclusao", dataLimite) // Filtra escopos com mais de 90 dias
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference) // Remove cada documento encontrado
                }
                batch.commit().addOnCompleteListener {
                    onComplete() // Após a exclusão, recarrega os escopos
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao excluir escopos antigos", e)
                onComplete()
            }
    }

    private fun carregarEscoposExcluidos() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nomeUsuario = snapshot.child("nome").getValue(String::class.java) ?: "Usuário desconhecido"

                db.collection("escoposExcluidos")
                    .orderBy("numeroEscopo", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar escopos.", Toast.LENGTH_SHORT).show()
                            progressBarContainer.visibility = View.GONE
                            buttonVoltarMenu.isEnabled = true // Habilitar o botão em caso de erro
                            return@addSnapshotListener
                        }

                        escoposList.clear()
                        containerExcluidos.removeAllViews()

                        var diasMinimos = Int.MAX_VALUE

                        snapshots?.let {
                            for (document in it) {
                                val dataExclusaoValue = document.get("dataExclusao")
                                val diasRestantes = when (dataExclusaoValue) {
                                    is com.google.firebase.Timestamp -> calcularDiasRestantes(dataExclusaoValue)
                                    else -> 0
                                }

                                val numeroEscopo = document.get("numeroEscopo")?.toString()?.toIntOrNull()?.toString() ?: ""

                                val escopo = hashMapOf<String, Any>(
                                    "numeroEscopo" to numeroEscopo,
                                    "empresa" to document.getString("empresa").orEmpty(),
                                    "dataEstimativa" to document.getString("dataEstimativa").orEmpty(),
                                    "tipoServico" to document.getString("tipoServico").orEmpty(),
                                    "status" to document.getString("status").orEmpty(),
                                    "resumoEscopo" to document.getString("resumoEscopo").orEmpty(),
                                    "numeroPedidoCompra" to document.getString("numeroPedidoCompra").orEmpty(),
                                    "motivoExclusao" to document.getString("motivoExclusao").orEmpty(),
                                    "excluidoPor" to (document.getString("excluidoPor") ?: nomeUsuario),
                                    "escopoId" to document.id,
                                    "dataExclusao" to Timestamp.now()
                                )
                                db.collection("escoposExcluidos").add(escopo)

                                val escopoStringMap = escopo.mapValues { it.value.toString() }

                                Log.d("Firestore", "Escopo carregado: $escopo")
                                adicionarTextoDinamico(escopoStringMap)

                                if (diasRestantes < diasMinimos) {
                                    diasMinimos = diasRestantes
                                }
                            }
                        }

                        textDiasRestantes.text = if (diasMinimos != Int.MAX_VALUE) {
                            "Os escopos excluídos serão removidos permanentemente em $diasMinimos dias."
                        } else {
                            "Não há escopos para excluir."
                        }
                        progressBarContainer.visibility = View.GONE // Oculta a ProgressBar após o carregamento
                        buttonVoltarMenu.isEnabled = true // Habilitar o botão após carregar
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EscoposExcluidosActivity, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show()
                progressBarContainer.visibility = View.GONE // Oculta a ProgressBar em caso de erro
                buttonVoltarMenu.isEnabled = true // Habilitar o botão em caso de erro
            }
        })
    }

    private fun calcularDiasRestantes(dataExclusao: com.google.firebase.Timestamp): Int {
        val dataExpiracao = Calendar.getInstance().apply {
            time = dataExclusao.toDate()
            add(Calendar.DAY_OF_YEAR, 90) // Adiciona 90 dias à data de exclusão
        }.time

        val hoje = Calendar.getInstance().time
        val diferencaMillis = dataExpiracao.time - hoje.time
        return (diferencaMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
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
