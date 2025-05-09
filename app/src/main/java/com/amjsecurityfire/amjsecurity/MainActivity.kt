package com.amjsecurityfire.amjsecurity

import android.app.DownloadManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private var isPasswordVisible: Boolean = false
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Se já tiver logado, buscar se foi aprovado e redirecionar
            database.child("users").child(currentUser.uid).get().addOnSuccessListener { snapshot ->
                val aprovado = snapshot.child("aprovado").getValue(Boolean::class.java) ?: false
                if (aprovado) {
                    val nome = snapshot.child("nome").value.toString()
                    val cargo = snapshot.child("cargo").value.toString()
                    val email = snapshot.child("email").value.toString()
                    navigateToProfile(nome, cargo, email)
                } else {
                    startActivity(Intent(this, AguardandoAprovacaoActivity::class.java))
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar aprovação automática", Toast.LENGTH_SHORT).show()
            }
            return
        }


        // Encontrando os componentes
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val senhaEditText = findViewById<EditText>(R.id.senhaEditText)
        val eyeIcon = findViewById<ImageView>(R.id.eyeIcon)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgotPasswordTextView)
        val entrarButton = findViewById<Button>(R.id.entrarButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        eyeIcon.setOnClickListener {
            if (isPasswordVisible) {
                senhaEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeIcon.setImageResource(R.drawable.closed_eye_icon)
            } else {
                senhaEditText.inputType = InputType.TYPE_CLASS_TEXT
                eyeIcon.setImageResource(R.drawable.olho)
            }
            isPasswordVisible = !isPasswordVisible
            senhaEditText.setSelection(senhaEditText.text.length)
        }

        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, EsqueciSenhaActivity::class.java)
            startActivity(intent)
        }

        entrarButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val senha = senhaEditText.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                mostrarProgressBar(true)
                signInWithEmailAndPassword(email, senha)
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
                        val aprovado = snapshot.child("aprovado").getValue(Boolean::class.java) ?: false
                        mostrarProgressBar(false)
                        if (aprovado) {
                            val nome = snapshot.child("nome").value.toString()
                            val cargo = snapshot.child("cargo").value.toString()
                            val email = snapshot.child("email").value.toString()
                            navigateToProfile(nome, cargo, email)
                        } else {
                            startActivity(Intent(this, AguardandoAprovacaoActivity::class.java))
                            finish()
                        }
                    }.addOnFailureListener {
                        mostrarProgressBar(false)
                        Toast.makeText(this, "Erro ao verificar aprovação", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                mostrarProgressBar(false)
                Toast.makeText(this, "Falha no login: E-mail ou senha incorreta!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(context: Context, fileUrl: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(fileUrl))
            request.setTitle("Baixando $fileName")
            request.setDescription("Por favor, aguarde...")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(false)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download iniciado...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao iniciar o download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarProgressBar(visivel: Boolean) {
        val progressBarContainer = findViewById<FrameLayout>(R.id.progressBarContainer)
        progressBarContainer.visibility = if (visivel) View.VISIBLE else View.GONE
        setEnableViews(!visivel)
    }

    private fun setEnableViews(enable: Boolean) {
        findViewById<EditText>(R.id.emailEditText).isEnabled = enable
        findViewById<EditText>(R.id.senhaEditText).isEnabled = enable
        findViewById<ImageView>(R.id.eyeIcon).isEnabled = enable
        findViewById<Button>(R.id.entrarButton).isEnabled = enable
        findViewById<Button>(R.id.registerButton).isEnabled = enable
        findViewById<TextView>(R.id.forgotPasswordTextView).isEnabled = enable
    }

    private fun navigateToProfile(nome: String, cargo: String, email: String) {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.putExtra("nome", nome)
        intent.putExtra("cargo", cargo)
        intent.putExtra("email", email)
        startActivity(intent)
        finish()
    }

    companion object {
        private var TAG = "EmailAndPassword"
    }
}
