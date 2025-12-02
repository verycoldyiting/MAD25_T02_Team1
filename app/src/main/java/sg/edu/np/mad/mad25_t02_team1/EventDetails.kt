package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.Event
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("EVENT_ID") ?: ""

        setContent {
            MAD25_T02_Team1Theme {
                EventDetailsScreen(
                    eventId = eventId,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@Composable
fun EventDetailsScreen(
    eventId: String,
    onBackPressed: () -> Unit
) {
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var loadingImage by remember { mutableStateOf(true) }

    // Fetch Event
    LaunchedEffect(eventId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("Events")
                .document(eventId)
                .get()
                .await()

            event = doc.toObject(Event::class.java)
        } catch (e: Exception) {
            Log.e("EventDetails", "Error: ${e.message}")
        }
        isLoading = false
    }

    // Load event image
    LaunchedEffect(event?.eventImage) {
        val raw = event?.eventImage ?: ""
        if (raw.isEmpty()) {
            loadingImage = false
            return@LaunchedEffect
        }

        loadingImage = true

        imageUrl = try {
            if (raw.startsWith("gs://")) {
                FirebaseStorage.getInstance().getReferenceFromUrl(raw)
                    .downloadUrl.await().toString()
            } else raw
        } catch (e: Exception) {
            Log.e("ImageLoad", "Error loading image: ${e.message}")
            null
        }

        loadingImage = false
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TicketLahHeader()

                // Beautiful Back Button
                IconButton(
                    onClick = onBackPressed,
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
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                event != null -> EventDetailsContent(
                    event = event!!,
                    imageUrl = imageUrl,
                    loadingImage = loadingImage
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Event not found", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun EventDetailsContent(
    event: Event,
    imageUrl: String?,
    loadingImage: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {

        // IMAGE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                loadingImage -> CircularProgressIndicator()
                imageUrl != null -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = event.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> Text("No Image Available", color = Color.Gray)
            }
        }

        // MAIN CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = event.name ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = event.description ?: event.caption ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))

                // Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "",
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Date", color = Color.Gray)
                        Text(formatDate(event.date))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Divider()

                Spacer(Modifier.height(16.dp))

                // Venue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "",
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Venue", color = Color.Gray)
                        Text(event.venue ?: "Venue TBA")
                    }

                    OutlinedButton(onClick = {}) {
                        Text("See On Maps", fontSize = 12.sp)
                    }
                }
            }
        }

        // BUY BUTTON
        Button(
            onClick = {
                val intent = Intent(context, BuyTicketActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("Buy Tickets")
        }

        Spacer(Modifier.height(16.dp))

        // Extra info
        if (event.artist != null || event.genre != null || event.price != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("Event Information", fontWeight = FontWeight.Bold)

                    event.artist?.let {
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Artist", it)
                    }

                    event.genre?.let {
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Genre", it)
                    }

                    event.price?.let {
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Starting Price", "$${"%.2f".format(it)}")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value)
    }
}

fun formatDate(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return "Date TBA"
    return SimpleDateFormat("EEE, MMM dd yyyy h:mm a", Locale.getDefault())
        .format(ts.toDate())
}
