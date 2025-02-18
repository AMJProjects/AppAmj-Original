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


public class HistoricosEscoposActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var listView: ListView
    private val historicoList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historicos_escopos)

        val buttonVoltarMenu = findViewById<Button>(R.id.button4)

        listView = findViewById(R.id.listViewHistorico)
        db = FirebaseFirestore.getInstance()

        // Buscar histórico dos escopos
        db.collection("historicoEscopos")
            .orderBy("data", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val numeroEscopo = document.get("numeroEscopo")?.toString() ?: "Desconhecido"
                    val acao = document.getString("acao") ?: "Ação desconhecida"
                    val usuario = document.getString("usuario") ?: "Usuário desconhecido"
                    val dataCompleta = document.getString("data") ?: "Data desconhecida"

                    // Remover o horário, mantendo apenas a data
                    val dataApenas = dataCompleta.split(" ").firstOrNull() ?: dataCompleta

                    val historicoInfo = "Escopo $numeroEscopo - $acao\nRealizado por $usuario em $dataApenas"
                    historicoList.add(historicoInfo)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historicoList)
                listView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar histórico: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        buttonVoltarMenu.setOnClickListener {
            finish()
        }
    }
}
