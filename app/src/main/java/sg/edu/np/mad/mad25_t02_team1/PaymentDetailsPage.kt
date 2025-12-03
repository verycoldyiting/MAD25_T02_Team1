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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PaymentPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Receive all event details passed from previous page
        val eventId = intent.getStringExtra("eventId") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val dateMillis = intent.getLongExtra("dateMillis", 0L)
        val venue = intent.getStringExtra("venue") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val section = intent.getStringExtra("section") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)
        val pricePerTicket = intent.getDoubleExtra("pricePerTicket", 0.0)

        // Load UI
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
                        pricePerTicket = pricePerTicket
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
    pricePerTicket: Double
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    // Firestore account info
    var accountId by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var accountLoading by remember { mutableStateOf(true) }

    // Error shown when payment fails
    var paymentError by remember { mutableStateOf<String?>(null) }

    // Validate the event data passed through Intent
    val initialInputError = remember(eventId, quantity, pricePerTicket) {
        when {
            eventId.isBlank() -> "Missing event information. Please go back and select the event again."
            quantity <= 0 -> "Invalid quantity selected. Please go back and reselect your tickets."
            pricePerTicket < 0.0 -> "Invalid ticket price. Please try again later."
            else -> null
        }
    }

    // Load account from Firestore
    LaunchedEffect(Unit) {
        try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                accountError = "You are not logged in. Please log in again."
                return@LaunchedEffect
            }

            val snapshot = db.collection("Account")
                .whereEqualTo("uid", uid)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                // Get "accountId" field from Firestore document
                val doc = snapshot.documents.first()
                accountId = doc.getString("accountId")
                if (accountId.isNullOrBlank()) {
                    accountError = "Account data is incomplete. Please contact support."
                }
            } else {
                accountError = "Your account could not be found. Please contact support."
            }
        } catch (e: Exception) {
            accountError = "Failed to load account. ${e.message ?: "Unknown error."}"
        } finally {
            accountLoading = false
        }
    }

    // Card inputs
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // Billing address inputs
    var billingName by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Singapore") }

    // Promo code state
    var promoInput by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<String?>(null) }
    var discountAmount by remember { mutableStateOf(0.0) }
    var promoError by remember { mutableStateOf<String?>(null) }

    // UI state
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Money calculation
    val subtotal = quantity * pricePerTicket
    val bookingFee = if (subtotal == 0.0) 0.0 else 3.5
    val finalTotal = (subtotal + bookingFee - discountAmount).coerceAtLeast(0.0)

    // Format event date
    val dateText = if (dateMillis == 0L) {
        "Date TBA"
    } else {
        SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(dateMillis))
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                TicketLahHeader()

                // Back button
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
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF2F4F7))
        ) {
            when {
                // 1. Incorrect data coming from previous page
                initialInputError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = initialInputError,
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        SecondaryOutlineButton(
                            text = "Go back",
                            enabled = true
                        ) {
                            activity?.finish()
                        }
                    }
                }

                // 2. Loading Firestore account
                accountLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // 3. Account not found or error
                accountError != null || accountId.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = accountError ?: "Unable to load account.",
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        SecondaryOutlineButton(
                            text = "Close",
                            enabled = true
                        ) {
                            activity?.finish()
                        }
                    }
                }

                // 4. Normal payment content
                else -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            "Payment",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Event summary card
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

                        // Price breakdown + promo code
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


                        // Card details section
                        CardDetailsSection(
                            cardName, { cardName = it },
                            cardNumber, { cardNumber = it },
                            expiryMonth, { expiryMonth = it },
                            expiryYear, { expiryYear = it },
                            cvv, { cvv = it }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Billing address
                        BillingAddressSection(
                            billingName, { billingName = it },
                            addressLine1, { addressLine1 = it },
                            postalCode, { postalCode = it },
                            city, { city = it },
                            country, { country = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        // Show payment error message if any
                        if (paymentError != null) {
                            Text(
                                text = paymentError ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (isProcessing) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        PrimaryGradientButton(
                            text = "Buy now • \$${String.format("%.2f", finalTotal)}",
                            enabled = !isProcessing
                        ) {
                            // Validate card inputs
                            if (!validateCard(cardName, cardNumber, expiryMonth, expiryYear, cvv)) {
                                Toast.makeText(context, "Check your card details", Toast.LENGTH_SHORT).show()
                                return@PrimaryGradientButton
                            }

                            // Validate billing address
                            if (billingName.isBlank() || addressLine1.isBlank() || postalCode.isBlank()) {
                                Toast.makeText(context, "Billing address required", Toast.LENGTH_SHORT).show()
                                return@PrimaryGradientButton
                            }

                            // Firestore must have account loaded
                            if (accountId.isNullOrBlank()) {
                                paymentError = "Your account is not available. Please try again."
                                return@PrimaryGradientButton
                            }
                            // Start Firestore booking write
                            scope.launch {
                                isProcessing = true
                                paymentError = null
                                try {
                                    val bookingId = generateBookingId()

                                    // Convert event data to Firestore timestamp
                                    val eventTime = if (dateMillis == 0L) {
                                        Timestamp.now()
                                    } else {
                                        Timestamp(Date(dateMillis))
                                    }
                                    // Create Firestore booking document
                                    val bookingData = hashMapOf(
                                        "AccID" to db.document("Account/$accountId"),
                                        "Category" to category,
                                        "ConcertTitle" to title,
                                        "EventID" to eventId,
                                        "EventTime" to eventTime,
                                        "Name" to cardName,
                                        "PaymentMethod" to "Card",
                                        "PurchaseTime" to Timestamp.now(),
                                        "Quantity" to quantity,
                                        "Section" to section,
                                        "TotalPrice" to finalTotal
                                    )

                                    //Save document

                                    db.collection("BookingDetails")
                                        .document(bookingId)
                                        .set(bookingData)
                                        .await()

                                    isProcessing = false
                                    showSuccessDialog = true
                                } catch (ex: Exception) {
                                    isProcessing = false
                                    paymentError = ex.message ?: "Payment failed. Please try again."
                                    Toast.makeText(
                                        context,
                                        "Payment failed. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        SecondaryOutlineButton("Cancel", !isProcessing) {
                            activity?.finish()
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    // Success dialog
                    if (showSuccessDialog) {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showSuccessDialog = false
                                        val bookingIntent = Intent(
                                            context,
                                            BookingHistoryActivity::class.java
                                        )
                                        context.startActivity(bookingIntent)
                                        activity?.finish()
                                    }
                                ) {
                                    Text("View Booking")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showSuccessDialog = false
                                        activity?.finish()
                                    }
                                ) {
                                    Text("Close")
                                }
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

/* ------------------------------------------------------------------
   Reusable Components
------------------------------------------------------------------ */

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

            Divider(Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (highlight) Color.Red else Color.Gray)
        val prefix = if (value < 0) "-$" else "$"
        Text(prefix + String.format("%.2f", kotlin.math.abs(value)))
    }
}

@Composable
private fun CardDetailsSection(
    cardName: String,
    onCardNameChange: (String) -> Unit,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiryMonth: String,
    onExpiryMonthChange: (String) -> Unit,
    expiryYear: String,
    onExpiryYearChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit
) {

    var nameError by remember { mutableStateOf<String?>(null) }
    var numberError by remember { mutableStateOf<String?>(null) }
    var monthError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }
    var cvvError by remember { mutableStateOf<String?>(null) }

    fun validate() {

        // Name
        nameError = if (cardName.isBlank()) "Name cannot be empty" else null

        // Card number
        numberError = when {
            cardNumber.isBlank() -> "Card number cannot be empty"
            cardNumber.length < 16 -> "Card number must be 16 digits"
            else -> null
        }

        // Month
        monthError = when {
            expiryMonth.isBlank() -> "Required"
            expiryMonth.toIntOrNull() == null -> "Numbers only"
            expiryMonth.toInt() !in 1..12 -> "Enter a valid month"
            else -> null
        }

        // Year (with live card-expired check)
        yearError = when {
            expiryYear.isBlank() -> "Required"
            expiryYear.length != 2 -> "Must be 2 digits"
            expiryYear.toIntOrNull() == null -> "Numbers only"

            else -> {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
                val inputYear = expiryYear.toInt()

                if (inputYear < currentYear) "Card expired"
                else null
            }
        }

        // CVV
        cvvError = when {
            cvv.isBlank() -> "Required"
            cvv.length != 3 -> "CVV must be 3 digits"
            else -> null
        }
    }

    // Trigger validation whenever user types
    LaunchedEffect(cardName, cardNumber, expiryMonth, expiryYear, cvv) {
        validate()
    }

    Card(
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("Card details", fontWeight = FontWeight.SemiBold)

            // NAME
            OutlinedTextField(
                value = cardName,
                onValueChange = onCardNameChange,
                label = { Text("Name on card") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null
            )
            if (nameError != null)
                Text(nameError!!, color = Color.Red, fontSize = 12.sp)

            // CARD NUMBER
            OutlinedTextField(
                value = cardNumber,
                onValueChange = {
                    val clean = it.filter { ch -> ch.isDigit() }.take(16)
                    onCardNumberChange(clean)
                },
                label = { Text("Card number") },
                singleLine = true,
                isError = numberError != null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (numberError != null)
                Text(numberError!!, color = Color.Red, fontSize = 12.sp)

            // MONTH + YEAR + CVV FIELDS
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // MONTH
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = expiryMonth,
                        onValueChange = {
                            val clean = it.filter { d -> d.isDigit() }.take(2)
                            onExpiryMonthChange(clean)
                        },
                        label = { Text("MM") },
                        isError = monthError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (monthError != null)
                        Text(monthError!!, color = Color.Red, fontSize = 12.sp)
                }

                // YEAR
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = {
                            val clean = it.filter { d -> d.isDigit() }.take(2)
                            onExpiryYearChange(clean)
                        },
                        label = { Text("YY") },
                        isError = yearError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (yearError != null)
                        Text(yearError!!, color = Color.Red, fontSize = 12.sp)
                }

                // CVV
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            val clean = it.filter { d -> d.isDigit() }.take(3)
                            onCvvChange(clean)
                        },
                        label = { Text("CVV") },
                        isError = cvvError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (cvvError != null)
                        Text(cvvError!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        }
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
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
private fun PrimaryGradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD300), // YELLOW
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
private fun SecondaryOutlineButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
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

/* ------------------------------------------------------------------
   Helpers
------------------------------------------------------------------ */

private fun validateCard(
    name: String,
    number: String,
    month: String,
    year: String,
    cvv: String
): Boolean {

    if (name.isBlank()) return false
    if (number.length != 16) return false

    val m = month.toIntOrNull() ?: return false
    if (m !in 1..12) return false

    if (year.length != 2) return false
    val y = year.toIntOrNull() ?: return false

    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100

    // Reject if year already passed
    if (y < currentYear) return false

    if (cvv.length != 3) return false

    return true
}



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
