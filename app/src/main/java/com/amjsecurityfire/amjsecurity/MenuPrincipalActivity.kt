package com.amjsecurityfire.amjsecurity;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


class MenuPrincipalActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var cargoUsuario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Botões principais
        val escoposPendentesButton = findViewById<Button>(R.id.btn_pendente)
        val adicionarEscopoButton = findViewById<Button>(R.id.btn_add_escopo)
        val escoposConcluidosButton = findViewById<Button>(R.id.btn_concluido)
        val escopoExcluidoButton = findViewById<ImageButton>(R.id.btn_lixo)
        val perfilButton = findViewById<ImageButton>(R.id.perfil)
        val historicoEscopoButton = findViewById<ImageButton>(R.id.btn_historico)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        NotificationHelper.createNotificationChannel(this)

        FirebaseMessaging.getInstance().subscribeToTopic("escopos")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Inscrito no tópico 'escopos'")
                } else {
                    Log.e("FCM", "Erro ao inscrever no tópico", task.exception)
                }
            }

        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = database.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        cargoUsuario = snapshot.child("cargo").getValue(String::class.java) ?: ""

                        if (cargoUsuario == "Supervisor") {
                            configurarPermissoesSupervisor(
                                escoposPendentesButton,
                                escoposConcluidosButton,
                                historicoEscopoButton,
                                perfilButton,
                                adicionarEscopoButton,
                                escopoExcluidoButton
                            )
                        } else if (cargoUsuario == "Técnico") {
                            configurarPermissoesTecnico(
                                escoposPendentesButton,
                                escoposConcluidosButton,
                                historicoEscopoButton,
                                perfilButton,
                                adicionarEscopoButton,
                                escopoExcluidoButton
                            )
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        escoposPendentesButton.setOnClickListener { navegarPara(EscoposPendentesActivity::class.java) }
        adicionarEscopoButton.setOnClickListener { navegarPara(AdicionarEscopoActivity::class.java) }
        escoposConcluidosButton.setOnClickListener { navegarPara(EscoposConcluidosActivity::class.java) }
        perfilButton.setOnClickListener { navegarPara(PerfilActivity::class.java) }
        escopoExcluidoButton.setOnClickListener { navegarPara(EscoposExcluidosActivity::class.java) }
        historicoEscopoButton.setOnClickListener { navegarPara(HistoricosEscoposActivity::class.java) }
    }
        private fun configurarPermissoesSupervisor(
            escoposPendentes: Button,
            escoposConcluidos: Button,
            historicoEscopo: ImageButton,
            perfil: ImageButton,
            adicionarEscopo: Button,
            escopoExcluido: ImageButton
        ) {
            val botoesPermitidos = setOf(escoposPendentes, escoposConcluidos, historicoEscopo, perfil)

            val botoesBloqueados = listOf(adicionarEscopo, escopoExcluido).filterNot { it in botoesPermitidos }

            for (botao in botoesBloqueados) {
                botao.setOnClickListener {
                    mostrarAlerta()
                }
            }
        }
    private fun configurarPermissoesTecnico(
        escoposPendentes: Button,
        escoposConcluidos: Button,
        historicoEscopo: ImageButton,
        perfil: ImageButton,
        adicionarEscopo: Button,
        escopoExcluido: ImageButton
    ) {
        val botoesPermitidos = setOf(escoposPendentes, escoposConcluidos, historicoEscopo, perfil)

        val botoesBloqueados = listOf(adicionarEscopo, escopoExcluido).filterNot { it in botoesPermitidos }

        for (botao in botoesBloqueados) {
            botao.setOnClickListener {
                mostrarAlerta()
            }
        }
    }
        private fun navegarPara(activityClass: Class<*>) {
            startActivity(Intent(this, activityClass))
        }

        private fun mostrarAlerta() {
            AlertDialog.Builder(this)
                .setTitle("Acesso Negado")
                .setMessage("Você não tem permissão para acessar esta funcionalidade.")
                .setPositiveButton("OK", null)
                .show()

         }
}
