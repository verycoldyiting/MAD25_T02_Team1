package sg.edu.np.mad.mad25_t02_team1.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.Timestamp
import sg.edu.np.mad.mad25_t02_team1.models.Booking
import sg.edu.np.mad.mad25_t02_team1.models.Event
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen() {
    var bookingWithEvents by remember { mutableStateOf<List<Pair<Booking, Event?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // TEMP: default account id for testing. Replace with FirebaseAuth currentUser?.uid later.
    val accountId = "A001"

    LaunchedEffect(accountId) {
        val db = FirebaseFirestore.getInstance()
        val accountRef = db.document("/Account/$accountId")

        db.collection("BookingDetails")
            .whereEqualTo("AccID", accountRef) // Corrected field name
            .get()
            .addOnSuccessListener { bookingSnapshot ->
                val bookingList: List<Booking> = bookingSnapshot.map { doc ->
                    doc.toObject(Booking::class.java).copy(id = doc.id)
                }

                if (bookingList.isEmpty()) {
                    bookingWithEvents = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                // Prepare tasks to fetch Event docs
                val eventTasks: List<Task<DocumentSnapshot>> = bookingList.map { booking ->
                    val eventId: String = booking.eventId ?: ""
                    db.collection("Events").document(eventId).get()
                }

                // Wait for all event fetch tasks to finish
                Tasks.whenAllSuccess<DocumentSnapshot>(eventTasks)
                    .addOnSuccessListener { results ->
                        // results are DocumentSnapshot in the same order as eventTasks
                        val paired: MutableList<Pair<Booking, Event?>> = mutableListOf()
                        for (i in bookingList.indices) {
                            val booking = bookingList[i]
                            val docSnapshot = results.getOrNull(i)
                            val event: Event? = docSnapshot?.toObject(Event::class.java)?.copy(id = docSnapshot.id)
                            paired.add(booking to event)
                        }
                        bookingWithEvents = paired
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookingHistory", "Failed to fetch events", e)
                        // Still map bookings without events
                        bookingWithEvents = bookingList.map { it to null }
                        isLoading = false
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BookingHistory", "Failed to fetch bookings", e)
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking History", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                bookingWithEvents.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No booking history found.")
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                        items(bookingWithEvents) { pair ->
                            BookingHistoryItem(pair.first, pair.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingHistoryItem(booking: Booking, event: Event?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { /* optionally navigate to booking detail */ },
        shape = RoundedCornerShape(16.dp), // Increased roundness for modern look
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Use a slight color variation
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Stronger shadow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Image: prefer event.eventImage, fallback to booking.eventImage
            val rawImageUrl = event?.eventImage ?: booking.eventImage
            EventImage(rawUrl = rawImageUrl)

            Spacer(modifier = Modifier.height(16.dp))

            // Event title
            Text(
                text = event?.name ?: booking.concertTitle ?: booking.name ?: "Unknown Event",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Artist
            if (!event?.artist.isNullOrEmpty()) {
                Text(
                    text = event!!.artist ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event Date and Venue (with Icons)
            event?.date?.let { ts: Timestamp ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = "Event Date", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timestampToString(ts), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (!event?.venue.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Venue", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = event!!.venue ?: "", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Booking details in two columns using a Surface for emphasis
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        DetailText("Category:", booking.category ?: "N/A")
                        DetailText("Seat:", booking.section ?: "N/A")
                        DetailText("Quantity:", (booking.quantity ?: 0).toString())
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        DetailText("Payment:", booking.paymentMethod ?: "N/A")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total: $${"%.2f".format(booking.totalPrice ?: 0.0)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error // Highlight the price
                        )
                    }
                }
            }

            booking.purchaseTime?.let { ts ->
                Spacer(modifier = Modifier.height(12.dp))
                Text("Purchased: ${timestampToString(ts)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DetailText(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EventImage(rawUrl: String?) {
    // Show placeholder if there is no URL
    var displayUrl by remember { mutableStateOf<String?>(null) }
    var loadingError by remember { mutableStateOf(false) }

    LaunchedEffect(rawUrl) {
        loadingError = false
        displayUrl = null
        if (rawUrl.isNullOrBlank()) return@LaunchedEffect

        val cleanedUrl = rawUrl.trim()

        // If it's a gs:// storage path, convert to https downloadUrl
        if (cleanedUrl.startsWith("gs://")) {
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(cleanedUrl)
                // FIX: Use suspending await()
                val uriResult = storageRef.downloadUrl.await()
                displayUrl = uriResult.toString()
            } catch (ex: Exception) {
                Log.w("EventImage", "Failed to resolve gs:// url: $cleanedUrl", ex)
                loadingError = true
            }
        } else {
            // Assume it's already an http/https url
            displayUrl = cleanedUrl
        }
    }

    val painter = rememberAsyncImagePainter(displayUrl)

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (displayUrl == null && !loadingError) {
            // loading placeholder
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (loadingError) {
            // fallback placeholder UI
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                Text("Image unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun timestampToString(ts: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val date: Date = ts.toDate()
    return sdf.format(date)
}