package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import kotlinx.coroutines.tasks.await
import sg.edu.np.mad.mad25_t02_team1.models.Event
import androidx.navigation.compose.currentBackStackEntryAsState




class HomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MAD25_T02_Team1Theme {
                HomePageScaffold()
            }
        }
    }
}


sealed class AppRoute(val route: String) {
    object Chatbot : AppRoute("chatbot")
}


// SCAFFOLD WITH BOTTOM BAR
@Composable
fun HomePageScaffold(startRoute: String? = null) {
    // initialise nav controller to manage app navigation state
    val navController = rememberNavController()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank()) {
            navController.navigate(startRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val speechLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.get(0)
                        ?.lowercase()

                spokenText?.let {
                    handleSpeechNavigation(it, navController, context) { newTab ->
                        selectedTab = newTab
                    }
                }
            }
        }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Scaffold(
        topBar = {
            if (currentRoute != AppRoute.Chatbot.route) {
                TicketLahHeader()
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { item ->
                    selectedTab = item
                    // implementation of standard navigation logic, this logic prevents multiple copies of the same destination
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRoute != AppRoute.Chatbot.route) {

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    //Speech FAB
                    FloatingActionButton(
                        onClick = {
                            startSpeech(speechLauncher)
                        },
                        containerColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speech Navigation",
                            tint = Color.Black
                        )
                    }

                    //Chatbot fab
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(AppRoute.Chatbot.route)
                        },
                        containerColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        shape = CircleShape
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chatbot),
                            contentDescription = "Chatbot",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    )
    { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomePageContent() }
            composable(BottomNavItem.Search.route) { ExploreEventsApp() }
            composable(BottomNavItem.Tickets.route) { BookingHistoryScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }
            composable(AppRoute.Chatbot.route) {
                ChatbotScreen(navController)
            }
        }
    }
}

// PAGE CONTENT
@Composable
fun HomePageContent() {

    var upcomingEvents by remember { mutableStateOf(listOf<Event>()) }
    var availableEvents by remember { mutableStateOf(listOf<Event>()) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance()
            .collection("Events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {

                    val allEvents = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Event::class.java)
                    }

                    // Sort by date field from new model
                    val sorted = allEvents.sortedBy { it.date }

                    upcomingEvents = sorted.take(3)
                    availableEvents = sorted
                }
            }
        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Upcoming Events",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // horizontal scrolling for the featured 'Upcoming' section
        if (upcomingEvents.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(upcomingEvents) { event ->
                    UpcomingEventCard(
                        event = event,
                        onClick = {
                            val intent = Intent(context, EventDetailsActivity::class.java)
                            intent.putExtra("EVENT_ID", event.id)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        } else {
            Text("Loading upcoming events...", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Available Events",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // vertical scrolling for the main event list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(availableEvents) { event ->
                AvailableEventCard(
                    event = event,
                    onClick = {
                        val intent = Intent(context, EventDetailsActivity::class.java)
                        intent.putExtra("EVENT_ID", event.id)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

// UPCOMING EVENT CARD
@Composable
fun UpcomingEventCard(event: Event, onClick: () -> Unit) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(event.eventImage) {
        isLoading = true
        val gsUrl = event.eventImage?.trim().orEmpty()

        imageUrl = if (gsUrl.startsWith("gs://")) {
            try {

                FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
                    .downloadUrl.await().toString()
            } catch (e: Exception) {
                null
            }
        } else {
            gsUrl
        }

        isLoading = false
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column {

            Box(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                if (isLoading) {
                    CircularProgressIndicator()

                } else if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "Error"
                    )
                }
            }

            Text(
                text = event.name ?: "Event Name",
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AvailableEventCard(event: Event, onClick: () -> Unit) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // image logic mirrored here for consistency across both card types
    LaunchedEffect(event.eventImage) {
        isLoading = true
        val gsUrl = event.eventImage?.trim().orEmpty()
        imageUrl = if (gsUrl.startsWith("gs://")) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
                    .downloadUrl.await().toString()
            } catch (e: Exception) {
                null
            }
        } else {
            gsUrl
        }
        isLoading = false
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "Error"
                    )
                }
            }

            Text(
                text = event.caption ?: "Event Name",
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun handleSpeechNavigation(
    text: String,
    navController: androidx.navigation.NavController,
    context: android.content.Context,
    setSelectedTab: (BottomNavItem) -> Unit
) {
    fun go(item: BottomNavItem) {
        setSelectedTab(item)
        navController.navigate(item.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    when {
        text.contains("home") -> go(BottomNavItem.Home)
        text.contains("search") || text.contains("explore") -> go(BottomNavItem.Search)
        text.contains("ticket") -> go(BottomNavItem.Tickets)
        text.contains("profile") -> go(BottomNavItem.Profile)
        text.contains("chatbot") || text.contains("chat") || text.contains("help") || text.contains("assistance") -> {

            navController.navigate(AppRoute.Chatbot.route) {
                launchSingleTop = true
            }
        }
        else -> {
            Toast.makeText(context, "Sorry, I didn't understand", Toast.LENGTH_SHORT).show()
        }
    }
}


fun startSpeech(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
    }
    launcher.launch(intent)
}
