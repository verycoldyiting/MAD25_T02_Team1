package sg.edu.np.mad.mad25_t02_team1

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Color

// --- 1. GEOMETRY DATA STRUCTURES ---
// These structures define the clickable zones. You must expand the SEATING_SECTIONS list
// to cover all sections (101, 203, 301, etc.) visible in your seating plan image.

data class SeatSection(
    val id: String,         // Section number, e.g., "101"
    val category: String,   // Category, e.g., "CAT A"
    val bounds: Rect        // Normalized coordinates [left, top, right, bottom]
)

// NOTE: This list contains placeholder coordinates and must be manually mapped
// by visually inspecting your image and calculating the 0.0 to 1.0 coordinates
// for each rectangular bounding box.
val SEATING_SECTIONS = listOf(
    // Section 101 (Placeholder example)
    SeatSection("101", "CAT B", Rect(0.15f, 0.60f, 0.25f, 0.70f)),
    // Section 203 (Placeholder example)
    SeatSection("203", "CAT C", Rect(0.05f, 0.35f, 0.15f, 0.45f)),
    // ... Add all other sections here
    SeatSection("301", "CAT A", Rect(0.30f, 0.20f, 0.50f, 0.30f))
)

// --- 2. INTERACTIVE MAP COMPOSABLE ---
@Composable
fun InteractiveSeatingMap(
    imageUrl: String?,
    onSectionSelected: (SeatSection) -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val containerSizeFloat = containerSize.toSize()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.White) // Ensure the box background is white
            .onSizeChanged { containerSize = it }
            .padding(horizontal = 0.dp) // Force edge-to-edge width
            .pointerInput(containerSize, SEATING_SECTIONS) {
                detectTapGestures { offset ->
                    if (containerSize.width == 0) return@detectTapGestures

                    // Normalize click coordinates (0.0 to 1.0)
                    val clickX = offset.x / containerSizeFloat.width
                    val clickY = offset.y / containerSizeFloat.height

                    // Check which section was hit
                    val selected = SEATING_SECTIONS.find { section ->
                        section.bounds.contains(Offset(clickX, clickY))
                    }

                    selected?.let {
                        onSectionSelected(it)
                    }
                }
            }
    ) {
        // --- Layer 1: The Visual Seating Plan Image (White Background) ---
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Seating Plan",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit // Use FIT to show the whole image (including borders)
            )
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}


// --- 3. BUY TICKET SCREEN (Main UI) ---
@Composable
fun BuyTicketScreen() {
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var seatNumber by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Data Fetching Logic
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("seating_plan.png")
            val uri = storageRef.downloadUrl.await()
            imageUrl = uri.toString()
        } catch (e: Exception) {
            imageUrl = null
        } finally {
            isLoading = false
        }
    }

    // Outer Column for the whole screen (inherits Scaffold's default white background)
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- INTERACTIVE MAP INTEGRATION ---
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        } else {
            InteractiveSeatingMap(
                imageUrl = imageUrl,
                onSectionSelected = { selectedSection ->
                    // Populate the input fields based on the click
                    category = selectedSection.category
                    seatNumber = selectedSection.id
                    // Quantity is left for manual input
                }
            )
        }

        // --- INPUTS SECTION (Padded Content) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // The input fields now show the dynamically updated state
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                // Make category display read-only as it's determined by the map click
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = seatNumber,
                onValueChange = { seatNumber = it },
                label = { Text("Seat Number / Section ID") },
                // Make seatNumber display read-only as it's determined by the map click
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Handle booking logic here */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book")
            }
        }
    }
}