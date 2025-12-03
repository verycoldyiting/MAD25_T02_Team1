package sg.edu.np.mad.mad25_t02_team1.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.BottomNavItem
import sg.edu.np.mad.mad25_t02_team1.BottomNavigationBar
import sg.edu.np.mad.mad25_t02_team1.HomePageContent
import sg.edu.np.mad.mad25_t02_team1.TicketLahHeader
import sg.edu.np.mad.mad25_t02_team1.models.Booking
import sg.edu.np.mad.mad25_t02_team1.models.Event
import sg.edu.np.mad.mad25_t02_team1.ExploreEventsApp
import sg.edu.np.mad.mad25_t02_team1.ProfileScreen
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun BookingHistoryScaffold() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Tickets) }

    var refreshTrigger by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { TicketLahHeader() },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { item ->
                    if (selectedTab == item) {
                        if (item == BottomNavItem.Tickets) {
                            refreshTrigger++
                        }
                    } else {
                        selectedTab = item
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Tickets.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomePageContent() }
            composable(BottomNavItem.Search.route) { ExploreEventsApp() }
            composable(BottomNavItem.Tickets.route) { BookingHistoryScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }
        }
    }
}


@Composable
fun BookingHistoryScreen() {
    var bookingWithEvents by remember { mutableStateOf<List<Pair<Booking, Event?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get the Firebase UID
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    // LaunchedEffect runs when the Firebase UID changes (i.e., user logs in)
    LaunchedEffect(key1 = firebaseUid) {
        if (firebaseUid.isNullOrEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        try {
            val db = FirebaseFirestore.getInstance()

            val accountQuerySnapshot = db.collection("Account")
                .whereEqualTo("uid", firebaseUid)
                .limit(1)
                .get()
                .await()

            val resolvedCustomAccId = accountQuerySnapshot.documents.firstOrNull()?.id

            // If the custom Account ID cannot be resolved, stop.
            if (resolvedCustomAccId.isNullOrEmpty()) {
                Log.e("BookingHistory", "Could not find custom Account ID for UID: $firebaseUid")
                isLoading = false
                return@LaunchedEffect
            }

            // FETCH BOOKINGS USING THE DocumentReference to the Account
            // Construct the DocumentReference using the resolved custom ID
            val accountRef = db.document("/Account/$resolvedCustomAccId")

            val bookingSnapshot = db.collection("BookingDetails")
                // Query looks for documents where AccID points to /Account/A001
                .whereEqualTo("AccID", accountRef)
                .get()
                .await()

            // --- STEP 3: Process Bookings and Fetch Events ---
            val bookingList = bookingSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(id = doc.id)
            }

            val eventTasks = bookingList.mapNotNull { booking ->
                booking.eventId?.let { db.collection("Events").document(it).get() }
            }
            val results = Tasks.whenAllSuccess<DocumentSnapshot>(eventTasks).await()
            val eventMap = results.filterIsInstance<DocumentSnapshot>()
                .mapNotNull { doc -> doc.toObject(Event::class.java)?.copy(id = doc.id) }
                .associateBy { it.id }

            val eventBookingPairs = bookingList.map { booking ->
                booking to booking.eventId?.let { eventMap[it] }
            }

            bookingWithEvents = eventBookingPairs

        } catch (e: Exception) {
            Log.e("BookingHistory", "Error fetching data", e)
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (bookingWithEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No booking history found.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                items(bookingWithEvents) { (booking, event) ->
                    BookingHistoryItem(booking, event)
                }
            }
        }
    }
}

@Composable
fun BookingHistoryItem(booking: Booking, event: Event?) {
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Use event image as primary source
            EventImage(rawUrl = event?.eventImage ?: booking.eventImage)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                // Use concertTitle as fallback for event name
                text = event?.name ?: booking.concertTitle.orEmpty().ifEmpty { "Unknown Event" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            event?.artist?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event Date
            event?.date?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, "Event Date", Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = timestampToString(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Event Venue
            event?.venue?.takeIf { it.isNotEmpty() }?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, "Venue", Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        DetailText("Category:", booking.category.orEmpty().ifEmpty { "N/A" })
                        DetailText("Seatt:", booking.section.orEmpty().ifEmpty { "N/A" })
                        DetailText("Quantity:", (booking.quantity ?: 0).toString())
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        DetailText("Payment:", booking.paymentMethod.orEmpty().ifEmpty { "N/A" })
                        Spacer(Modifier.height(8.dp))

                        val totalPrice = booking.totalPrice ?: 0.0
                        Text(
                            text = "Total: $${String.format("%.2f", totalPrice)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            booking.purchaseTime?.let {
                Spacer(Modifier.height(12.dp))
                Text("Purchased: ${timestampToString(it)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DetailText(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EventImage(rawUrl: String?) {
    var displayUrl by remember { mutableStateOf<String?>(null) }
    var loadingError by remember { mutableStateOf(false) }

    LaunchedEffect(rawUrl) {
        loadingError = false
        displayUrl = null

        val cleanedUrl = rawUrl?.trim()

        if (cleanedUrl.isNullOrBlank()) {
            loadingError = true
            return@LaunchedEffect
        }

        if (cleanedUrl.startsWith("gs://")) {
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(cleanedUrl)
                val uri = storageRef.downloadUrl.await()
                displayUrl = uri.toString()
            } catch (e: Exception) {
                Log.w("EventImage", "Failed to resolve gs:// url: $cleanedUrl", e)
                loadingError = true
            }
        } else {
            displayUrl = cleanedUrl
        }
    }

    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)), Alignment.Center) {
        if (displayUrl == null && !loadingError) {
            Box(Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.5f)), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (loadingError) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer), Alignment.Center) {
                Text("Image unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            Image(
                painter = rememberAsyncImagePainter(model = displayUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun timestampToString(ts: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(ts.toDate())
}