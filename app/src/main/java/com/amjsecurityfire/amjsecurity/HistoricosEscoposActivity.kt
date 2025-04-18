package com.amjsecurityfire.amjsecurity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoricosEscoposActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var listView: ListView
    private val historicoList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historicos_escopos)

        // Inicializa os componentes da interface
        val buttonVoltarMenu = findViewById<Button>(R.id.button4)
        listView = findViewById(R.id.listViewHistorico)
        db = FirebaseFirestore.getInstance()

        // Carrega o histórico ao abrir a tela
        carregarHistorico()

        // Configura o botão para voltar ao menu
        buttonVoltarMenu.setOnClickListener {
            finish() // Fecha a activity atual e volta para a tela anterior
        }
    }

    private fun carregarHistorico() {
        db.collection("historicoEscopos")
            .orderBy("data", Query.Direction.DESCENDING) // Ordena do mais recente ao mais antigo
            .get()
            .addOnSuccessListener { querySnapshot ->
                historicoList.clear()
                for (document in querySnapshot) {
                    val numeroEscopo = document.getLong("número do escopo")?.toString() ?: "Desconhecido"
                    val acao = document.getString("ação") ?: "Ação desconhecida"
                    val usuario = document.getString("usuário") ?: "Usuário desconhecido"
                    val data = document.getString("data") ?: "Data desconhecida"
                    val status = document.getString("status") ?: "Status desconhecido" // Recupera o status

                    // Se a data estiver no formato completo (exemplo: dd/MM/yyyy HH:mm:ss), use substring para remover a hora
                    val dataFormatada = if (data.length >= 10) data.substring(0, 10) else data

                    // Formata a informação para exibição, incluindo o status
                    val historicoInfo = "Escopo: #$numeroEscopo\n" +
                            "Ação: $acao\n" +
                            "Realizado por: $usuario\n" +
                            "Data: $dataFormatada\n" +
                            "Status: $status" // Adiciona o status à string formatada
                    historicoList.add(historicoInfo)
                }

                // Atualiza o ListView com os dados
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historicoList)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar histórico: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}