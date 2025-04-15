package com.amjsecurityfire.amjsecurity;

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*


class EscoposConcluidosActivity : AppCompatActivity(){
    private lateinit var db: FirebaseFirestore
    private lateinit var containerConcluidos: LinearLayout
    private lateinit var buttonVoltarMenu: Button
    private lateinit var searchView: SearchView
    private val escoposList = mutableListOf<Map<String, String>>()
    private var nomeUsuario: String = "Desconhecido"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.escopos_concluidos)

        db = FirebaseFirestore.getInstance()
        containerConcluidos = findViewById(R.id.layoutDinamico)
        buttonVoltarMenu = findViewById(R.id.button4)
        searchView = findViewById(R.id.searchView)
        carregarNomeUsuario()

        buttonVoltarMenu.setOnClickListener { finish() }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filtrarEscopos(query); return true }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) carregarEscoposConcluidos() else filtrarEscopos(newText)
                return true
            }
        })

        // Configuração do SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarEscopos(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    carregarEscoposConcluidos() // Recarrega todos os escopos quando a busca é apagada
                } else {
                    filtrarEscopos(newText)
                }
                return true
            }
        })
    }

    private fun carregarEscoposConcluidos() {
        db.collection("escoposConcluidos")
                .orderBy("numeroEscopo", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this@EscoposConcluidosActivity, "Erro ao carregar escopos.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            escoposList.clear()
            containerConcluidos.removeAllViews()

            snapshots?.let {
                for (document in it) {
                    val escopo = hashMapOf(
                            "numeroEscopo" to (document.getLong("numeroEscopo")?.toString() ?: ""),
                    "empresa" to document.getString("empresa").orEmpty(),
                            "dataEstimativa" to document.getString("dataEstimativa").orEmpty(),
                            "status" to document.getString("status").orEmpty(),
                            "tipoServico" to document.getString("tipoServico").orEmpty(),
                            "resumoEscopo" to document.getString("resumoEscopo").orEmpty(),
                            "numeroPedidoCompra" to document.getString("numeroPedidoCompra").orEmpty(),
                            "escopoId" to document.id,
                            "pdfUrl" to document.getString("pdfUrl").orEmpty(),
                            "criadorNome" to document.getString("criadorNome").orEmpty(),
                            "dataCriacao" to document.getString("dataCriacao").orEmpty()
                        )
                    escoposList.add(escopo)
                    adicionarTextoDinamico(escopo)
                }
            }
        }
    }

    private fun carregarNomeUsuario() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    nomeUsuario = snapshot.getValue(Usuario::class.java)?.nome ?: "Desconhecido"
                    carregarEscoposConcluidos()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EscoposConcluidosActivity, "Erro ao carregar nome do usuário", Toast.LENGTH_SHORT).show()
                    carregarEscoposConcluidos()
                }
            })
        } else {
            carregarEscoposConcluidos()
        }
    }

    private fun filtrarEscopos(query: String?) {
        val filtro = query?.toLowerCase(Locale.getDefault())?.trim()
        containerConcluidos.removeAllViews()

        for (escopo in escoposList) {
            if (escopo["numeroEscopo"]?.toLowerCase(Locale.getDefault())?.contains(filtro ?: "") == true ||
                    escopo["empresa"]?.toLowerCase(Locale.getDefault())?.contains(filtro ?: "") == true
            ) {
                adicionarTextoDinamico(escopo)
            }
        }
    }

    private fun adicionarTextoDinamico(escopo: Map<String, String>) {
        val layoutEscopo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            background = resources.getDrawable(R.drawable.botaoredondo)
        }

        val dataEstimativaStr = escopo["dataEstimativa"].orEmpty()
        var borderColor = Color.GRAY
        if (dataEstimativaStr.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                val dataEstimativa = sdf.parse(dataEstimativaStr)
                val diffMillis = dataEstimativa.time - Date().time
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                borderColor = when {
                    diffDays < 0 -> Color.parseColor("#FF0000")
                    diffDays <= 3 -> Color.parseColor("#FFA500")
                    diffDays <= 7 -> Color.parseColor("#FFFF00")
                    else -> Color.parseColor("#00FF00")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                borderColor = Color.GRAY
            }
        }

        val circleView = CircleView(this, borderColor).apply {
            layoutParams = LinearLayout.LayoutParams(50, 50).apply {
                setMargins(0, 0, 0, 8)
            }
        }
        layoutEscopo.addView(circleView)

        val textoEscopo = """
        Número: ${escopo["numeroEscopo"]}
        Empresa: ${escopo["empresa"]}
        Data Estimada: ${escopo["dataEstimativa"]}
        Status: ${escopo["status"]}
        Criado por: ${escopo["criadorNome"] ?: "Desconhecido"}
    """.trimIndent()

        val textView = TextView(this).apply {
            text = textoEscopo
            textSize = 16f
        }

        val buttonsLayoutHorizontal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 16)
        }

        val buttonVisualizar = criarBotao("Visualizar") {
            navegarParaDetalhesEscopo(escopo)
        }
        val buttonExcluir = criarBotao("Excluir") {
            excluirEscopo(escopo)
        }

        val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(8, 0, 8, 0)
        }

        buttonVisualizar.layoutParams = layoutParams
        buttonExcluir.layoutParams = layoutParams

        buttonsLayoutHorizontal.apply {
            addView(buttonVisualizar)
            addView(buttonExcluir)
        }

        val buttonAlterarStatusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val layoutParamsAlterarStatus = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f).apply {
            setMargins(8, 0, 8, 0)
        }

        val buttonAlterarStatus = criarBotao("Marcar como Pendente") {
            alterarStatusEscopoParaPendente(escopo, "escoposConcluidos", "escoposPendentes", "Pendente")
        }

        buttonAlterarStatus.layoutParams = layoutParamsAlterarStatus
        buttonAlterarStatusLayout.addView(buttonAlterarStatus)

        layoutEscopo.apply {
            addView(textView)
            addView(buttonsLayoutHorizontal)
            addView(buttonAlterarStatusLayout)
        }

        containerConcluidos.addView(layoutEscopo)
    }

    private fun criarBotao(texto: String, acao: () -> Unit): Button {
        return Button(this).apply {
            this.text = texto
            setOnClickListener { acao() }
        }
    }

    private fun navegarParaDetalhesEscopo(escopo: Map<String, String>) {
        val intent = Intent(this, DetalhesEscopoActivity::class.java).apply {
            escopo.forEach { putExtra(it.key, it.value) }
            putExtra("colecaoOrigem", "escoposConcluidos")
        }
        startActivity(intent)
    }

    private fun alterarStatusEscopoParaPendente(
            escopo: Map<String, String>,
    colecaoAtual: String,
            novaColecao: String,
            novoStatus: String
    ) {
        val escopoId = escopo["escopoId"] ?: return
                val alertDialog = AlertDialog.Builder(this)
                .setTitle("Confirmar Alteração de Status")
                .setMessage("Você tem certeza de que deseja marcar este escopo como $novoStatus?")
                .setPositiveButton("Sim") { dialog, _ ->
                db.collection(colecaoAtual).document(escopoId).get()
                        .addOnSuccessListener { document ->
            if (document.exists()) {
                val dadosAtualizados = document.data?.toMutableMap() ?: return@addOnSuccessListener
                        dadosAtualizados["status"] = novoStatus
                moverDocumentoParaColecao(dadosAtualizados, escopoId, novaColecao, colecaoAtual)
            }
        }
            dialog.dismiss()
        }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .create()
        alertDialog.show()
    }

    private fun excluirEscopo(escopo: Map<String, String>) {
        val escopoId = escopo["escopoId"] ?: return
        val input = EditText(this).apply {
            hint = "Digite o motivo da exclusão"
            setPadding(20, 0, 0, 25)
        }

        AlertDialog.Builder(this)
            .setTitle("Excluir Escopo")
            .setMessage("Tem certeza de que deseja excluir este escopo permanentemente?")
            .setView(input)
            .setPositiveButton("Sim") { _, _ ->
                val motivo = input.text.toString().trim()
                if (motivo.isEmpty()) {
                    Toast.makeText(this, "Por favor, informe o motivo da exclusão.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                db.collection("escoposConcluidos").document(escopoId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val escopoData = document.data?.toMutableMap() ?: return@addOnSuccessListener
                            escopoData["motivoExclusao"] = motivo
                            escopoData["dataExclusao"] = System.currentTimeMillis()
                            escopoData["excluidoPor"] = nomeUsuario  // Salva o nome fixo no momento da exclusão

                            moverDocumentoParaColecao(escopoData, escopoId, "escoposExcluidos", "escoposConcluidos")
                        }
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun moverDocumentoParaColecao(
            dadosAtualizados: MutableMap<String, Any>,
    escopoId: String,
            novaColecao: String,
            colecaoAtual: String
    ) {
        db.collection(novaColecao).document(escopoId)
                .set(dadosAtualizados)
                .addOnSuccessListener {
            db.collection(colecaoAtual).document(escopoId).delete()
                    .addOnSuccessListener {
                Toast.makeText(this, "Operação concluída com sucesso!", Toast.LENGTH_SHORT).show()
                carregarEscoposConcluidos()
            }
                    .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao remover escopo da coleção atual: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao mover escopo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
