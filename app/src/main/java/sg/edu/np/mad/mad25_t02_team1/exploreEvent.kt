package sg.edu.np.mad.mad25_t02_team1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// --- DATA MODEL ---
data class Event(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val genre: String = "",
    val rawImageRef: String = ""
)

@Composable
fun TicketLahApp() {
    // --- STATE ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }

    val focusManager = LocalFocusManager.current

    // --- FETCH DATA FROM FIRESTORE ---
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Events").get()
            .addOnSuccessListener { result ->
                val fetchedEvents = result.map { document ->
                    Event(
                        id = document.id,
                        name = document.getString("Name") ?: "Unknown Event",
                        artist = document.getString("Artist") ?: "",
                        genre = document.getString("Genre") ?: "Other",
                        rawImageRef = document.getString("Event Image") ?: ""
                    )
                }
                allEvents = fetchedEvents

                availableGenres = fetchedEvents.map { it.genre }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
            }
            .addOnFailureListener { e ->
                Log.e("TicketLah", "Error loading data", e)
            }
    }

    // --- SEARCH & FILTER LOGIC ---
    val displayedEvents = remember(searchQuery, selectedGenre, allEvents) {
        allEvents.filter { event ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                event.artist.contains(searchQuery, ignoreCase = true) ||
                        event.name.contains(searchQuery, ignoreCase = true)
            }
            val matchesGenre = if (selectedGenre == null) true else {
                event.genre.equals(selectedGenre, ignoreCase = true)
            }
            matchesSearch && matchesGenre
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // SEARCH BAR
            SearchBarWithFilter(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClicked = { focusManager.clearFocus() },
                availableGenres = availableGenres,
                selectedGenre = selectedGenre,
                onGenreSelected = { genre -> selectedGenre = genre }
            )

            // --- REMOVED THE TEXT THAT SHOWED "FILTER: RAP" HERE ---

            Spacer(modifier = Modifier.height(16.dp))

            // LIST
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedEvents, key = { it.id }) { event ->
                    EventCard(event)
                }
            }
        }
    }
}

// --- SEARCH BAR WITH FILTER & DROPDOWN ---
@Composable
fun SearchBarWithFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClicked: () -> Unit,
    availableGenres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Text Field
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search Artist...", fontSize = 14.sp, color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchClicked() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFE0E0E0),
                unfocusedContainerColor = Color(0xFFE0E0E0),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Search Icon
        IconButton(onClick = onSearchClicked, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Filter Icon with Dropdown
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_filter_alt_24),
                    contentDescription = "Filter",
                    tint = if (selectedGenre != null) Color.Blue else Color.Black
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                // Option to Clear Filter
                DropdownMenuItem(
                    text = { Text("All Genres", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onGenreSelected(null)
                        showMenu = false
                    }
                )
                Divider()

                // Dynamic Genre Options
                availableGenres.forEach { genre ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = genre,
                                color = if (selectedGenre == genre) Color.Blue else Color.Black
                            )
                        },
                        onClick = {
                            onGenreSelected(genre)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

// --- HELPER FUNCTION TO RESOLVE URLS ---
@Composable
fun getImageUrl(rawUrl: String): String {
    var httpUrl by remember { mutableStateOf("") }
    LaunchedEffect(rawUrl) {
        val cleaned = rawUrl.trim()
        if (cleaned.isEmpty()) {
            httpUrl = ""
            return@LaunchedEffect
        }
        if (cleaned.startsWith("gs://")) {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(cleaned)
                ref.downloadUrl
                    .addOnSuccessListener { uri -> httpUrl = uri.toString() }
                    .addOnFailureListener { httpUrl = "" }
            } catch (e: Exception) { Log.e("ImageLoad", "Bad GS", e) }
        } else {
            httpUrl = cleaned
        }
    }
    return httpUrl
}

@Composable
fun EventCard(event: Event) {
    val finalImageUrl = getImageUrl(rawUrl = event.rawImageRef)

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                if (finalImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(finalImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { Log.e("CoilError", "Error loading") }
                    )
                }

                // Show genre tag on image
                if(event.genre.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = event.genre,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Column {
                    Text(
                        text = event.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
