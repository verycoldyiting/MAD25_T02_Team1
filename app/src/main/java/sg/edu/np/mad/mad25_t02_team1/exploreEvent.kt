package sg.edu.np.mad.mad25_t02_team1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import sg.edu.np.mad.mad25_t02_team1.models.Event // FIXED: Import the correct Event model
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import sg.edu.np.mad.mad25_t02_team1.ui.BookingHistoryScreen

// --- REMOVED THE LOCAL, INCORRECT DATA MODEL ---

class ExploreEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAD25_T02_Team1Theme {
                ExploreEventsScaffold()
            }
        }
    }
}

@Composable
fun ExploreEventsScaffold() {
    // 1. Setup NavController
    val navController = rememberNavController()
    // 2. Initial selected tab is Search (as this is the Explore screen)
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Search) }

    Scaffold(
        topBar = { TicketLahHeader() }, // Top Bar
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { item ->
                    selectedTab = item
                    // Navigation logic to switch tabs
                    navController.navigate(item.route) {
                        // Avoid building up large back stacks when switching tabs
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        // 3. NavHost handles screen switching
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Search.route, // Start on the search screen
            modifier = Modifier.padding(innerPadding) // Apply padding from bars
        ) {
            // Define all navigation destinations
            composable(BottomNavItem.Home.route) {
                // Placeholder content for the Home route
                HomePageContent()
            }

            composable(BottomNavItem.Search.route) {
                // The main content of this file
                ExploreEventsApp()
            }

            composable(BottomNavItem.Tickets.route) {
                // Placeholder content for the Tickets route
                BookingHistoryScreen()
            }

            composable(BottomNavItem.Profile.route) {
                // Placeholder content for the Profile route
                Text("Profile Screen Placeholder")
            }
        }
    }
}

@Composable
fun ExploreEventsApp() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val result = db.collection("Events").get().await()

            val fetchedEvents = result.documents.mapNotNull { document ->
                document.toObject(Event::class.java)?.copy(
                    id = document.id
                )
            }
            allEvents = fetchedEvents

            availableGenres = fetchedEvents.mapNotNull { it.genre }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            Log.e("ExploreEvent", "Error loading event data", e)
        }
    }

    val displayedEvents = remember(searchQuery, selectedGenre, allEvents) {
        allEvents.filter { event ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                event.artist?.contains(searchQuery, ignoreCase = true) == true ||
                        event.name?.contains(searchQuery, ignoreCase = true) == true
            }
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
            SearchBarWithFilter(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClicked = { focusManager.clearFocus() },
                availableGenres = availableGenres,
                selectedGenre = selectedGenre,
                onGenreSelected = { genre -> selectedGenre = genre }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = "Filter",
                    tint = if (selectedGenre != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("All Genres", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onGenreSelected(null)
                        showMenu = false
                    }
                )
                Divider()

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
fun EventCard(event: Event) {
    var finalImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }

    LaunchedEffect(event.eventImage) {
        isLoadingImage = true
        val rawUrl = event.eventImage?.trim() ?: ""
        if (rawUrl.isEmpty()) {
            isLoadingImage = false
            return@LaunchedEffect
        }
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
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
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
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = event.genre.orEmpty().uppercase(), // FIXED
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
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
