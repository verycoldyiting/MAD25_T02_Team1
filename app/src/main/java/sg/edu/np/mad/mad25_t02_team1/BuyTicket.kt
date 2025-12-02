package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.SeatCategory
import sg.edu.np.mad.mad25_t02_team1.models.Event
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.util.Locale

// --- STABLE CUSTOM TOP BAR (Matches EventDetails Look and Go-Back Functionality) ---
@Composable
fun StableCustomAppBar(onBackPressed: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF00A2FF)) // Blue Color
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(onClick = onBackPressed) { // Executes Activity.finish()
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, // Stable RTL icon
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // No title text, as requested, allowing the bar to remain empty/minimal
        Spacer(modifier = Modifier.weight(1f))
    }
}


// --- MAIN SCREEN ---
@Composable
fun BuyTicketScreen(navController: NavController) {
    val activity = LocalContext.current as ComponentActivity
    val context = LocalContext.current
    val eventId = remember { activity.intent.getStringExtra("EVENT_ID") ?: "" }

    // --- State Management ---
    var event by remember { mutableStateOf<Event?>(null) }
    var selectedCategory by remember { mutableStateOf<SeatCategory?>(null) }
    var selectedSectionId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    var allSeatCategories by remember { mutableStateOf<List<SeatCategory>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }

    // --- Dropdown Menu State ---
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSectionMenu by remember { mutableStateOf(false) }
    var showQuantityMenu by remember { mutableStateOf(false) }

    // --- Data Fetching ---
    LaunchedEffect(eventId) {
        val db = FirebaseFirestore.getInstance()

        // Fetch Event Details
        try {
            val document = db.collection("Events").document(eventId).get().await()
            event = document.toObject(Event::class.java)
        } catch (e: Exception) { /* Log error */ }

        // Fetch Image URL & Categories
        isLoadingImage = true
        isLoadingCategories = true
        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("seating_plan.png")
            imageUrl = storageRef.downloadUrl.await().toString()

            val snapshot = db.collection("SeatCategory").get().await()
            allSeatCategories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SeatCategory::class.java)
            }
        } catch (e: Exception) { imageUrl = null }
        finally {
            isLoadingImage = false
            isLoadingCategories = false
        }
    }

    val totalPrice = (selectedCategory?.price ?: 0.0) * (quantity.toIntOrNull() ?: 0)

    Scaffold(
        topBar = { StableCustomAppBar(onBackPressed = { activity.finish() }) },
        bottomBar = {
            if (totalPrice > 0) {
                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total Price Display
                        Column {
                            Text("TOTAL PRICE", style = MaterialTheme.typography.labelSmall)
                            Text("S$ ${String.format(Locale.getDefault(), "%.2f", totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        // Booking/Payment Button
                        Button(
                            onClick = {
                                // Prepare the Intent to launch PaymentPage (which you called PaymentDetailActivity previously)
                                val intent = Intent(context, PaymentPage::class.java).apply {

                                    // --- 1. Event Details (Fetched from 'event' state) ---
                                    // Note: Use the naming convention expected by PaymentPage's onCreate
                                    putExtra("eventId", eventId)
                                    putExtra("title", event?.name ?: "Unknown Concert")
                                    putExtra("artist", event?.artist ?: "Unknown Artist")
                                    // Convert Firebase Timestamp to Milliseconds for dateMillis
                                    // Using a default of 0L if the date is null
                                    val dateMillis = event?.date?.toDate()?.time ?: 0L
                                    putExtra("dateMillis", dateMillis)
                                    putExtra("venue", event?.venue ?: "TBA")

                                    // --- 2. Ticket Details (Fetched from state) ---
                                    putExtra("category", selectedCategory?.category)
                                    putExtra("section", selectedSectionId)
                                    putExtra("quantity", quantity.toIntOrNull() ?: 1)

                                    // --- 3. Price Details (Fetch price per ticket, not total) ---
                                    // Calculate the price per ticket from the selected category
                                    putExtra("pricePerTicket", selectedCategory?.price ?: 0.0)

                                    // TOTAL_PRICE is no longer needed by PaymentPage's onCreate but is included
                                    // for completeness or if it's used elsewhere in PaymentPage.
                                    putExtra("TOTAL_PRICE", totalPrice)
                                }
                                context.startActivity(intent)
                            },
                            enabled = selectedCategory != null && selectedSectionId != null && quantity.toIntOrNull() in 1..4
                        ) {
                            Text("Book Now")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. STATIC SEATING MAP IMAGE ---
            if (isLoadingImage || isLoadingCategories) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.White)
                        .padding(horizontal = 0.dp)
                ) {
                    if (imageUrl != null) {
                        AsyncImage(model = imageUrl, contentDescription = "Seating Plan", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else {
                        Text("Seating map unavailable.", Modifier.align(Alignment.Center))
                    }
                }
            }

            // --- 2. CONCERT TITLE DISPLAY (BELOW IMAGE) ---
            if (event != null) {
                Text(
                    text = event!!.name ?: "Ticket Selection",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).padding(horizontal = 16.dp)
                )
            }

            // --- 3. INPUTS SECTION (Dropdowns) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // A. CATEGORY DROPDOWN
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory?.category ?: "Select Category",
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable(enabled = allSeatCategories.isNotEmpty()) { showCategoryMenu = true })
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        allSeatCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.category) }, onClick = {
                                selectedCategory = cat
                                selectedSectionId = null
                                showCategoryMenu = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // B. PRICE DISPLAY (Read-Only)
                OutlinedTextField(
                    value = selectedCategory?.price?.let { "S$ ${String.format("%.2f", it)}" } ?: "Price",
                    onValueChange = {},
                    label = { Text("Price (Per Ticket)") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // C. SECTION DROPDOWN
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSectionId ?: "Select Section ID",
                        onValueChange = {},
                        label = { Text("Section ID") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable(enabled = selectedCategory?.sections?.isNotEmpty() == true) { showSectionMenu = true })
                    DropdownMenu(expanded = showSectionMenu, onDismissRequest = { showSectionMenu = false }) {
                        selectedCategory?.sections?.forEach { section ->
                            DropdownMenuItem(text = { Text(section) }, onClick = {
                                selectedSectionId = section
                                showSectionMenu = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // D. QUANTITY DROPDOWN (1-4)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {},
                        label = { Text("Quantity") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showQuantityMenu = true })
                    DropdownMenu(expanded = showQuantityMenu, onDismissRequest = { showQuantityMenu = false }) {
                        (1..4).forEach { qty ->
                            DropdownMenuItem(text = { Text(qty.toString()) }, onClick = {
                                quantity = qty.toString()
                                showQuantityMenu = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}