package com.amjsecurityfire.amjsecurityfire;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)


        // Botões principais
        val escoposPendentesButton = findViewById<Button>(R.id.btn_pendente)
        val adicionarEscopoButton = findViewById<Button>(R.id.btn_add_escopo)
        val escoposConcluidosButton = findViewById<Button>(R.id.btn_concluido)
        val escopoExcluidoButton = findViewById<ImageButton>(R.id.btn_lixo)
        val perfilButton = findViewById<ImageButton>(R.id.perfil)
        val historicoEscopoButton = findViewById<ImageButton>(R.id.btn_historico)

        // Navegação para as atividades correspondentes
        escoposPendentesButton.setOnClickListener {
            val intent = Intent(this, EscoposPendentesActivity::class.java)
            startActivity(intent)
        }

        adicionarEscopoButton.setOnClickListener {
            val intent = Intent(this, AdicionarEscopoActivity::class.java)
            startActivity(intent)
        }

        escoposConcluidosButton.setOnClickListener {
            val intent = Intent(this, EscoposConcluidosActivity::class.java)
            startActivity(intent)
        }

        perfilButton.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        escopoExcluidoButton.setOnClickListener {
            val intent = Intent(this, EscoposExcluidosActivity::class.java)
            startActivity(intent)
        }

        historicoEscopoButton.setOnClickListener {
            val intent = Intent(this, HistoricosEscoposActivity::class.java)
            startActivity(intent)
        }
    }

    private fun excluirTodosOsEscopos() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("escoposExcluidos")
            val limit = 100

            while (true) {
                val documents = collectionRef.limit(limit.toLong()).get().await()

                if (documents.isEmpty) {
                    withContext(Dispatchers.Main) {
                        showToast("Todos os escopos foram excluídos!")
                    }
                    break
                }

                val batch = db.batch()
                for (document in documents) {
                    batch.delete(collectionRef.document(document.id))
                }

                try {
                    batch.commit().await()
                    Log.d("Firestore", "Todos os documentos deletados com sucesso!")
                } catch (e: Exception) {
                    Log.w("Firestore", "Erro ao deletar documentos em lote", e)
                }
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
