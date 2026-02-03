package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.util.Log
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
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.Event

@Composable
fun ExploreEventsApp() {

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val result = db.collection("Events").get().await()
            val fetchedEvents = result.documents.mapNotNull { document ->
                document.toObject(Event::class.java)?.copy(
                    id = document.id // snsure the ID is captured from the document metadata
                )
            }
            allEvents = fetchedEvents

            // extracts unique genres for the filter dropdown
            availableGenres = fetchedEvents.mapNotNull { it.genre }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            Log.e("ExploreEvent", "Error loading event data", e)
        }
    }

    // reactive filter logic
    // recalculates 'displayedEvents' only when search, genre, or the list changes.

    val displayedEvents = remember(searchQuery, selectedGenre, allEvents) {
        allEvents.filter { event ->
            // Search Requirement: Matches Artist OR Event Name
            val matchesSearch = if (searchQuery.isBlank()) true else {
                event.artist?.contains(searchQuery, ignoreCase = true) == true ||
                        event.name?.contains(searchQuery, ignoreCase = true) == true
            }
            // Filter Requirement: Matches selected Genre
            val matchesGenre = if (selectedGenre == null) true else {
                event.genre?.equals(selectedGenre, ignoreCase = true) == true
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
            // search bar and filter icons
            SearchBarWithFilter(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClicked = { focusManager.clearFocus() },
                availableGenres = availableGenres,
                selectedGenre = selectedGenre,
                onGenreSelected = { genre -> selectedGenre = genre }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // lazy column
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = {
                            // Intent to navigate to Details page, passing the unique Event ID
                            val intent = Intent(context, EventDetailsActivity::class.java)
                            intent.putExtra("EVENT_ID", event.id)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

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

        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search Artist...", fontSize = 14.sp, color = Color.Gray) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchClicked() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0F0F0),
                unfocusedContainerColor = Color(0xFFF0F0F0),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // filter Dropdown logic
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = "Filter",
                    // change colour if a filter is active
                    tint = if (selectedGenre != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                // reset filter
                DropdownMenuItem(
                    text = { Text("All Genres", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onGenreSelected(null)
                        showMenu = false
                    }
                )


                // list available genres
                availableGenres.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre, color = if (selectedGenre == genre) MaterialTheme.colorScheme.primary else Color.Black) },
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

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    // manages the final resolved image URL (handling gs:// + https://)
    var finalImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }

    LaunchedEffect(event.eventImage) {
        isLoadingImage = true
        val rawUrl = event.eventImage?.trim() ?: ""
        if (rawUrl.isEmpty()) {
            isLoadingImage = false
            return@LaunchedEffect
        }
        // If URL starts with gs://, we need to fetch the actual download URL
        finalImageUrl = if (rawUrl.startsWith("gs://")) {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(rawUrl)
                ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("ImageLoad", "Failed to get download URL for ${event.name}", e)
                null
            }
        } else {
            rawUrl
        }
        isLoadingImage = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                // loading state vs success state for image
                if (isLoadingImage) {
                    CircularProgressIndicator()
                } else if (finalImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(finalImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "No Image Available", tint = Color.Gray)
                }

                if (event.genre?.isNotEmpty() == true) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = event.genre.orEmpty().uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // event details section
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)) {
                Text(
                    text = event.name.orEmpty(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = event.artist.orEmpty(),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}