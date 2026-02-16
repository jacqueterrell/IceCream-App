package com.icecreamapp.sweethearts.data

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/** Result of loading dropoff requests: either data or a load error message. */
data class DropoffRequestsResult(
    val requests: List<DropoffRequest>,
    val loadError: String? = null,
)

private const val POLL_INTERVAL_MS = 5000L

class DropoffRepository {

    /**
     * Fetches dropoff requests via Cloud Function (no direct Firestore read needed).
     * Emits immediately, then polls every [POLL_INTERVAL_MS] while collected.
     */
    fun dropoffRequestsFlow(): Flow<DropoffRequestsResult> = flow {
        emit(fetchDropoffRequests())
        while (true) {
            delay(POLL_INTERVAL_MS)
            emit(fetchDropoffRequests())
        }
    }

    suspend fun fetchDropoffRequests(): DropoffRequestsResult = runCatching {
        val result = Firebase.functions
            .getHttpsCallable("getDropoffRequests")
            .call()
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: return@runCatching DropoffRequestsResult(emptyList())
        val rawList = data["requests"] as? List<Map<String, Any>> ?: emptyList()
        val requests = rawList.mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            val name = map["name"] as? String ?: return@mapNotNull null
            val phoneNumber = map["phoneNumber"] as? String ?: return@mapNotNull null
            val lat = (map["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            val lng = (map["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            DropoffRequest(id = id, name = name, phoneNumber = phoneNumber, latitude = lat, longitude = lng)
        }
        DropoffRequestsResult(requests = requests)
    }.getOrElse { e ->
        DropoffRequestsResult(emptyList(), "Could not load: ${e.message ?: "Unknown error"}")
    }

    suspend fun markDropoffDone(dropoffId: String): Result<Unit> = kotlin.runCatching {
        Firebase.functions
            .getHttpsCallable("markDropoffDone")
            .call(mapOf("dropoffId" to dropoffId))
            .await()
    }
}
