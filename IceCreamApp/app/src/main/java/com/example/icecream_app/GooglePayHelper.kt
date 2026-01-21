package com.example.icecream_app

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.*
import org.json.JSONArray
import org.json.JSONObject

data class TransactionInfo(
    val totalPrice: String,
    val currencyCode: String = "USD",
    val totalPriceStatus: String = "FINAL"
)

class GooglePayHelper(private val activity: ComponentActivity) {
    private val paymentsClient: PaymentsClient = Wallet.getPaymentsClient(
        activity,
        Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // Use TEST for sandbox/development
            .build()
    )

    private var paymentDataRequest: ActivityResultLauncher<IntentSenderRequest>? = null

    fun initializePaymentLauncher(
        onPaymentSuccess: (String) -> Unit,
        onPaymentError: (Exception) -> Unit
    ) {
        paymentDataRequest = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val paymentData = PaymentData.getFromIntent(result.data ?: return@registerForActivityResult)
                paymentData?.let {
                    val paymentInfo = it.toJson()
                    onPaymentSuccess(paymentInfo)
                } ?: onPaymentError(Exception("Payment data is null"))
            } else {
                onPaymentError(Exception("Payment cancelled or failed"))
            }
        }
    }

    fun isGooglePayAvailable(callback: (Boolean) -> Unit) {
        val request = IsReadyToPayRequest.fromJson(createIsReadyToPayRequest().toString())
        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                try {
                    val result = task.getResult(ApiException::class.java)
                    callback(result ?: false)
                } catch (e: ApiException) {
                    callback(false)
                }
            }
    }

    fun requestPaymentWithLauncher(
        transactionInfo: TransactionInfo,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val paymentDataRequestJson = createPaymentDataRequest(transactionInfo)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        paymentsClient.loadPaymentData(request)
            .addOnCompleteListener { task ->
                android.util.Log.d("GooglePayHelper", "Payment task completed, isSuccessful: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    // task.result is already a PaymentData object, not an Intent
                    val paymentData = task.result
                    android.util.Log.d("GooglePayHelper", "Payment data received: ${paymentData != null}")
                    paymentData?.let {
                        val paymentInfo = it.toJson()
                        android.util.Log.d("GooglePayHelper", "Calling onSuccess callback")
                        onSuccess(paymentInfo)
                    } ?: onError(Exception("Payment data is null"))
                } else {
                    val exception = task.exception
                    android.util.Log.e("GooglePayHelper", "Payment failed with exception: ${exception?.javaClass?.simpleName}", exception)
                    android.util.Log.e("GooglePayHelper", "Exception message: ${exception?.message}")
                    
                    if (exception is ResolvableApiException) {
                        android.util.Log.d("GooglePayHelper", "Exception is ResolvableApiException, attempting to resolve")
                        try {
                            val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                            paymentDataRequest?.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            android.util.Log.e("GooglePayHelper", "Error launching intent sender: ${e.message}", e)
                            onError(e)
                        }
                    } else {
                        android.util.Log.e("GooglePayHelper", "Exception is not resolvable, calling onError")
                        onError(exception ?: Exception("Payment request failed"))
                    }
                }
            }
    }

    private fun createIsReadyToPayRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "CARD")
                    put("parameters", JSONObject().apply {
                        put("allowedAuthMethods", JSONArray().apply {
                            put("PAN_ONLY")
                            put("CRYPTOGRAM_3DS")
                        })
                        put("allowedCardNetworks", JSONArray().apply {
                            put("AMEX")
                            put("DISCOVER")
                            put("JCB")
                            put("MASTERCARD")
                            put("VISA")
                        })
                    })
                    put("tokenizationSpecification", JSONObject().apply {
                        put("type", "PAYMENT_GATEWAY")
                        put("parameters", JSONObject().apply {
                            put("gateway", "example")
                            put("gatewayMerchantId", "exampleGatewayMerchantId")
                        })
                    })
                })
            })
        }
    }

    private fun createPaymentDataRequest(transactionInfo: TransactionInfo): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("merchantInfo", JSONObject().apply {
                put("merchantName", "Ice Cream Shop")
                put("merchantId", "BCR2DN6T7L5ODK3Q") // Test merchant ID for sandbox
            })
            put("transactionInfo", JSONObject().apply {
                put("totalPrice", transactionInfo.totalPrice)
                put("totalPriceStatus", transactionInfo.totalPriceStatus)
                put("currencyCode", transactionInfo.currencyCode)
            })
            put("allowedPaymentMethods", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "CARD")
                    put("parameters", JSONObject().apply {
                        put("allowedAuthMethods", JSONArray().apply {
                            put("PAN_ONLY")
                            put("CRYPTOGRAM_3DS")
                        })
                        put("allowedCardNetworks", JSONArray().apply {
                            put("AMEX")
                            put("DISCOVER")
                            put("JCB")
                            put("MASTERCARD")
                            put("VISA")
                        })
                    })
                    put("tokenizationSpecification", JSONObject().apply {
                        put("type", "PAYMENT_GATEWAY")
                        put("parameters", JSONObject().apply {
                            put("gateway", "example")
                            put("gatewayMerchantId", "exampleGatewayMerchantId")
                        })
                    })
                })
            })
        }
    }

}
