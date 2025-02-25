package com.amjsecurityfire.amjsecurityfire;

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurityfire.R
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
            .orderBy("data", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                historicoList.clear()
                for (document in querySnapshot) {
                    val numeroEscopo = document.getLong("numero do escopo")?.toString() ?: "Desconhecido"
                    val acao = document.getString("ação") ?: "Ação desconhecida"
                    val usuario = document.getString("usuário") ?: "Usuário desconhecido"
                    val data = document.getString("data") ?: "Data desconhecida"

                    // Formata a informação para exibição
                    val historicoInfo = "Escopo #$numeroEscopo - $acao\n" +
                            "Realizado por: $usuario\n" +
                            "Data: $data"
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