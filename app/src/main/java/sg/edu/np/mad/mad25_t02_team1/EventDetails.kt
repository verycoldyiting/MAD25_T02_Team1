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
import java.util.*

class EventDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the event ID passed from the previous screen
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
    var isLoadingImage by remember { mutableStateOf(true) }

    // Fetch event details from Firebase
    LaunchedEffect(eventId) {
        isLoading = true
        try {
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("Events").document(eventId).get().await()
            event = document.toObject(Event::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("EventDetails", "Error loading event", e)
        } finally {
            isLoading = false
        }
    }

    // Load event image
    LaunchedEffect(event?.eventImage) {
        val rawUrl = event?.eventImage?.trim() ?: ""
        if (rawUrl.isEmpty()) {
            isLoadingImage = false
            return@LaunchedEffect
        }

        isLoadingImage = true
        imageUrl = if (rawUrl.startsWith("gs://")) {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(rawUrl)
                ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("ImageLoad", "Failed to get download URL", e)
                null
            }
        } else {
            rawUrl
        }
        isLoadingImage = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Custom Header with Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .background(Color(0xFF00A2FF))
        ) {
            // Back Button
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Center Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                AsyncImage(
                    model = "https://firebasestorage.googleapis.com/v0/b/mad25t02team1.firebasestorage.app/o/image-removebg-preview.png?alt=media&token=3b068498-aeb6-4491-8ab2-17c10f807a2d",
                    contentDescription = "TicketLah Logo",
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "TicketLah!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // Content Area
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (event != null) {
            EventDetailsContent(
                event = event!!,
                imageUrl = imageUrl,
                isLoadingImage = isLoadingImage
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Event not found",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun EventDetailsContent(
    event: Event,
    imageUrl: String?,
    isLoadingImage: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Event Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingImage) {
                CircularProgressIndicator()
            } else if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = event.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No Image Available", color = Color.Gray)
            }
        }

        // Event Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Event Name
                Text(
                    text = event.name ?: "Event Name",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Event Description/Caption
                Text(
                    text = event.description ?: event.caption ?: "No description available",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Date Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Date",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatDate(event.date),
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.LightGray, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Venue Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Venue",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Venue",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = event.venue ?: "Venue TBA",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // See On Maps Button
                    OutlinedButton(
                        onClick = { /* TODO: Implement maps functionality */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "See On Maps",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        val context = LocalContext.current
        // Buy Tickets Button
        Button(
            onClick = {
                val intent = Intent(context, BuyTicketActivity::class.java).apply {
                    putExtra("EVENT_ID", event.id) // Pass the event ID
                }
                context.startActivity(intent)            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFC107)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Buy Tickets",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Info Card (Artist, Genre, Price)
        if (event.artist != null || event.genre != null || event.price != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Event Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    event.artist?.let { artist ->
                        InfoRow(label = "Artist", value = artist)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    event.genre?.let { genre ->
                        InfoRow(label = "Genre", value = genre)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    event.price?.let { price ->
                        InfoRow(label = "Starting Price", value = "$${"%.2f".format(price)}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Date TBA"

    return try {
        val date = timestamp.toDate()
        val format = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        "Date TBA"
    }
}