package com.amjsecurityfire.amjsecurity;

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amjsecurityfire.amjsecurity.databinding.RegistroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.FirebaseDatabase


class RegistroActivity : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: RegistroBinding
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializando o binding
        binding = RegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val nome = binding.etNome.text.toString().trim()
            val cargo = binding.etCargo.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Verificar campos vazios
            if (nome.isEmpty() || cargo.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            } else {
                createUserAndSaveData(nome, cargo, email, password)
            }
        }

        binding.btnVoltarMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java) // Voltar ao menu principal
            startActivity(intent)
            finish()
        }
    }

    private fun createUserAndSaveData(nome: String, cargo: String, email: String, password: String) {
        val dominioPermitido = "amjsecurityfire.com.br"
        val dominioEmail = email.substringAfterLast("@")

        if (dominioEmail != dominioPermitido) {
            Toast.makeText(this, "O registro só é permitido para emails do domínio da empresa", Toast.LENGTH_LONG).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val userMap = mapOf(
                        "nome" to nome,
                        "cargo" to cargo,
                        "email" to email,
                        "aprovado" to false // Usuário começa não aprovado
                    )
                    database.child("users").child(userId).setValue(userMap).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {
                            Toast.makeText(this, "Conta criada! Aguarde aprovação.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AguardandoAprovacaoActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Erro ao salvar os dados", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                handleFirebaseAuthError(task.exception)
            }
        }
    }

    private fun navigateToProfile(nome: String, cargo: String, email: String) {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.putExtra("nome", nome)
        intent.putExtra("cargo", cargo)
        intent.putExtra("email", email)
        startActivity(intent)
        finish()
    }

    private fun handleFirebaseAuthError(exception: Exception?) {
        if (exception is FirebaseAuthWeakPasswordException) {
            Toast.makeText(this, "Senha muito fraca! Deve conter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
        } else if (exception is FirebaseAuthUserCollisionException) {
            Toast.makeText(this, "Este e-mail já está cadastrado.", Toast.LENGTH_SHORT).show()
        } else if (exception is FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "E-mail inválido.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Erro: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
