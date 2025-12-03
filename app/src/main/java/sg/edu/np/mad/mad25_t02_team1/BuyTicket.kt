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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.SeatCategory
import sg.edu.np.mad.mad25_t02_team1.models.Event
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun BuyTicketScreen(navController: NavController) {

    val activity = LocalContext.current as ComponentActivity
    val eventId = activity.intent.getStringExtra("EVENT_ID") ?: ""
    val context = LocalContext.current

    var event by remember { mutableStateOf<Event?>(null) }
    var selectedCategory by remember { mutableStateOf<SeatCategory?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("1") }

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }

    var allSeatCategories by remember { mutableStateOf<List<SeatCategory>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(true) }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSectionMenu by remember { mutableStateOf(false) }
    var showQuantityMenu by remember { mutableStateOf(false) }

    // -------- FETCH FIREBASE DATA --------
    LaunchedEffect(eventId) {
        val db = FirebaseFirestore.getInstance()

        try {
            val doc = db.collection("Events").document(eventId).get().await()
            event = doc.toObject(Event::class.java)
        } catch (_: Exception) {}

        try {
            val ref = FirebaseStorage.getInstance().reference.child("seating_plan.png")
            imageUrl = ref.downloadUrl.await().toString()

            val snapshot = db.collection("SeatCategory").get().await()
            allSeatCategories = snapshot.documents.mapNotNull { it.toObject(SeatCategory::class.java) }

        } catch (_: Exception) {
            imageUrl = null
        } finally {
            isLoadingImage = false
            isLoadingCategories = false
        }
    }

    val totalPrice = (selectedCategory?.price ?: 0.0) * (quantity.toIntOrNull() ?: 0)

    // -------- UI --------
    Scaffold(

        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TicketLahHeader()
                var onBackPressed = { finish() }
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
        },

        bottomBar = {
            if (totalPrice > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL PRICE", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "S$ ${String.format(Locale.getDefault(), "%.2f", totalPrice)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            enabled = selectedCategory != null && selectedSection != null,
                            onClick = {

                                val intent = Intent(context, PaymentPage::class.java).apply {
                                    putExtra("eventId", eventId)
                                    putExtra("title", event?.name ?: "")
                                    putExtra("artist", event?.artist ?: "")
                                    putExtra("dateMillis", event?.date?.toDate()?.time ?: 0L)
                                    putExtra("venue", event?.venue ?: "")
                                    putExtra("category", selectedCategory?.category)
                                    putExtra("section", selectedSection)
                                    putExtra("quantity", quantity.toIntOrNull() ?: 1)
                                    putExtra("pricePerTicket", selectedCategory?.price ?: 0.0)
                                    putExtra("TOTAL_PRICE", totalPrice)
                                }

                                context.startActivity(intent)
                            }
                        ) {
                            Text("Book Now")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Seating Image
            if (isLoadingImage) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Seating",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Fit
                )
            }

            if (event != null) {
                Text(
                    text = event!!.name ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // -------- CATEGORY --------
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory?.category ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryMenu = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        allSeatCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.category) },
                                onClick = {
                                    selectedCategory = cat
                                    selectedSection = null
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // -------- PRICE --------
                OutlinedTextField(
                    value = selectedCategory?.price?.let { "S$ %.2f".format(it) } ?: "Price",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Price (Per Ticket)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // -------- SECTION --------
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSection ?: "Select Section",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Section") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = selectedCategory != null) { showSectionMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSectionMenu,
                        onDismissRequest = { showSectionMenu = false }
                    ) {
                        selectedCategory?.sections?.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    selectedSection = sec
                                    showSectionMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // -------- QUANTITY --------
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quantity") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showQuantityMenu = true }
                    )
                    DropdownMenu(
                        expanded = showQuantityMenu,
                        onDismissRequest = { showQuantityMenu = false }
                    ) {
                        (1..4).forEach { qty ->
                            DropdownMenuItem(
                                text = { Text(qty.toString()) },
                                onClick = {
                                    quantity = qty.toString()
                                    showQuantityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun BoxScope.finish() {
    TODO("Not yet implemented")
}
