package sg.edu.np.mad.mad25_t02_team1

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import sg.edu.np.mad.mad25_t02_team1.models.Booking
import sg.edu.np.mad.mad25_t02_team1.models.Event
import java.text.SimpleDateFormat
import java.util.*


// main scaffold that holds the entire booking history screen with navigation
@Composable
fun BookingHistoryScaffold() {
    // create navigation controller to manage screen transitions
    val navController = rememberNavController()

    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Tickets) }

    // trigger to force refresh when needed
    var refreshTrigger by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { TicketLahHeader() },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { item ->
                    // if tapping the same tab (tickets), refresh the content
                    if (selectedTab == item) {
                        if (item == BottomNavItem.Tickets) {
                            refreshTrigger++
                        }
                    } else {
                        // navigate to different tab
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
        // navigation host manages which screen content to show based on selected tab
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


// main screen that displays the user's booking history
@Composable
fun BookingHistoryScreen() {
    // store list of bookings paired with their event details
    var bookingWithEvents by remember { mutableStateOf<List<Pair<Booking, Event?>>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    // get current logged-in user's firebase uid
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    // fetch booking data when screen loads or user changes
    LaunchedEffect(key1 = firebaseUid) {
        // if no user is logged in, stop loading
        if (firebaseUid.isNullOrEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        try {
            // get firestore database instance
            val db = FirebaseFirestore.getInstance()

            // find the account document for this user using their firebase uid
            val accountQuerySnapshot = db.collection("Account")
                .whereEqualTo("uid", firebaseUid)
                .limit(1)
                .get()
                .await()

            // extract the custom account id from the found document
            val resolvedCustomAccId = accountQuerySnapshot.documents.firstOrNull()?.id

            // if account not found, log error and stop
            if (resolvedCustomAccId.isNullOrEmpty()) {
                Log.e("BookingHistory", "Could not find custom Account ID for UID: $firebaseUid")
                isLoading = false
                return@LaunchedEffect
            }

            // create reference to the account document
            val accountRef = db.document("/Account/$resolvedCustomAccId")

            // fetch all bookings that belong to this account
            val bookingSnapshot = db.collection("BookingDetails")
                .whereEqualTo("AccID", accountRef)
                .get()
                .await()

            // convert firebase documents to booking objects
            val bookingList = bookingSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(id = doc.id)
            }

            // fetch event details for each booking
            val eventTasks = bookingList.mapNotNull { booking ->
                booking.eventId?.let { db.collection("Events").document(it).get() }
            }

            // wait for all event fetches to complete
            val results = Tasks.whenAllSuccess<DocumentSnapshot>(eventTasks).await()

            // convert event documents to event objects and create a map
            val eventMap = results.filterIsInstance<DocumentSnapshot>()
                .mapNotNull { doc -> doc.toObject(Event::class.java)?.copy(id = doc.id) }
                .associateBy { it.id }

            // pair each booking with its corresponding event
            val eventBookingPairs = bookingList.map { booking ->
                booking to booking.eventId?.let { eventMap[it] }
            }

            // update state with fetched data
            bookingWithEvents = eventBookingPairs

        } catch (e: Exception) {
            Log.e("BookingHistory", "Error fetching data", e)
        } finally {
            // always set loading to false when done
            isLoading = false
        }
    }

    // display ui based on loading state
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (isLoading) {
            // show loading spinner while fetching data
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (bookingWithEvents.isEmpty()) {
            // show message if no bookings found
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No booking history found.") }
        } else {
            // display list of booking cards
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                items(bookingWithEvents) { (booking, event) ->
                    BookingHistoryItem(booking, event)
                }
            }
        }
    }
}

// individual booking card that displays ticket details
@Composable
fun BookingHistoryItem(booking: Booking, event: Event?) {
    // get context for displaying toasts and launching intents
    val context = LocalContext.current

    // coroutine scope for launching async tasks
    val coroutineScope = rememberCoroutineScope()

    // track if "add to calendar" button is currently pressed
    var isPressed by remember { mutableStateOf(false) }

    // handle calendar permission requests
    var pendingEvent by remember { mutableStateOf<Event?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // check if all permissions were granted
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // if granted, add event to calendar
            pendingEvent?.let { evt ->
                addEventToCalendarDirectly(context, evt)
            }
        } else {
            // if denied, show error message
            Toast.makeText(context, "Calendar permission is required to add events", Toast.LENGTH_LONG).show()
        }
        pendingEvent = null
    }

    // card container for the booking
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

            // show event name in bold
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

            // row showing event date and "add to calendar" button
            event?.date?.let { eventDate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // date display section
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

                    // "add to calendar" button with border
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
                                    // check if calendar permissions are already granted
                                    val hasCalendarPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_CALENDAR
                                    ) == PackageManager.PERMISSION_GRANTED &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_CALENDAR
                                            ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCalendarPermission) {
                                        // permissions already granted, add to calendar directly
                                        isPressed = true
                                        addEventToCalendarDirectly(context, evt)
                                        coroutineScope.launch {
                                            delay(300)
                                            isPressed = false
                                        }
                                    } else {
                                        // request permissions first
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
                        // button text with underline when pressed
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

            // show venue location if available
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

            // booking details section (category, seat, quantity, payment, price)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    // left column: category, seat, quantity
                    Column {
                        DetailText("Category:", booking.category.orEmpty().ifEmpty { "N/A" })
                        DetailText("Seat:", booking.section.orEmpty().ifEmpty { "N/A" })
                        DetailText("Quantity:", (booking.quantity ?: 0).toString())
                    }
                    // right column: payment method and total price
                    Column(horizontalAlignment = Alignment.End) {
                        DetailText("Payment:", booking.paymentMethod.orEmpty().ifEmpty { "N/A" })
                        Spacer(Modifier.height(8.dp))

                        // show total price in red
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

            // show purchase timestamp if available
            booking.purchaseTime?.let {
                Spacer(Modifier.height(12.dp))
                Text("Purchased: ${timestampToString(it)}", style = MaterialTheme.typography.labelSmall)
            }

            // divider before qr code section
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // display qr code with timer
            QRCodeWithTimer(booking = booking)
        }
    }
}

