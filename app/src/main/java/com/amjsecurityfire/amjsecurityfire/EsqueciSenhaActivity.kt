package com.amjsecurityfire.amjsecurityfire;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


public class EsqueciSenhaActivity : AppCompatActivity(){
    private lateinit var emailEditText: EditText
    private lateinit var sendRecoveryButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.esqueci_senha)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Vincular os componentes da interface
        emailEditText = findViewById(R.id.emailEditText)
        sendRecoveryButton = findViewById(R.id.sendRecoveryButton)

        // Configurar ação do botão "Enviar"
        sendRecoveryButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty() && isValidEmail(email)) {
                enviarEmailDeRecuperacao(email)
            } else {
                Toast.makeText(this, "Por favor, insira um e-mail válido.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuração do botão "Voltar"
        val backButton = findViewById<ImageButton>(R.id.backButton)  // Encontrando o botão de voltar pelo ID
                backButton.setOnClickListener {
            // Criando o Intent para voltar para a LoginActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)  // Inicia a LoginActivity
            finish()  // Finaliza a EsqueciSenhaActivity para evitar empilhamento desnecessário
        }
    }

    // Função para enviar e-mail de recuperação de senha
    private fun enviarEmailDeRecuperacao(email: String) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                        this,
                        "Email de recuperação enviado para $email.",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                val errorMessage = task.exception?.message ?: "Erro ao enviar o e-mail."
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para validar formato de e-mail
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
