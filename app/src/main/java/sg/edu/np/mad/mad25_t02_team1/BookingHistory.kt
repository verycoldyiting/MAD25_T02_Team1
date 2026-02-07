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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.Booking
import sg.edu.np.mad.mad25_t02_team1.models.Event
import java.text.SimpleDateFormat
import java.util.*

private enum class TicketViewMode { UPCOMING, RECENT, PAST }

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

// main screen displaying user's booking history with qr codes
@Composable
fun BookingHistoryScreen() {
    // store list of bookings paired with their event details
    var bookingWithEvents by remember { mutableStateOf<List<Pair<Booking, Event?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    var viewMode by remember { mutableStateOf(TicketViewMode.UPCOMING) }

    // fetch booking data when screen loads or user changes
    LaunchedEffect(key1 = firebaseUid) {
        // if no user logged in, stop loading
        if (firebaseUid.isNullOrEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        try {
            val db = FirebaseFirestore.getInstance()

            // find account document using firebase uid
            val accountQuerySnapshot = db.collection("Account")
                .whereEqualTo("uid", firebaseUid)
                .limit(1)
                .get()
                .await()

            // extract custom account id from document
            val resolvedCustomAccId = accountQuerySnapshot.documents.firstOrNull()?.id

            // if account not found, log error and stop
            if (resolvedCustomAccId.isNullOrEmpty()) {
                Log.e("BookingHistory", "Could not find custom Account ID for UID: $firebaseUid")
                isLoading = false
                return@LaunchedEffect
            }

            val accountRef = db.document("/Account/$resolvedCustomAccId")

            val bookingSnapshot = db.collection("BookingDetails")
                .whereEqualTo("AccID", accountRef)
                .get()
                .await()

            // convert firebase documents to booking objects
            val bookingList = bookingSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(id = doc.id)
            }

            val eventTasks = bookingList.mapNotNull { booking ->
                booking.eventId?.let { db.collection("Events").document(it).get() }
            }

            // wait for all event fetches to complete
            val results = Tasks.whenAllSuccess<DocumentSnapshot>(eventTasks).await()

            // convert event documents to objects and create map
            val eventMap = results.filterIsInstance<DocumentSnapshot>()
                .mapNotNull { doc -> doc.toObject(Event::class.java)?.copy(id = doc.id) }
                .associateBy { it.id }

            // pair each booking with its corresponding event
            val eventBookingPairs = bookingList.map { booking ->
                booking to booking.eventId?.let { eventMap[it] }
            }

            // store raw list; we'll sort it based on filter mode
            bookingWithEvents = eventBookingPairs

        } catch (e: Exception) {
            Log.e("BookingHistory", "Error fetching data", e)
        } finally {
            // always stop loading when done
            isLoading = false
        }
    }

    val now = System.currentTimeMillis()

    // filter + sorting logic
    val displayedList = remember(bookingWithEvents, viewMode, now) {

        fun eventTimeMillis(event: Event?): Long {
            return event?.date?.toDate()?.time ?: 0L
        }

        val upcoming = bookingWithEvents.filter { (_, event) ->
            val t = eventTimeMillis(event)
            t == 0L || t >= now
        }

        val past = bookingWithEvents.filter { (_, event) ->
            val t = eventTimeMillis(event)
            t in 1 until now
        }

        when (viewMode) {
            TicketViewMode.UPCOMING ->
                // soonest event first
                upcoming.sortedBy { (_, event) ->
                    val t = eventTimeMillis(event)
                    if (t == 0L) Long.MAX_VALUE else t
                }

            TicketViewMode.RECENT ->
                // upcoming tickets, latest purchase first
                upcoming.sortedByDescending { (booking, _) ->
                    booking.purchaseTime?.toDate()?.time ?: 0L
                }

            TicketViewMode.PAST ->
                // most recent past event first
                past.sortedByDescending { (_, event) ->
                    eventTimeMillis(event)
                }
        }
    }


    // display ui based on loading state
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        //  3 filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewMode == TicketViewMode.UPCOMING,
                onClick = { viewMode = TicketViewMode.UPCOMING },
                label = { Text("Upcoming") }
            )
            FilterChip(
                selected = viewMode == TicketViewMode.RECENT,
                onClick = { viewMode = TicketViewMode.RECENT },
                label = { Text("Recently Purchased") }
            )
            FilterChip(
                selected = viewMode == TicketViewMode.PAST,
                onClick = { viewMode = TicketViewMode.PAST },
                label = { Text("Past") }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            // show loading spinner
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (displayedList.isEmpty()) {
            // show empty state message depending on tab
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when (viewMode) {
                        TicketViewMode.UPCOMING -> "No upcoming tickets."
                        TicketViewMode.RECENT -> "No recent purchases for upcoming events."
                        TicketViewMode.PAST -> "No past tickets."
                    }
                )
            }
        } else {
            // display scrollable list of booking cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(displayedList) { (booking, event) ->
                    BookingHistoryItem(booking, event)
                }
            }
        }
    }
}

