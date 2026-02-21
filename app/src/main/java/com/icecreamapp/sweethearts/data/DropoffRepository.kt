package com.icecreamapp.sweethearts.data

import android.util.Log
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
            val status = map["status"] as? String
            DropoffRequest(id = id, name = name, phoneNumber = phoneNumber, latitude = lat, longitude = lng, status = status)
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

    suspend fun updateDropoffStatus(dropoffId: String, status: String): Result<Unit> =
        kotlin.runCatching {
            Firebase.functions
                .getHttpsCallable("updateDropoffStatus")
                .call(mapOf("dropoffId" to dropoffId, "status" to status))
                .await()
        }

    suspend fun getOptimizedRoute(
        originLat: Double,
        originLng: Double,
        waypoints: List<DropoffRequest>,
    ): Result<OptimizedRouteResponse> = kotlin.runCatching {
        if (waypoints.isEmpty()) {
            Log.d(TAG_DIRECTIONS, "getOptimizedRoute: no waypoints, returning empty")
            return@runCatching OptimizedRouteResponse(
                waypointOrder = emptyList(),
                legDurationsSeconds = emptyList(),
                encodedPolyline = "",
            )
        }
        Log.d(TAG_DIRECTIONS, "getOptimizedRoute: calling Cloud Function (origin=$originLat,$originLng, waypoints=${waypoints.size})")
        val result = Firebase.functions
            .getHttpsCallable("getOptimizedRoute")
            .call(
                mapOf(
                    "origin" to mapOf("latitude" to originLat, "longitude" to originLng),
                    "waypoints" to waypoints.map { w ->
                        mapOf(
                            "id" to w.id,
                            "name" to w.name,
                            "latitude" to w.latitude,
                            "longitude" to w.longitude,
                        )
                    },
                )
            )
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: throw IllegalStateException("No data")
        val waypointOrder = (data["waypointOrder"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
        val legDurationsSeconds = (data["legDurationsSeconds"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
        val encodedPolyline = data["encodedPolyline"] as? String ?: ""
        Log.d(TAG_DIRECTIONS, "getOptimizedRoute: success waypointOrder=${waypointOrder.size} legs=${legDurationsSeconds.size} polylineLen=${encodedPolyline.length}")
        OptimizedRouteResponse(
            waypointOrder = waypointOrder,
            legDurationsSeconds = legDurationsSeconds,
            encodedPolyline = encodedPolyline,
        )
    }.also { result ->
        result.exceptionOrNull()?.let { e ->
            Log.w(TAG_DIRECTIONS, "getOptimizedRoute: failure", e)
        }
    }
}

private const val TAG_DIRECTIONS = "DirectionsAPI"

data class OptimizedRouteResponse(
    val waypointOrder: List<Int>,
    val legDurationsSeconds: List<Long>,
    val encodedPolyline: String,
)
