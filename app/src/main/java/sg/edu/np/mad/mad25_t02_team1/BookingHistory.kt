package sg.edu.np.mad.mad25_t02_team1

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import sg.edu.np.mad.mad25_t02_team1.models.Booking
import sg.edu.np.mad.mad25_t02_team1.models.Event
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

            // if the custom Account ID cannot be resolved, stop.
            if (resolvedCustomAccId.isNullOrEmpty()) {
                Log.e("BookingHistory", "Could not find custom Account ID for UID: $firebaseUid")
                isLoading = false
                return@LaunchedEffect
            }

            // fetch bookings from DocumentReference to the Account
            // construct the DocumentReference using the resolved custom ID
            val accountRef = db.document("/Account/$resolvedCustomAccId")

            val bookingSnapshot = db.collection("BookingDetails")
                // Query looks for documents where AccID points to /Account/A001
                .whereEqualTo("AccID", accountRef)
                .get()
                .await()

            // processes Bookings and Fetch Events
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    // permission launcher for putting event details onto the phone's calendar app
    var pendingEvent by remember { mutableStateOf<Event?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingEvent?.let { evt ->
                addEventToCalendarDirectly(context, evt)
            }
        } else {
            Toast.makeText(context, "Calendar permission is required to add events", Toast.LENGTH_LONG).show()
        }
        pendingEvent = null
    }

    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            EventImage(rawUrl = event?.eventImage ?: booking.eventImage)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
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

            // event Date with Add to Calendar button
            event?.date?.let { eventDate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Event, "Event Date", Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timestampToString(eventDate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // add to Calendar Button with border
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                event?.let { evt ->
                                    // Check if permissions are granted
                                    val hasCalendarPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_CALENDAR
                                    ) == PackageManager.PERMISSION_GRANTED &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_CALENDAR
                                            ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCalendarPermission) {
                                        isPressed = true
                                        addEventToCalendarDirectly(context, evt)
                                        coroutineScope.launch {
                                            delay(300)
                                            isPressed = false
                                        }
                                    } else {
                                        // Request permissions
                                        pendingEvent = evt
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Add To Calendar",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isPressed) TextDecoration.Underline else TextDecoration.None
                        )
                    }
                }
            }

            // event venue
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
                        DetailText("Seat:", booking.section.orEmpty().ifEmpty { "N/A" })
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

// directly insert event into phone's calendar database
private fun addEventToCalendarDirectly(context: android.content.Context, event: Event) {
    try {
        val eventDateMillis = event.date?.toDate()?.time

        if (eventDateMillis == null) {
            Toast.makeText(context, "Event date not available", Toast.LENGTH_SHORT).show()
            return
        }

        // get the first available calendar ID
        val calendarId = getCalendarId(context)

        if (calendarId == null) {
            Toast.makeText(context, "No calendar found on device. Event details: ${event.name}, ${event.venue}, ${timestampToString(event.date!!)}", Toast.LENGTH_LONG).show()
            return
        }

        // build event details
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, eventDateMillis)
            put(CalendarContract.Events.DTEND, eventDateMillis + (3 * 60 * 60 * 1000)) //duration is default set to 3h for standardisation
            put(CalendarContract.Events.TITLE, event.name ?: "Concert")

            val description = buildString {
                append("Artist: ${event.artist ?: "Unknown"}\n")
                append("Venue: ${event.venue ?: "TBA"}")
                if (!event.description.isNullOrBlank()) {
                    append("\n\n${event.description}")
                }
            }
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, event.venue ?: "")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().timeZone.id)
            put(CalendarContract.Events.HAS_ALARM, 1) // adds reminder
        }

        // insert event into calendar
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        if (uri != null) {
            // add a reminder (1 day before)
            val eventId = uri.lastPathSegment?.toLongOrNull()
            if (eventId != null) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 1440) // 1 day = 1440 minutes
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            Toast.makeText(
                context,
                "✓ Event added to calendar!\n${event.name}\n${timestampToString(event.date!!)}",
                Toast.LENGTH_LONG
            ).show()

            Log.d("Calendar", "Event added successfully: ${event.name}")
        } else {
            Toast.makeText(context, "Failed to add event to calendar", Toast.LENGTH_SHORT).show()
        }

    } catch (e: SecurityException) {
        Log.e("Calendar", "Permission denied", e)
        Toast.makeText(context, "Calendar permission required", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("Calendar", "Error adding to calendar", e)
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// get the first available calendar ID from the device
private fun getCalendarId(context: android.content.Context): Long? {
    return try {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIndex = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

                val calendarId = it.getLong(idIndex)
                val calendarName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                val accountName = if (accountIndex >= 0) it.getString(accountIndex) else "Unknown"

                Log.d("Calendar", "Using calendar: $calendarName (Account: $accountName, ID: $calendarId)")
                return calendarId
            }
        }
        null
    } catch (e: SecurityException) {
        Log.e("Calendar", "No permission to access calendars", e)
        null
    } catch (e: Exception) {
        Log.e("Calendar", "Error getting calendar ID", e)
        null
    }
}