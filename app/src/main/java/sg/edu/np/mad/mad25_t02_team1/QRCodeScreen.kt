package sg.edu.np.mad.mad25_t02_team1

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QRCodeScreen() {
    val activity = LocalContext.current as ComponentActivity
    val context = LocalContext.current

    // Get ticket details from intent
    val bookingId = activity.intent.getStringExtra("BOOKING_ID") ?: ""
    val eventName = activity.intent.getStringExtra("EVENT_NAME") ?: ""
    val artist = activity.intent.getStringExtra("ARTIST") ?: ""
    val venue = activity.intent.getStringExtra("VENUE") ?: ""
    val dateMillis = activity.intent.getLongExtra("DATE_MILLIS", 0L)
    val category = activity.intent.getStringExtra("CATEGORY") ?: ""
    val section = activity.intent.getStringExtra("SECTION") ?: ""
    val quantity = activity.intent.getIntExtra("QUANTITY", 1)
    val pricePerTicket = activity.intent.getDoubleExtra("PRICE_PER_TICKET", 0.0)

    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    // State for QR code and countdown
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var secondsRemaining by remember { mutableStateOf(60) }
    var currentToken by remember { mutableStateOf("") }

    // Format date
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val eventDate = if (dateMillis > 0) dateFormat.format(Date(dateMillis)) else "N/A"

    // Generate QR code every 60 seconds
    LaunchedEffect(Unit) {
        while (true) {
            // Generate new token
            val timestamp = System.currentTimeMillis()
            val tokenData = "$bookingId:$userEmail:$timestamp"
            currentToken = generateSecureToken(tokenData)

            val verificationUrl = "https://natalie-s10257861.github.io/MAD_Assignment2_TicketLahVerification/?"  +
                    "eventName=${eventName.replace(" ", "%20")}&" +
                    "artist=${artist.replace(" ", "%20")}&" +
                    "venue=${venue.replace(" ", "%20")}&" +
                    "date=${eventDate.replace(" ", "%20")}&" +
                    "category=$category&" +
                    "section=$section&" +
                    "quantity=$quantity&" +
                    "price=$pricePerTicket&" +
                    "email=$userEmail&" +
                    "bookingId=$bookingId&" +
                    "token=$currentToken"

            // Generate QR code
            qrBitmap = generateQRCode(verificationUrl, 512, 512)

            // Countdown from 60 to 0
            for (i in 60 downTo 1) {
                secondsRemaining = i
                delay(1000L)
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                TicketLahHeader()
                IconButton(
                    onClick = { (context as? ComponentActivity)?.finish() ?: Unit },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Event Name Title
            Text(
                text = eventName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ticket Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QRTicketInfoRow("Artist", artist)
                    QRTicketInfoRow("Date", eventDate)
                    QRTicketInfoRow("Venue", venue)
                    QRTicketInfoRow("Category", category)
                    QRTicketInfoRow("Section", section)
                    QRTicketInfoRow("Quantity", quantity.toString())
                    QRTicketInfoRow("Email", userEmail)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Ticket QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } ?: CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Text - Simple and minimal
            Text(
                text = "Refreshes in $secondsRemaining seconds",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QRTicketInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun TicketInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

fun generateQRCode(content: String, width: Int, height: Int): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.MARGIN, 1)
    }

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

fun generateSecureToken(data: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }.take(32)
}