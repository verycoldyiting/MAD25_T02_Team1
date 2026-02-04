package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PaymentPage : ComponentActivity() {

    private lateinit var paymentSheet: PaymentSheet
    private var onStripeSuccess: (() -> Unit)? = null
    private var onStripeFailure: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("eventId") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val dateMillis = intent.getLongExtra("dateMillis", 0L)
        val venue = intent.getStringExtra("venue") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val section = intent.getStringExtra("section") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)
        val pricePerTicket = intent.getDoubleExtra("pricePerTicket", 0.0)

        PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY)

        paymentSheet = PaymentSheet(this) { result ->
            when (result) {
                is PaymentSheetResult.Completed -> onStripeSuccess?.invoke()
                is PaymentSheetResult.Canceled -> onStripeFailure?.invoke("Payment cancelled")
                is PaymentSheetResult.Failed -> onStripeFailure?.invoke(
                    result.error.localizedMessage ?: "Payment failed"
                )
            }
        }

        setContent {
            MAD25_T02_Team1Theme {
                Surface {
                    PaymentScreen(
                        eventId = eventId,
                        title = title,
                        artist = artist,
                        dateMillis = dateMillis,
                        venue = venue,
                        category = category,
                        section = section,
                        quantity = quantity,
                        pricePerTicket = pricePerTicket,
                        startStripePayment = { clientSecret, onSuccess, onFailure ->
                            onStripeSuccess = onSuccess
                            onStripeFailure = onFailure

                            paymentSheet.presentWithPaymentIntent(
                                clientSecret,
                                PaymentSheet.Configuration(merchantDisplayName = "TicketLah")
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(
    eventId: String,
    title: String,
    artist: String,
    dateMillis: Long,
    venue: String,
    category: String,
    section: String,
    quantity: Int,
    pricePerTicket: Double,
    startStripePayment: (clientSecret: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val FUNCTIONS_REGION = "asia-southeast1"
    val functions = remember { FirebaseFunctions.getInstance(FUNCTIONS_REGION) }

    val scope = rememberCoroutineScope()

    var uid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            uid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var accountDocId by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var accountLoading by remember { mutableStateOf(true) }

    var paymentError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val initialInputError = remember(eventId, quantity, pricePerTicket) {
        when {
            eventId.isBlank() -> "Missing event information. Please go back and select the event again."
            quantity <= 0 -> "Invalid quantity selected. Please go back and reselect your tickets."
            pricePerTicket < 0.0 -> "Invalid ticket price. Please try again later."
            else -> null
        }
    }


    LaunchedEffect(uid) {
        accountLoading = true
        accountError = null
        accountDocId = null

        val safeUid = uid
        if (safeUid.isNullOrBlank()) {
            accountError = "Login required."
            accountLoading = false
            return@LaunchedEffect
        }

        try {
            val snap = db.collection("Account")
                .whereEqualTo("uid", safeUid)
                .limit(1)
                .get()
                .await()

            if (snap.isEmpty) {
                accountError = "Account not found. Please contact support."
            } else {
                accountDocId = snap.documents.first().id
            }
        } catch (e: Exception) {
            accountError = "Failed to load account: ${e.message ?: "Unknown error"}"
        } finally {
            accountLoading = false
        }
    }

    // Billing address
    var billingName by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Singapore") }

    // Promo
    var promoInput by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<String?>(null) }
    var discountAmount by remember { mutableStateOf(0.0) }
    var promoError by remember { mutableStateOf<String?>(null) }

    val subtotal = quantity * pricePerTicket
    val bookingFee = if (subtotal == 0.0) 0.0 else 3.5
    val finalTotal = (subtotal + bookingFee - discountAmount).coerceAtLeast(0.0)

    val dateText = if (dateMillis == 0L) {
        "Date TBA"
    } else {
        SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(dateMillis))
    }

    suspend fun fetchClientSecret(amountCents: Int, bookingId: String): String {
        val user = auth.currentUser ?: throw Exception(
            "Login required (FirebaseAuth currentUser is null)."
        )

        user.getIdToken(true).await()

        val data = hashMapOf(
            "amount" to amountCents,
            "currency" to "sgd",
            "eventId" to eventId,
            "bookingId" to bookingId
        )

        try {
            val result = functions
                .getHttpsCallable("createPaymentIntent")
                .call(data)
                .await()

            val map = result.data as Map<*, *>
            return map["clientSecret"] as String
        } catch (e: FirebaseFunctionsException) {
            val msg = when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    "NOT_FOUND: Function not deployed OR wrong region. Make sure Android region = $FUNCTIONS_REGION and function exists."
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "UNAUTHENTICATED: Function received no auth. Usually emulator mismatch or wrong Firebase project."
                else -> e.message ?: "Function error."
            }
            throw Exception(msg)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                TicketLahHeader()

                IconButton(
                    onClick = { activity?.finish() },
                    modifier = Modifier
                        .padding(start = 10.dp, top = 20.dp)
                        .size(60.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(24.dp)
                    )
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF2F4F7))
        ) {
            when {
                initialInputError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(initialInputError, color = Color.Red, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        SecondaryOutlineButton("Go back", true) { activity?.finish() }
                    }
                }

                accountLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                accountError != null || accountDocId.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            accountError ?: "Unable to load account.",
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        SecondaryOutlineButton("Close", true) { activity?.finish() }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Payment", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(8.dp))

                        EventSummaryCard(
                            title = title,
                            artist = artist,
                            dateText = dateText,
                            venue = venue,
                            category = category,
                            section = section,
                            quantity = quantity
                        )

                        Spacer(Modifier.height(12.dp))

                        PriceCardWithPromo(
                            subtotal = subtotal,
                            bookingFee = bookingFee,
                            discountAmount = discountAmount,
                            finalTotal = finalTotal,
                            promoInput = promoInput,
                            onPromoInputChange = { promoInput = it.uppercase() },
                            appliedPromo = appliedPromo,
                            promoError = promoError,
                            onApplyPromo = {
                                val result = evaluatePromoCode(promoInput.trim(), subtotal)
                                if (result > 0) {
                                    appliedPromo = promoInput.trim().uppercase()
                                    discountAmount = result
                                    promoError = null
                                } else {
                                    appliedPromo = null
                                    discountAmount = 0.0
                                    promoError = "Invalid promo code"
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        BillingAddressSection(
                            billingName, { billingName = it },
                            addressLine1, { addressLine1 = it },
                            postalCode, { postalCode = it },
                            city, { city = it },
                            country, { country = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        if (paymentError != null) {
                            Text(paymentError ?: "", color = Color(0xFFDC2626), fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                        }

                        if (isProcessing) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                        }

                        PrimaryGradientButton(
                            text = "Pay with Stripe • \$${String.format("%.2f", finalTotal)}",
                            enabled = !isProcessing
                        ) {
                            scope.launch {
                                try {
                                    paymentError = null
                                    isProcessing = true

                                    if (billingName.isBlank() || addressLine1.isBlank() || postalCode.isBlank()) {
                                        isProcessing = false
                                        Toast.makeText(context, "Billing address required", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val accDocId = accountDocId!!
                                    val bookingId = generateBookingId()
                                    val amountCents = (finalTotal * 100).toInt().coerceAtLeast(50)

                                    val clientSecret = fetchClientSecret(amountCents, bookingId)

                                    isProcessing = false


                                    startStripePayment(
                                        clientSecret,
                                        {
                                            scope.launch {
                                                try {
                                                    isProcessing = true

                                                    val eventTime = if (dateMillis == 0L) {
                                                        Timestamp.now()
                                                    } else {
                                                        Timestamp(Date(dateMillis))
                                                    }

                                                    val bookingData = hashMapOf(
                                                        "AccID" to db.document("Account/$accDocId"),
                                                        "Category" to category,
                                                        "ConcertTitle" to title,
                                                        "EventID" to eventId,
                                                        "EventTime" to eventTime,
                                                        "Name" to billingName,
                                                        "PaymentMethod" to "Stripe",
                                                        "PurchaseTime" to Timestamp.now(),
                                                        "Quantity" to quantity,
                                                        "Section" to section,
                                                        "TotalPrice" to finalTotal,
                                                        "PaymentStatus" to "Paid"
                                                    )

                                                    db.collection("BookingDetails")
                                                        .document(bookingId)
                                                        .set(bookingData)
                                                        .await()

                                                    isProcessing = false
                                                    showSuccessDialog = true
                                                } catch (e: Exception) {
                                                    isProcessing = false
                                                    paymentError = "Paid but failed to save booking: ${e.message}"
                                                }
                                            }
                                        },
                                        { msg ->
                                            paymentError = msg
                                            isProcessing = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    isProcessing = false
                                    paymentError = e.message ?: "Failed to start payment."
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        SecondaryOutlineButton("Cancel", !isProcessing) { activity?.finish() }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (showSuccessDialog) {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showSuccessDialog = false
                                        context.startActivity(Intent(context, BookingHistoryActivity::class.java))
                                        activity?.finish()
                                    }
                                ) { Text("View Booking") }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showSuccessDialog = false
                                        activity?.finish()
                                    }
                                ) { Text("Close") }
                            },
                            title = { Text("Payment Successful") },
                            text = { Text("Your tickets are confirmed.") }
                        )
                    }
                }
            }
        }
    }
}

// UI Components

@Composable
private fun EventSummaryCard(
    title: String,
    artist: String,
    dateText: String,
    venue: String,
    category: String,
    section: String,
    quantity: Int
) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (artist.isNotBlank()) Text(artist, color = Color.Gray)
            Text(dateText)
            Text(venue)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Category", color = Color.Gray, fontSize = 12.sp)
                    Text(category)
                }
                Column {
                    Text("Section", color = Color.Gray, fontSize = 12.sp)
                    Text(section)
                }
                Column {
                    Text("Qty", color = Color.Gray, fontSize = 12.sp)
                    Text(quantity.toString())
                }
            }
        }
    }
}

@Composable
private fun PriceCardWithPromo(
    subtotal: Double,
    bookingFee: Double,
    discountAmount: Double,
    finalTotal: Double,
    promoInput: String,
    onPromoInputChange: (String) -> Unit,
    appliedPromo: String?,
    promoError: String?,
    onApplyPromo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Price breakdown", fontWeight = FontWeight.SemiBold)

            PriceRow("Tickets", subtotal)
            PriceRow("Booking Fee", bookingFee)
            if (discountAmount > 0) PriceRow("Discount", -discountAmount, highlight = true)

            Divider(Modifier.padding(vertical = 4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("\$${String.format("%.2f", finalTotal)}", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Text("Promo code", fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = promoInput,
                    onValueChange = { onPromoInputChange(it.uppercase()) },
                    label = { Text("Enter code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = onApplyPromo, enabled = promoInput.isNotBlank()) {
                    Text(if (appliedPromo != null && promoError == null) "Applied" else "Apply")
                }
            }

            if (appliedPromo != null && promoError == null) {
                Text("Applied: $appliedPromo", color = Color(0xFF16A34A))
            }
            if (promoError != null) {
                Text(promoError, color = Color(0xFFDC2626))
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: Double, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (highlight) Color.Red else Color.Gray)
        val prefix = if (value < 0) "-$" else "$"
        Text(prefix + String.format("%.2f", kotlin.math.abs(value)))
    }
}

@Composable
private fun BillingAddressSection(
    billingName: String,
    onBillingNameChange: (String) -> Unit,
    addressLine1: String,
    onAddressLine1Change: (String) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Billing address", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = billingName,
                onValueChange = onBillingNameChange,
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = addressLine1,
                onValueChange = onAddressLine1Change,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = {
                        if (it.length <= 6) onPostalCodeChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("Postal code") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = country,
                onValueChange = onCountryChange,
                label = { Text("Country") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PrimaryGradientButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD300),
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryOutlineButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
    }
}

// Helpers

private fun generateBookingId(): String {
    val prefix = "BK"
    val randomPart = (100000..999999).random(Random(System.currentTimeMillis()))
    return "$prefix$randomPart"
}

private fun evaluatePromoCode(code: String, subtotal: Double): Double {
    if (subtotal <= 0) return 0.0
    return when (code.uppercase()) {
        "TICKET10" -> subtotal * 0.10
        "STUDENT5" -> 5.0
        else -> 0.0
    }
}
