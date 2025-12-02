package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PaymentPage : ComponentActivity() {
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

    // Card fields
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // Billing fields
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

    // UI state
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Money
    val subtotal = quantity * pricePerTicket
    val bookingFee = if (subtotal == 0.0) 0.0 else 3.5
    val finalTotal = (subtotal + bookingFee - discountAmount).coerceAtLeast(0.0)

    val dateText = if (dateMillis == 0L) {
        "Date TBA"
    } else {
        SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(dateMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(
                text = "Payment",
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
                onPromoInputChange = { promoInput = it },
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
                        promoError = "Invalid or ineligible code"
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            CardDetailsSection(
                cardName = cardName,
                onCardNameChange = { cardName = it },
                cardNumber = cardNumber,
                onCardNumberChange = { cardNumber = it },
                expiryMonth = expiryMonth,
                onExpiryMonthChange = { expiryMonth = it },
                expiryYear = expiryYear,
                onExpiryYearChange = { expiryYear = it },
                cvv = cvv,
                onCvvChange = { cvv = it }
            )

            Spacer(Modifier.height(12.dp))

            BillingAddressSection(
                billingName = billingName,
                onBillingNameChange = { billingName = it },
                addressLine1 = addressLine1,
                onAddressLine1Change = { addressLine1 = it },
                postalCode = postalCode,
                onPostalCodeChange = { postalCode = it },
                city = city,
                onCityChange = { city = it },
                country = country,
                onCountryChange = { country = it }
            )

            Spacer(Modifier.height(24.dp))

            Column {

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                PrimaryGradientButton(
                    text = "Buy now • \$${String.format("%.2f", finalTotal)}",
                    enabled = !isProcessing
                ) {

                    if (!validateCard(cardName, cardNumber, expiryMonth, expiryYear, cvv)) {
                        Toast.makeText(context, "Check your card details", Toast.LENGTH_SHORT).show()
                        return@PrimaryGradientButton
                    }

                    if (billingName.isBlank() || addressLine1.isBlank() || postalCode.isBlank()) {
                        Toast.makeText(context, "Billing address required", Toast.LENGTH_SHORT).show()
                        return@PrimaryGradientButton
                    }

                    isProcessing = true

                    val bookingId = generateBookingId()

                    val bookingData = hashMapOf(
                        "BookingId" to bookingId,
                        "EventId" to eventId,
                        "Title" to title,
                        "Artist" to artist,
                        "Date" to Timestamp(Date(dateMillis)),
                        "Venue" to venue,
                        "Category" to category,
                        "Section" to section,
                        "Quantity" to quantity,
                        "PricePerTicket" to pricePerTicket,
                        "Subtotal" to subtotal,
                        "BookingFee" to bookingFee,
                        "DiscountAmount" to discountAmount,
                        "TotalPrice" to finalTotal,
                        "PaymentMethod" to "Card",
                        "CardLast4" to cardNumber.takeLast(4),
                        "BillingName" to billingName,
                        "BillingAddress" to addressLine1,
                        "PostalCode" to postalCode,
                        "City" to city,
                        "Country" to country,
                        "AppliedPromo" to appliedPromo,
                        "CreatedAt" to Timestamp.now()
                    )

                    db.collection("BookingDetails")
                        .document(bookingId)
                        .set(bookingData)
                        .addOnSuccessListener {
                            isProcessing = false
                            showSuccessDialog = true
                        }
                        .addOnFailureListener { ex ->
                            isProcessing = false
                            Toast.makeText(context, "Error: ${ex.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                }

                Spacer(Modifier.height(8.dp))

                SecondaryOutlineButton(
                    text = "Cancel",
                    enabled = !isProcessing
                ) {
                    activity?.finish()
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
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
                text = { Text("Your tickets are confirmed. View them in Booking Details.") }
            )
        }
    }
}

/* ------------------------------------------------------------------
   Reusable Components (NO CHANGES NEEDED)
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
            Divider()
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column { Text("Category", color = Color.Gray, fontSize = 12.sp); Text(category) }
                Column { Text("Section", color = Color.Gray, fontSize = 12.sp); Text(section) }
                Column { Text("Qty", color = Color.Gray, fontSize = 12.sp); Text(quantity.toString()) }
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
                    Text(if (appliedPromo != null) "Applied" else "Apply")
                }
            }

            if (appliedPromo != null && promoError == null)
                Text("Applied: $appliedPromo", color = Color(0xFF16A34A))

            if (promoError != null)
                Text(promoError, color = Color(0xFFDC2626))
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
    Card(
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("Card details", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = cardName,
                onValueChange = onCardNameChange,
                label = { Text("Name on card") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cardNumber,
                onValueChange = {
                    onCardNumberChange(it.filter { ch -> ch.isDigit() }.take(16))
                },
                label = { Text("Card number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = expiryMonth,
                    onValueChange = { if (it.length <= 2) onExpiryMonthChange(it.filter { d -> d.isDigit() }) },
                    label = { Text("MM") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = expiryYear,
                    onValueChange = { if (it.length <= 2) onExpiryYearChange(it.filter { d -> d.isDigit() }) },
                    label = { Text("YY") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 3) onCvvChange(it.filter { d -> d.isDigit() }) },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
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
                    onValueChange = { if (it.length <= 6) onPostalCodeChange(it.filter { c -> c.isDigit() }) },
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2563EB),
                            Color(0xFF4F46E5)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
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
