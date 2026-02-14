package com.icecreamapp.sweethearts.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "dropoffRequests"

class DropoffRepository {

    private val db = Firebase.firestore

    /**
     * Stream of dropoff requests where done is not true (includes missing done).
     * Updates when Firestore data changes.
     */
    fun dropoffRequestsFlow(): Flow<List<DropoffRequest>> = callbackFlow {
        val listener = db.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.filter { (it.get("done") as? Boolean) != true }
                    ?.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val phoneNumber = doc.getString("phoneNumber") ?: return@mapNotNull null
                        val lat = (doc.get("latitude") as? Number)?.toDouble() ?: return@mapNotNull null
                        val lng = (doc.get("longitude") as? Number)?.toDouble() ?: return@mapNotNull null
                        DropoffRequest(
                            id = doc.id,
                            name = name,
                            phoneNumber = phoneNumber,
                            latitude = lat,
                            longitude = lng,
                        )
                    }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markDropoffDone(dropoffId: String): Result<Unit> = kotlin.runCatching {
        Firebase.functions
            .getHttpsCallable("markDropoffDone")
            .call(mapOf("dropoffId" to dropoffId))
            .await()
    }
}
