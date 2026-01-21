package com.example.icecream_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.app.Activity
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.icecream_app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.icecream_app.ui.theme.IceCreamAppTheme

data class IceCreamProduct(
    val id: Int,
    val name: String,
    val price: Double,
    val imageRes: Int = R.drawable.ice_cream_ex_1
)

class MainActivity : ComponentActivity() {
    private lateinit var googlePayHelper: GooglePayHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        googlePayHelper = GooglePayHelper(this)
        googlePayHelper.initializePaymentLauncher(
            onPaymentSuccess = { paymentInfo ->
                runOnUiThread {
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                    // Handle successful payment - you can parse paymentInfo JSON here
                }
            },
            onPaymentError = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Payment failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        setContent {
            IceCreamAppTheme {
                IceCreamAppApp(googlePayHelper, this)
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun IceCreamAppApp(googlePayHelper: GooglePayHelper? = null, activity: Activity? = null) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val context = LocalContext.current
    val activityContext = remember(activity) { 
        activity ?: run {
            var act: Activity? = null
            var ctx = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) {
                    act = ctx
                    break
                }
                ctx = ctx.baseContext
            }
            act ?: (context as? Activity)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> {
                if (googlePayHelper != null) {
                    IceCreamProductScreen(
                        onCheckout = { total, products, cart ->
                            val transactionInfo = TransactionInfo(
                                totalPrice = total.replace("$", ""),
                                currencyCode = "USD",
                                totalPriceStatus = "FINAL"
                            )
                            googlePayHelper.requestPaymentWithLauncher(
                                transactionInfo = transactionInfo,
                                onSuccess = { paymentInfo ->
                                    android.util.Log.d("PaymentFlow", "Google Pay success callback triggered")
                                    Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                                    
                                    // Ensure we're on UI thread
                                    android.util.Log.d("PaymentFlow", "About to call sendPurchaseEmail, activityContext: ${activityContext != null}")
                                    if (activityContext is Activity) {
                                        (activityContext as Activity).runOnUiThread {
                                            android.util.Log.d("PaymentFlow", "Calling sendPurchaseEmail on UI thread")
                                            sendPurchaseEmail(context, activityContext, products, cart, total)
                                        }
                                    } else {
                                        android.util.Log.d("PaymentFlow", "Calling sendPurchaseEmail directly (no Activity)")
                                        sendPurchaseEmail(context, activityContext, products, cart, total)
                                    }
                                },
                                onError = { exception ->
                                    android.util.Log.e("PaymentFlow", "Payment error callback: ${exception.message}", exception)
                                    Toast.makeText(context, "Payment failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                } else {
                    IceCreamProductScreen(
                        onCheckout = { _, _, _ ->
                            Toast.makeText(context, "Google Pay not initialized", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            AppDestinations.FAVORITES -> {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = "Favorites",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            AppDestinations.PROFILE -> {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = "Profile",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IceCreamTopBar() {
    TopAppBar(
        title = { Text("Ice Cream Shop") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF90CAF9)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IceCreamProductScreen(
    onCheckout: (String, List<IceCreamProduct>, Map<Int, Int>) -> Unit
) {
    val products = remember { sampleIceCreams() }
    val cart = remember { mutableStateMapOf<Int, Int>() }
    val context = LocalContext.current
    val activity = remember { context as? Activity }

    Scaffold(
        topBar = {
            IceCreamTopBar()
        },
        bottomBar = {
            BottomAppBar {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Quantity: ${calculateTotalQuantity(cart)}",
                        fontSize = 14.sp
                    )
                    Text(
                        "Total: $${calculateTotal(cart, products)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    // Receipt button to test email feature
                    Button(
                        onClick = {
                            val total = calculateTotal(cart, products)
                            val quantity = calculateTotalQuantity(cart)
                            if (quantity > 0) {
                                sendPurchaseEmail(context, activity, products, cart.toMap(), total)
                            } else {
                                Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = calculateTotalQuantity(cart) > 0,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Receipt")
                    }
                    // Checkout button for Google Pay
                    Button(
                        onClick = {
                            val total = calculateTotal(cart, products)
                            val quantity = calculateTotalQuantity(cart)
                            if (quantity > 0) {
                                onCheckout(total, products, cart.toMap())
                            }
                        },
                        enabled = calculateTotalQuantity(cart) > 0
                    ) {
                        Text("Checkout")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(products) { product ->
                IceCreamProductItem(product, cart)
            }
        }
    }
}

@Composable
fun IceCreamProductItem(product: IceCreamProduct, cart: MutableMap<Int, Int>) {
    // Read cart value to ensure proper state observation
    val quantity = cart[product.id] ?: 0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = CenterVertically
    ) {
        Image(
            painter = painterResource(id = product.imageRes),
            contentDescription = product.name,
            modifier = Modifier.size(64.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                product.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Price: $${product.price}",
                fontSize = 14.sp
            )
        }
        Row(verticalAlignment = CenterVertically) {
            IconButton(onClick = { decrement(cart, product.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
            Text(
                quantity.toString(),
                modifier = Modifier.padding(8.dp)
            )
            IconButton(onClick = { increment(cart, product.id) }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}

// JSON-like data structure (hardcoded in Kotlin)
private val iceCreamJsonData = """
    [
        {
            "id": 1,
            "name": "Vanilla Bean",
            "price": 4.99
        },
        {
            "id": 2,
            "name": "Chocolate Fudge",
            "price": 5.49
        },
        {
            "id": 3,
            "name": "Strawberry Swirl",
            "price": 4.99
        },
        {
            "id": 4,
            "name": "Mint Chocolate Chip",
            "price": 5.29
        },
        {
            "id": 5,
            "name": "Cookies & Cream",
            "price": 5.49
        },
        {
            "id": 6,
            "name": "Rocky Road",
            "price": 5.79
        },
        {
            "id": 7,
            "name": "Butter Pecan",
            "price": 5.99
        },
        {
            "id": 8,
            "name": "Neapolitan",
            "price": 4.99
        }
    ]
""".trimIndent()

fun sampleIceCreams(): List<IceCreamProduct> {
    // Parse JSON-like data structure into IceCreamProduct objects
    // Since we're hardcoding, we'll create a list directly from the JSON structure
    return listOf(
        IceCreamProduct(id = 1, name = "Vanilla Bean", price = 4.99),
        IceCreamProduct(id = 2, name = "Chocolate Fudge", price = 5.49),
        IceCreamProduct(id = 3, name = "Strawberry Swirl", price = 4.99),
        IceCreamProduct(id = 4, name = "Mint Chocolate Chip", price = 5.29),
        IceCreamProduct(id = 5, name = "Cookies & Cream", price = 5.49),
        IceCreamProduct(id = 6, name = "Rocky Road", price = 5.79),
        IceCreamProduct(id = 7, name = "Butter Pecan", price = 5.99),
        IceCreamProduct(id = 8, name = "Neapolitan", price = 4.99)
    )
}

fun increment(cart: MutableMap<Int, Int>, productId: Int) {
    cart[productId] = (cart[productId] ?: 0) + 1
}

fun decrement(cart: MutableMap<Int, Int>, productId: Int) {
    val current = cart[productId] ?: 0
    if (current > 0) {
        cart[productId] = current - 1
        if (cart[productId] == 0) {
            cart.remove(productId)
        }
    }
}

fun calculateTotalQuantity(cart: Map<Int, Int>): Int {
    return cart.values.sum()
}

fun calculateTotal(cart: Map<Int, Int>, products: List<IceCreamProduct>): String {
    val total = cart.entries.sumOf { (productId, quantity) ->
        val product = products.find { it.id == productId }
        (product?.price ?: 0.0) * quantity
    }
    return String.format("%.2f", total)
}

fun sendPurchaseEmail(
    context: android.content.Context,
    activity: Activity?,
    products: List<IceCreamProduct>,
    cart: Map<Int, Int>,
    total: String
) {
    android.util.Log.d("EmailIntent", "sendPurchaseEmail called, activity: ${activity != null}")
    
    val emailBody = buildString {
        appendLine("Thank you for your purchase!")
        appendLine()
        appendLine("Order Details:")
        appendLine("=".repeat(50))
        appendLine()
        
        cart.forEach { (productId, quantity) ->
            val product = products.find { it.id == productId }
            product?.let {
                val itemTotal = it.price * quantity
                appendLine("${it.name}")
                appendLine("  Quantity: $quantity")
                appendLine("  Price: $${String.format("%.2f", it.price)} each")
                appendLine("  Subtotal: $${String.format("%.2f", itemTotal)}")
                appendLine()
            }
        }
        
        appendLine("=".repeat(50))
        appendLine("Total: $${total}")
        appendLine()
        appendLine("Thank you for shopping with Ice Cream Shop!")
    }

    // Use ACTION_SEND with email type for better compatibility
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("tony.p.doan@gmail.com", "Jacqueterrell@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Ice Cream Shop - Order Confirmation")
        putExtra(Intent.EXTRA_TEXT, emailBody)
    }

    // Check if there are apps that can handle this Intent
    val packageManager = context.packageManager
    val resolveInfo = packageManager.queryIntentActivities(emailIntent, 0)
    android.util.Log.d("EmailIntent", "Apps that can handle email intent: ${resolveInfo.size}")
    
    if (resolveInfo.isEmpty()) {
        Toast.makeText(context, "No email app found on device", Toast.LENGTH_LONG).show()
        return
    }
    
    try {
        // Create chooser
        val chooser = Intent.createChooser(emailIntent, "Send email using...")
        android.util.Log.d("EmailIntent", "Chooser created, starting activity...")
        
        // Start activity - prefer Activity context to show chooser properly
        val targetActivity = activity ?: (context as? Activity)
        android.util.Log.d("EmailIntent", "Target activity: ${targetActivity != null}")
        
        if (targetActivity != null) {
            // Use Activity context - no need for FLAG_ACTIVITY_NEW_TASK
            targetActivity.startActivity(chooser)
            android.util.Log.d("EmailIntent", "Activity started with Activity context")
        } else {
            // Non-Activity context, need flag
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            android.util.Log.d("EmailIntent", "Activity started with context + flag")
        }
    } catch (e: android.content.ActivityNotFoundException) {
        android.util.Log.e("EmailIntent", "ActivityNotFoundException: ${e.message}", e)
        // Try alternative with plain text if no email app found
        val alternativeIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ice Cream Shop - Order Confirmation")
            putExtra(Intent.EXTRA_TEXT, "To: tony.p.doan@gmail.com, Jacqueterrell@gmail.com\n\n$emailBody")
        }
        try {
            val chooser = Intent.createChooser(alternativeIntent, "Share order details using...")
            val targetActivity = activity ?: (context as? Activity)
            if (targetActivity != null) {
                targetActivity.startActivity(chooser)
            } else {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (ex: Exception) {
            Toast.makeText(context, "No email or sharing app found. Error: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("EmailIntent", "Exception sending email: ${e.message}", e)
        Toast.makeText(context, "Error sending email: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}
