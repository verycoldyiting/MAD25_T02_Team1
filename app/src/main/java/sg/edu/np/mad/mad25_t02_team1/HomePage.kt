package sg.edu.np.mad.mad25_t02_team1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.storage.FirebaseStorage
import sg.edu.np.mad.mad25_t02_team1.ui.BookingHistoryScreen
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import androidx.navigation.NavGraph.Companion.findStartDestination

// ----------------------------
// DATA CLASS
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
    var Date: Timestamp? = null
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
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
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
            composable(BottomNavItem.Search.route) {  }
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

    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance()
            .collection("Events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allEvents = snapshot.documents.mapNotNull { it.toObject(Event::class.java) }
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
                    UpcomingEventCard(event)
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
                AvailableEventCard(event)
            }
        }
    }
}


// ----------------------------
// UPCOMING EVENT CARD
// ----------------------------
@Composable
fun UpcomingEventCard(event: Event) {

    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(event.EventImage) {
        val gsUrl = event.EventImage.trim()
        if (gsUrl.startsWith("gs://")) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
            ref.downloadUrl.addOnSuccessListener { url ->
                imageUrl = url.toString()
            }.addOnFailureListener {
                imageUrl = null // Handle error
            }
        } else {
            imageUrl = gsUrl
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(160.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(imageUrl ?: R.drawable.ticket), // Fallback to a placeholder
                contentDescription = event.Name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
            )

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
fun AvailableEventCard(event: Event) {

    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(event.EventImage) {
        val gsUrl = event.EventImage.trim()
        if (gsUrl.startsWith("gs://")) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl)
            ref.downloadUrl.addOnSuccessListener { url ->
                imageUrl = url.toString()
            }.addOnFailureListener {
                imageUrl = null // Handle error
            }
        } else {
            imageUrl = gsUrl
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {

            Image(
                painter = rememberAsyncImagePainter(imageUrl ?: R.drawable.ticket), // Fallback to a placeholder
                contentDescription = event.Name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
            )

            Text(
                text = event.Caption.ifEmpty { "Event Name" },
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
