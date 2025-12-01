package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.storage.FirebaseStorage
import sg.edu.np.mad.mad25_t02_team1.ui.BookingHistoryScreen
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import kotlinx.coroutines.tasks.await

// ----------------------------
// DATA CLASS (Keep for compatibility)
// ----------------------------
@IgnoreExtraProperties
data class Event(

    @get:PropertyName("Name") @set:PropertyName("Name")
    var Name: String = "",

    @get:PropertyName("Caption") @set:PropertyName("Caption")
    var Caption: String = "",

    @get:PropertyName("Event Image") @set:PropertyName("Event Image")
    var EventImage: String = "",

    @get:PropertyName("Date") @set:PropertyName("Date")
    var Date: Timestamp? = null,

    var id: String = ""
)

// ----------------------------
// MAIN ACTIVITY
// ----------------------------
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

// ----------------------------
// SCAFFOLD WITH BOTTOM BAR
// ----------------------------
@Composable
fun HomePageScaffold() {

    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    Scaffold(
        topBar = { TicketLahHeader() },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedTab,
                onItemSelected = { item ->
                    selectedTab = item
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomePageContent() }
            composable(BottomNavItem.Search.route) { ExploreEventsApp() }
            composable(BottomNavItem.Tickets.route) { BookingHistoryScreen() }
            composable(BottomNavItem.Profile.route) {  }
        }
    }
}

// ----------------------------
// PAGE CONTENT
// ----------------------------
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
                        doc.toObject(Event::class.java)?.copy(id = doc.id)
                    }
                    val sorted = allEvents.sortedBy { it.Date }

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

// ----------------------------
// UPCOMING EVENT CARD
// ----------------------------
@Composable
fun UpcomingEventCard(
    event: Event,
    onClick: () -> Unit
) {

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(event.EventImage) {
        isLoading = true
        val gsUrl = event.EventImage.trim()
        imageUrl = if (gsUrl.startsWith("gs://")) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl).downloadUrl.await().toString()
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
                        contentDescription = event.Name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.ic_launcher_background), contentDescription = "Error")
                }
            }

            Text(
                text = event.Name.ifEmpty { "Event Name" },
                modifier = Modifier.padding(8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ----------------------------
// AVAILABLE EVENT CARD
// ----------------------------
@Composable
fun AvailableEventCard(
    event: Event,
    onClick: () -> Unit
) {

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(event.EventImage) {
        isLoading = true
        val gsUrl = event.EventImage.trim()
        imageUrl = if (gsUrl.startsWith("gs://")) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl).downloadUrl.await().toString()
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
                } else if (!imageUrl.isNullOrEmpty()){
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.Name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.ic_launcher_background), contentDescription = "Error")
                }
            }

            Text(
                text = event.Caption.ifEmpty { "Event Name" },
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}