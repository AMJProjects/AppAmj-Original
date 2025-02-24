package com.amjsecurityfire.amjsecurityfire

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ExcluirEscoposWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) { // Usa CoroutineWorker para lidar com chamadas assíncronas corretamente

    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()

        return try {
            val documents = db.collection("escoposExcluidos").get().await()

            val batch = db.batch()
            documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            Log.d("Firestore", "Escopos excluídos permanentemente.")
            Result.success()
        } catch (e: Exception) {
            Log.e("Firestore", "Erro ao excluir escopos", e)
            Result.retry() // Tenta novamente se houver falha
        }
    }
}
