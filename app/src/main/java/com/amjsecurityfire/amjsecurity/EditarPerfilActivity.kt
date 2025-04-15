package com.amjsecurityfire.amjsecurity;

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditarPerfilActivity : AppCompatActivity(){
    private var imageUri: Uri? = null
    private lateinit var storageReference: StorageReference
    private lateinit var imageView: ImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_perfil)

        // Inicializar a referência de armazenamento
        storageReference = FirebaseStorage.getInstance().reference

        // Referências dos campos
        val etNome = findViewById<EditText>(R.id.etNome)
                val etCargo = findViewById<EditText>(R.id.etCargo)
                val btnSalvar = findViewById<Button>(R.id.btn_salvar)

                // Preencher campos com dados recebidos
                etNome.setText(intent.getStringExtra("nome"))
        etCargo.setText(intent.getStringExtra("cargo"))

        // ID do usuário
        val userId = intent.getStringExtra("userId") ?: ""

        // Ação do botão de salvar
        btnSalvar.setOnClickListener {
            val nomeAtualizado = etNome.text.toString().trim()
            val cargoAtualizado = etCargo.text.toString().trim()

            if (nomeAtualizado.isNotBlank() || cargoAtualizado.isNotBlank() || imageUri != null) {
                val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
                val usuarioAtualizado = mutableMapOf<String, Any>()

                if (nomeAtualizado.isNotBlank()) {
                    usuarioAtualizado["nome"] = nomeAtualizado
                }
                if (cargoAtualizado.isNotBlank()) {
                    usuarioAtualizado["cargo"] = cargoAtualizado
                }

                imageUri?.let { uri ->
                        val fotoRef = storageReference.child("perfil/$userId.jpg")
                    fotoRef.putFile(uri).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            fotoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    usuarioAtualizado["fotoPerfil"] = downloadUrl.toString()
                                database.updateChildren(usuarioAtualizado).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        enviarResultado(nomeAtualizado, cargoAtualizado)
                                    } else {
                                        Toast.makeText(this, "Erro ao atualizar dados!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.addOnFailureListener {
                                Toast.makeText(this, "Erro ao obter URL da imagem!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Erro ao fazer upload da foto!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    database.updateChildren(usuarioAtualizado).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            enviarResultado(nomeAtualizado, cargoAtualizado)
                        } else {
                            Toast.makeText(this, "Erro ao atualizar dados!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Nenhuma alteração foi feita!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarResultado(nome: String, cargo: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("nome", nome)
        resultIntent.putExtra("cargo", cargo)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private val galleryResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.data?.let { uri ->
                    imageUri = uri
                Glide.with(this)
                        .load(uri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imageView)
            }
        }
    }
}
