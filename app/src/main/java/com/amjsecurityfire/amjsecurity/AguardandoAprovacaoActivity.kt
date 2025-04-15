package com.amjsecurityfire.amjsecurity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurity.databinding.AguardandoAprovacaoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AguardandoAprovacaoActivity : AppCompatActivity() {
    private lateinit var binding: AguardandoAprovacaoBinding
    private val database = FirebaseDatabase.getInstance().reference
    private var checandoAprovacao = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = AguardandoAprovacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificarAprovacaoPeriodicamente()
    }

    private fun verificarAprovacaoPeriodicamente() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val handler = Handler(Looper.getMainLooper())
            val delay: Long = 5000 // Verificar a cada 5 segundos

            handler.post(object : Runnable {
                override fun run() {
                    if (checandoAprovacao) {
                        database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val aprovado = snapshot.child("aprovado").getValue(Boolean::class.java)

                                // Verificação adicional
                                if (aprovado == true) {
                                    checandoAprovacao = false
                                    Toast.makeText(this@AguardandoAprovacaoActivity, "Conta aprovada!", Toast.LENGTH_SHORT).show()

                                    val nome = snapshot.child("nome").getValue(String::class.java) ?: ""
                                    val cargo = snapshot.child("cargo").getValue(String::class.java) ?: ""
                                    val email = snapshot.child("email").getValue(String::class.java) ?: ""

                                    val intent = Intent(this@AguardandoAprovacaoActivity, PerfilActivity::class.java)
                                    intent.putExtra("nome", nome)
                                    intent.putExtra("cargo", cargo)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Se ainda não aprovado, continua verificando
                                    handler.postDelayed(this, delay)
                                }
                            } else {
                                Toast.makeText(this@AguardandoAprovacaoActivity, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                                checandoAprovacao = false
                                FirebaseAuth.getInstance().signOut()
                                startActivity(Intent(this@AguardandoAprovacaoActivity, MainActivity::class.java))
                                finish()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this@AguardandoAprovacaoActivity, "Erro ao verificar aprovação", Toast.LENGTH_SHORT).show()
                            handler.postDelayed(this, delay)
                        }
                    }
                }
            })
        }
    }
}