// individual booking card displaying ticket details and qr code
@Composable
fun BookingHistoryItem(booking: Booking, event: Event?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var pendingEvent by remember { mutableStateOf<Event?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // add event to calendar
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

            // display event poster image
            EventImage(rawUrl = event?.eventImage ?: booking.eventImage)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = event?.name ?: booking.concertTitle.orEmpty().ifEmpty { "Unknown Event" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // show artist name if available
            event?.artist?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Event Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timestampToString(eventDate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

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
                                    val hasCalendarPermission =
                                        ContextCompat.checkSelfPermission(
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

            // booking details section
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // check if event has passed (for expired ticket detection)
            val currentTime = System.currentTimeMillis()
            val eventTime = event?.date?.toDate()?.time ?: 0L
            val isExpired = eventTime > 0 && currentTime > eventTime

            // show either "View QR Code" button OR "QR Code Expired" box
            if (isExpired) {
                // expired ticket - show gray box (not clickable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "QR Code Expired",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "QR Code Expired",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // activate ticket - show yellow button (clickable)
                Button(
                    onClick = {
                        val intent = android.content.Intent(context, QRCodeActivity::class.java).apply {
                            putExtra("BOOKING_ID", booking.id)
                            putExtra("EVENT_NAME", event?.name ?: booking.concertTitle ?: "")
                            putExtra("ARTIST", event?.artist ?: "")
                            putExtra("VENUE", event?.venue ?: "")
                            putExtra("DATE_MILLIS", event?.date?.toDate()?.time ?: 0L)
                            putExtra("CATEGORY", booking.category ?: "")
                            putExtra("SECTION", booking.section ?: "")
                            putExtra("QUANTITY", booking.quantity ?: 1)
                            putExtra("PRICE_PER_TICKET", (booking.totalPrice ?: 0.0) / (booking.quantity ?: 1))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEB3B), // Yellow
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View QR Code", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// helper composable displaying label-value pairs
@Composable
fun DetailText(label: String, value: String) {
    Row {
        // label in medium weight
        Text(
            label,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))

        // value in bold
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// composable loading and displaying event image from firebase or url
@Composable
fun EventImage(rawUrl: String?) {
    var displayUrl by remember { mutableStateOf<String?>(null) }
    var loadingError by remember { mutableStateOf(false) }
    LaunchedEffect(rawUrl) {
        // reset states
        loadingError = false
        displayUrl = null

        // clean up url string
        val cleanedUrl = rawUrl?.trim()

        // if no url, mark as error
        if (cleanedUrl.isNullOrBlank()) {
            loadingError = true
            return@LaunchedEffect
        }

        // if firebase storage path, get download url
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

    // image container box with rounded corners
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp)),
        Alignment.Center
    ) {
        if (displayUrl == null && !loadingError) {
            // show loading spinner while fetching
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.5f)),
                Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (loadingError) {
            // show error message if loading failed
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                Alignment.Center
            ) {
                Text(
                    "Image unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            // display image using coil
            Image(
                painter = rememberAsyncImagePainter(model = displayUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// convert firebase timestamp to readable date string
fun timestampToString(ts: Timestamp): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Singapore")
    }.format(ts.toDate())
}

// add event directly to phone's calendar database
private fun addEventToCalendarDirectly(context: android.content.Context, event: Event) {
    try {
        val eventDateMillis = event.date?.toDate()?.time

        // if no date, show error
        if (eventDateMillis == null) {
            Toast.makeText(context, "Event date not available", Toast.LENGTH_SHORT).show()
            return
        }

        // get device's calendar id
        val calendarId = getCalendarId(context)

        // if no calendar found, show error
        if (calendarId == null) {
            Toast.makeText(context, "No calendar found on device.", Toast.LENGTH_LONG).show()
            return
        }

        // build event data for calendar
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, eventDateMillis)
            put(CalendarContract.Events.DTEND, eventDateMillis + (3 * 60 * 60 * 1000))
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
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        // insert event into calendar
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri != null) {
            val eventId = uri.lastPathSegment?.toLongOrNull()

            if (eventId != null) {
                // create reminder for 1 day before (1440 minutes)
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 1440)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }

                // insert reminder
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            // show success message
            Toast.makeText(context, "✓ Event added to calendar!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// get first available calendar id from device
private fun getCalendarId(context: android.content.Context): Long? {
    return try {
        // specify which columns to retrieve
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        // read results
        cursor?.use {
            if (it.moveToFirst()) {
                // get calendar id
                val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                return it.getLong(idIndex)
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}
