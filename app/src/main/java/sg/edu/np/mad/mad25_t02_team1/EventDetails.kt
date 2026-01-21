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
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW
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
                    onBackPressed = { finish() } // Closes activity when back is pressed }
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
    var imageError by remember { mutableStateOf<String?>(null) }

    // Fetch Event details from Firestore based on eventId
    LaunchedEffect(eventId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("Events")
                .document(eventId)
                .get()
                .await()

            event = doc.toObject(Event::class.java)
            Log.d("EventDetails", "Event loaded: ${event?.name}, Image: ${event?.eventImage}")
        } catch (e: Exception) {
            Log.e("EventDetails", "Error loading event: ${e.message}", e)
        }
        isLoading = false
    }

    // Load event image
    // Logic to handle both direct HTTP links and Firebase Storage (gs://) paths
    LaunchedEffect(event?.eventImage) {
        loadingImage = true
        val rawUrl = event?.eventImage?.trim() ?: ""

        if (rawUrl.isEmpty()) {
            loadingImage = false
            imageError = null
            return@LaunchedEffect
        }

        imageUrl = if (rawUrl.startsWith("gs://")) {
            try {
                Log.d("ImageLoad", "Loading gs:// URL: $rawUrl")
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(rawUrl)
                ref.downloadUrl.await().toString().also {
                    Log.d("ImageLoad", "Successfully loaded image URL: $it")
                }
            } catch (e: Exception) {
                Log.e("ImageLoad", "Failed to get download URL for ${event?.name}", e)
                imageError = e.message
                null
            }
        } else {
            Log.d("ImageLoad", "Direct URL: $rawUrl")
            rawUrl
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

                // Back Button
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
                    loadingImage = loadingImage,
                    imageError = imageError
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Event not found", color = Color.Gray) }
            }
        }
    }
}

/**
 * Displays the content of the event once data is successfully loaded.
 */
@Composable
fun EventDetailsContent(
    event: Event,
    imageUrl: String?,
    loadingImage: Boolean,
    imageError: String?
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {

        // IMAGE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            when {
                loadingImage -> CircularProgressIndicator()

                imageError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Failed to load image",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        imageError,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                imageUrl != null -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .listener(
                            onError = { _, result ->
                                Log.e("AsyncImage", "Error loading image: ${result.throwable.message}")
                            },
                            onSuccess = { _, _ ->
                                Log.d("AsyncImage", "Image loaded successfully")
                            }
                        )
                        .build(),
                    contentDescription = event.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No Image Available", color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Image path: ${event.eventImage ?: "null"}",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
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
                        Text("Date", color = Color.Gray, fontSize = 12.sp)
                        Text(formatDate(event.date), fontWeight = FontWeight.Medium)
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
                        Text("Venue", color = Color.Gray, fontSize = 12.sp)
                        Text(event.venue ?: "Venue TBA", fontWeight = FontWeight.Medium)
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
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = YELLOW,
                contentColor = Color.Black
            )
        ) {
            Text("Buy Tickets", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Utility to format Firebase Timestamp into readable date string
 */
fun formatDate(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return "Date TBA"
    return SimpleDateFormat("EEE, MMM dd yyyy h:mm a", Locale.getDefault())
        .format(ts.toDate())
}