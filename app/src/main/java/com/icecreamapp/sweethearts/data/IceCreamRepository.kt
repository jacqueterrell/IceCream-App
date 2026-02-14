package com.icecreamapp.sweethearts.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for ice cream API calls (MVVM data layer).
 * Calls Firebase Cloud Functions callable API.
 */
class IceCreamRepository {

    private val functions: FirebaseFunctions = Firebase.functions

    suspend fun getMenu(): Result<List<IceCreamMenuItem>> = runCatching {
        val result = functions
            .getHttpsCallable("getIceCreamMenu")
            .call()
            .await()
        @Suppress("UNCHECKED_CAST")
        val list = result.getData() as? List<Map<String, Any>>
            ?: return@runCatching emptyList()
        list.map { map ->
            IceCreamMenuItem(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
            )
        }
    }

    suspend fun requestIceCream(flavorId: String, flavorName: String): Result<RequestIceCreamResponse> =
        runCatching {
            val data = mapOf(
                "flavorId" to flavorId,
                "flavorName" to flavorName,
            )
            val result = functions
                .getHttpsCallable("requestIceCream")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any?>
                ?: throw IllegalStateException("Unexpected response")
            RequestIceCreamResponse(
                success = map["success"] as? Boolean ?: false,
                message = map["message"] as? String ?: "",
                orderId = map["orderId"] as? String,
            )
        }

    data class RequestIceCreamResponse(
        val success: Boolean,
        val message: String,
        val orderId: String?,
    )

    suspend fun requestIceCreamDropoff(
        name: String,
        phoneNumber: String,
        latitude: Double,
        longitude: Double,
    ): Result<Unit> = runCatching {
        val data = mapOf(
            "name" to name,
            "phoneNumber" to phoneNumber,
            "latitude" to latitude,
            "longitude" to longitude,
        )
        functions
            .getHttpsCallable("requestIceCreamDropoff")
            .call(data)
            .await()
    }
}
