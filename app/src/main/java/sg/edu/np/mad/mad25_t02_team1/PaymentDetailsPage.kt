package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PaymentPage : ComponentActivity() {


    // Stripe PaymentSheet instance (Stripe SDK UI for collecting payment)
    private lateinit var paymentSheet: PaymentSheet

    // Lambdas to be set by Compose and invoked when Stripe returns a result
    private var onStripeSuccess: (() -> Unit)? = null
    private var onStripeFailure: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read booking details passed from previous screen
        val eventId = intent.getStringExtra("eventId") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val dateMillis = intent.getLongExtra("dateMillis", 0L)
        val venue = intent.getStringExtra("venue") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val section = intent.getStringExtra("section") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)
        val pricePerTicket = intent.getDoubleExtra("pricePerTicket", 0.0)

        // Stripe client-side init (Publishable key is safe to embed).
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

                        //startStripePayment is called by PaymentScreen once it has a clientSecret.
                        // It binds callbacks (onSuccess/onFailure), then presents Stripe PaymentSheet UI.
                        startStripePayment = { clientSecret, onSuccess, onFailure ->
                            onStripeSuccess = onSuccess
                            onStripeFailure = onFailure

                            paymentSheet.presentWithPaymentIntent(
                                clientSecret,
                                PaymentSheet.Configuration(
                                    merchantDisplayName = "TicketLah"
                                )
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

    // Firebase services (remember so they aren't recreated during recomposition)
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Must match the Cloud Functions deployment region
    val FUNCTIONS_REGION = "asia-southeast1"
    val functions = remember { FirebaseFunctions.getInstance(FUNCTIONS_REGION) }

    // Coroutine scope for async work (Firestore, Functions, Stripe)
    val scope = rememberCoroutineScope()

    // Snackbar host for error/status messages
    val snackbarHostState = remember { SnackbarHostState() }

    var uid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            uid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Account collection lookup results
    var accountDocId by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var accountLoading by remember { mutableStateOf(true) }


    // UI state flags
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Promo code UI state
    var promoInput by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<String?>(null) }
    var discountAmount by remember { mutableStateOf(0.0) }
    var promoError by remember { mutableStateOf<String?>(null) }


    // Price computation
    val subtotal = quantity * pricePerTicket
    val bookingFee = if (subtotal == 0.0) 0.0 else 3.5
    val finalTotal = (subtotal + bookingFee - discountAmount).coerceAtLeast(0.0)

    // Format display date
    val dateText = if (dateMillis == 0L) {
        "Date TBA"
    } else {
        SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(dateMillis))
    }

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

    // Calls Firebase Callable Function createPaymentIntent to get Stripe clientSecret.
    suspend fun fetchClientSecret(amountCents: Int, bookingId: String): String {
        val user = auth.currentUser ?: throw Exception("Login required (FirebaseAuth currentUser is null).")

        // Ensure valid auth token for callable function (request.auth in backend)
        user.getIdToken(true).await()

        // Payload sent to Cloud Function
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

    val canCheckout = initialInputError == null &&
            !accountLoading &&
            accountError == null &&
            !accountDocId.isNullOrBlank()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (canCheckout) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                formatSgd(finalTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Secure checkout powered by Stripe",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isProcessing = true

                                        val accDocId = accountDocId!!
                                        val bookingId = generateBookingId()
                                        val amountCents = (finalTotal * 100).toInt().coerceAtLeast(50)

                                        // Step 1 Get clientSecret from Cloud Function
                                        val clientSecret = fetchClientSecret(amountCents, bookingId)

                                        isProcessing = false

                                        // Step 2 Launch PaymentSheet using clientSecret
                                        startStripePayment(
                                            clientSecret,

                                            // Success callback (Stripe payment completed)
                                            {
                                                scope.launch {
                                                    try {
                                                        isProcessing = true

                                                        // Determine payer name for receipt/booking display
                                                        val payerName =
                                                            auth.currentUser?.displayName
                                                                ?: auth.currentUser?.email?.substringBefore("@")
                                                                ?: "User"

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
                                                            "Name" to payerName,
                                                            "PaymentMethod" to "Stripe",
                                                            "PurchaseTime" to Timestamp.now(),
                                                            "Quantity" to quantity,
                                                            "Section" to section,
                                                            "TotalPrice" to finalTotal,
                                                            "PaymentStatus" to "Paid"
                                                        )

                                                        // Step 3 Persist confirmed booking (only after successful payment)
                                                        db.collection("BookingDetails")
                                                            .document(bookingId)
                                                            .set(bookingData)
                                                            .await()

                                                        isProcessing = false
                                                        showSuccessDialog = true
                                                    } catch (e: Exception) {
                                                        isProcessing = false
                                                        snackbarHostState.showSnackbar(
                                                            "Paid but failed to save booking: ${e.message ?: "Unknown error"}"
                                                        )
                                                    }
                                                }
                                            },
                                            { msg ->
                                                isProcessing = false
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )

                                    } catch (e: Exception) {
                                        isProcessing = false
                                        snackbarHostState.showSnackbar(e.message ?: "Failed to start payment.")
                                    }
                                }
                            },
                            enabled = !isProcessing && finalTotal > 0,
                            modifier = Modifier.height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD300),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isProcessing) "Processing..." else "Pay with Stripe", fontWeight = FontWeight.SemiBold)
                        }
                    }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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

                // 4) Normal UI state: show event + pricing + promo controls
                else -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                            .padding(bottom = 96.dp)
                    ) {
                        Text(
                            "Payment",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
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
                                    scope.launch { snackbarHostState.showSnackbar("Promo applied!") }
                                } else {
                                    appliedPromo = null
                                    discountAmount = 0.0
                                    promoError = "Invalid promo code"
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Processing indicator while fetching clientSecret / saving booking
                        if (isProcessing) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                        }

                        // Cancel exits this screen
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
                                        val intent = Intent(context, HomePage::class.java).apply {
                                            putExtra("startRoute", BottomNavItem.Tickets.route)
                                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        }
                                        context.startActivity(intent)
                                        activity?.finish()

                                        context.startActivity(intent)
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

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
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

            Divider(Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(formatSgd(finalTotal), fontWeight = FontWeight.Bold)
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
        val prefix = if (value < 0) "- " else ""
        Text(prefix + formatSgd(kotlin.math.abs(value)))
    }
}

@Composable
private fun SecondaryOutlineButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
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

private fun formatSgd(amount: Double): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "SG"))
    nf.currency = Currency.getInstance("SGD")
    return nf.format(amount)
}
