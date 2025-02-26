package com.amjsecurityfire.amjsecurityfire;

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import kotlin.random.Random
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilActivity : AppCompatActivity(){
    private val REQUEST_CODE_EDITAR_PERFIL = 1
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var profileImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val tvNome = findViewById<TextView>(R.id.tvNome)
                val tvCargo = findViewById<TextView>(R.id.tvCargo)
                val tvEmail = findViewById<TextView>(R.id.tvEmail)
                profileImageView = findViewById(R.id.imageView4)

        val user = auth.currentUser
        if (user != null) {
            tvEmail.text = user.email
            val userId = user.uid
            val userRef = database.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val nome = snapshot.child("nome").getValue(String::class.java) ?: "Nome não disponível"
                        val cargo = snapshot.child("cargo").getValue(String::class.java) ?: "Cargo não disponível"

                        tvNome.text = nome
                        tvCargo.text = cargo

                        val firstLetter = nome.firstOrNull() ?: 'N'
                        val profileImage = generateProfileImage(firstLetter, Color.WHITE, 200)
                        profileImageView.setImageBitmap(profileImage)
                    } else {
                        tvNome.text = "Nome não disponível"
                        tvCargo.text = "Cargo não disponível"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvNome.text = "Erro ao carregar nome"
                    tvCargo.text = "Erro ao carregar cargo"
                }
            })
        }

        findViewById<Button>(R.id.btn_editar_perfil).setOnClickListener {
            val intent = Intent(this, EditarPerfilActivity::class.java)
            intent.putExtra("nome", tvNome.text.toString())
            intent.putExtra("cargo", tvCargo.text.toString())
            intent.putExtra("userId", user?.uid)
            startActivityForResult(intent, REQUEST_CODE_EDITAR_PERFIL)
        }

        findViewById<ImageButton>(R.id.btn_voltar).setOnClickListener {
            startActivity(Intent(this, MenuPrincipalActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_logout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDITAR_PERFIL && resultCode == RESULT_OK && data != null) {
            val nomeAtualizado = data.getStringExtra("nome") ?: ""
            val cargoAtualizado = data.getStringExtra("cargo") ?: ""

            // Atualiza os textos na tela de perfil
            findViewById<TextView>(R.id.tvNome).text = nomeAtualizado
            findViewById<TextView>(R.id.tvCargo).text = cargoAtualizado

            // Gera a nova imagem com base na primeira letra do nome atualizado
            val firstLetter = nomeAtualizado.firstOrNull()?.uppercaseChar() ?: 'N'
            val profileImage = generateProfileImage(firstLetter, Color.WHITE, 200)
            profileImageView.setImageBitmap(profileImage)
        }
    }

    private fun generateProfileImage(firstLetter: Char, textColor: Int, imageSize: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Definir a cor de fundo como vermelha (código de cor #FF0000)
        val backgroundColor = Color.RED

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = backgroundColor
        }

        // Desenhar o círculo de fundo
        val radius = imageSize / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // Configuração do texto
        paint.color = textColor
        paint.textSize = imageSize * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val bounds = Rect()

        // Converter a letra para maiúscula
        val upperCaseLetter = firstLetter.uppercaseChar()

        // Obter os limites do texto
        paint.getTextBounds(upperCaseLetter.toString(), 0, 1, bounds)
        val x = imageSize / 2f
        val y = imageSize / 2f - (bounds.top + bounds.bottom) / 2f

        // Desenhar a letra maiúscula no centro
        canvas.drawText(upperCaseLetter.toString(), x, y, paint)
        return bitmap
    }


    private fun generateRandomColor(): Int {
        val random = Random(System.currentTimeMillis())
        val red = random.nextInt(256)
        val green = random.nextInt(256)
        val blue = random.nextInt(256)
        return Color.rgb(red, green, blue)
    }
}