// qr code component that generates and displays unique qr codes with countdown timer
@Composable
fun QRCodeWithTimer(booking: Booking) {
    // counter tracking how many times qr code has changed (not displayed but tracked)
    var qrCounter by remember { mutableStateOf(0) }

    // countdown from 60 to 0 seconds
    var countdown by remember { mutableStateOf(60) }

    // timestamp seed that makes each qr code unique
    var randomSeed by remember { mutableStateOf(System.currentTimeMillis()) }

    // background timer that runs continuously
    LaunchedEffect(Unit) {
        while (true) {
            // wait 1 second
            delay(1000)

            // decrease countdown by 1
            countdown--

            // when countdown reaches 0, generate new qr code
            if (countdown <= 0) {
                // get new timestamp to make qr code unique
                randomSeed = System.currentTimeMillis()

                // increment counter
                qrCounter++

                // reset countdown to 60 seconds
                countdown = 60
            }
        }
    }

    // create qr code data string with booking info and timestamp for uniqueness
    val currentQRData = "TICKET:${booking.id}:${booking.concertTitle}:${booking.section}:${booking.category}:TIME:$randomSeed"

    // layout column to center qr code and timer text
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // qr code title
        Text(
            text = "Your QR Code",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // generate qr code bitmap only when currentqrdata changes
        val qrBitmap = remember(currentQRData) { generateQRCode(currentQRData) }

        // display qr code if generation successful
        qrBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Ticket QR Code",
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White)
                    .padding(8.dp)
            )
        } ?: run {
            // show error if qr generation failed
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("QR Code Error", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // display countdown timer text
        Text(
            text = "Next refresh in $countdown seconds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// function that generates qr code bitmap from text data using zxing library
fun generateQRCode(data: String, size: Int = 512): Bitmap? {
    return try {
        // create qr code writer from zxing library
        val writer = QRCodeWriter()

        // encode text data into qr code matrix (512x512 pixels)
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)

        // get dimensions of the matrix
        val width = bitMatrix.width
        val height = bitMatrix.height

        // create empty bitmap to draw qr code
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        // loop through each pixel in the matrix
        for (x in 0 until width) {
            for (y in 0 until height) {
                // set pixel to black if qr matrix has true, white if false
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        // return generated bitmap
        bitmap
    } catch (e: Exception) {
        // log error and return null if generation fails
        Log.e("QRCode", "Error generating QR code", e)
        null
    }
}

// helper composable to display label-value pairs
@Composable
fun DetailText(label: String, value: String) {
    Row {
        // label text in medium weight
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))

        // value text in bold
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun EventImage(rawUrl: String?) {

    var displayUrl by remember { mutableStateOf<String?>(null) }
    var loadingError by remember { mutableStateOf(false) }

    LaunchedEffect(rawUrl) {
        // reset states
        loadingError = false
        displayUrl = null

        val cleanedUrl = rawUrl?.trim()

        if (cleanedUrl.isNullOrBlank()) {
            loadingError = true
            return@LaunchedEffect
        }

        // if url is firebase storage path (gs://), get download url
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
            // if regular url, use directly
            displayUrl = cleanedUrl
        }
    }

    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)), Alignment.Center) {
        if (displayUrl == null && !loadingError) {

            Box(Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.5f)), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (loadingError) {
            // show error message if image failed to load
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer), Alignment.Center) {
                Text("Image unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            // display image using coil library
            Image(
                painter = rememberAsyncImagePainter(model = displayUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// utility function to convert firebase timestamp to readable date string
fun timestampToString(ts: Timestamp): String {
    // format: "12 Jan 2025, 12:00 PM"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(ts.toDate())
}

// function to add event directly to phone's calendar database
private fun addEventToCalendarDirectly(context: android.content.Context, event: Event) {
    try {
        // convert firebase timestamp to milliseconds
        val eventDateMillis = event.date?.toDate()?.time

        // if no date available, show error and return
        if (eventDateMillis == null) {
            Toast.makeText(context, "Event date not available", Toast.LENGTH_SHORT).show()
            return
        }

        // get the device's calendar id
        val calendarId = getCalendarId(context)

        // if no calendar found, show error and return
        if (calendarId == null) {
            Toast.makeText(context, "No calendar found on device.", Toast.LENGTH_LONG).show()
            return
        }

        // build event data to insert into calendar
        val values = ContentValues().apply {
            // event start time
            put(CalendarContract.Events.DTSTART, eventDateMillis)

            // event end time (3 hours after start)
            put(CalendarContract.Events.DTEND, eventDateMillis + (3 * 60 * 60 * 1000))

            // event title
            put(CalendarContract.Events.TITLE, event.name ?: "Concert")

            // build event description with artist and venue info
            val description = buildString {
                append("Artist: ${event.artist ?: "Unknown"}\n")
                append("Venue: ${event.venue ?: "TBA"}")
                if (!event.description.isNullOrBlank()) {
                    append("\n\n${event.description}")
                }
            }
            put(CalendarContract.Events.DESCRIPTION, description)

            // event location
            put(CalendarContract.Events.EVENT_LOCATION, event.venue ?: "")

            // which calendar to add to
            put(CalendarContract.Events.CALENDAR_ID, calendarId)

            // timezone
            put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().timeZone.id)

            // enable alarm/reminder
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        // insert event into calendar database
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        // if insertion successful, add reminder
        if (uri != null) {
            // get the id of newly created event
            val eventId = uri.lastPathSegment?.toLongOrNull()

            if (eventId != null) {
                // create reminder for 1 day before event (1440 minutes)
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 1440)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }

                // insert reminder into database
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            // show success message
            Toast.makeText(context, "✓ Event added to calendar!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        // show error message if something went wrong
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// function to get the first available calendar id from device
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

        // use cursor to read results
        cursor?.use {
            if (it.moveToFirst()) {

                val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                return it.getLong(idIndex)
            }
        }

        null
    } catch (e: Exception) {

        null
    }
}