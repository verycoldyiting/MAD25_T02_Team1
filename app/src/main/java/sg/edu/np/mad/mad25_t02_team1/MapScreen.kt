package sg.edu.np.mad.mad25_t02_team1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

// Data class to hold pin information
data class MapPin(
    val name: String,
    val location: LatLng,
    val type: String, // "FOOD" or "TRANSIT"
    val rating: Double? = null
)

class VenueMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Get the Key safely from Gradle
        val apiKey = BuildConfig.MAPS_API_KEY

        // 2. Initialize Places
        try {
            if (apiKey.isNotEmpty() && !Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            } else if (apiKey.isEmpty()) {
                Log.e("VenueMap", "Warning: API Key is missing from local.properties")
                Toast.makeText(this, "Map Key Missing!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("VenueMap", "Places Error", e)
        }

        val venueName = intent.getStringExtra("VENUE_NAME") ?: "Singapore"

        setContent {
            MAD25_T02_Team1Theme {
                VenueMapScreenContent(venueName, { finish() })
            }
        }
    }
}

@Composable
private fun VenueMapScreenContent(
    venueName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State Variables ---
    val defaultLocation = LatLng(1.3521, 103.8198)
    var venueLocation by remember { mutableStateOf(defaultLocation) }
    var venueAddress by remember { mutableStateOf("Locating...") }

    // Toggle States for Pins
    var showFood by remember { mutableStateOf(false) }
    var showTransit by remember { mutableStateOf(false) }

    // Lists to hold data
    var foodPins by remember { mutableStateOf<List<MapPin>>(emptyList()) }
    var transitPins by remember { mutableStateOf<List<MapPin>>(emptyList()) }

    // Map Configuration
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // Permission State
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    // --- 1. Fetch Venue Coordinates (Background) ---
    LaunchedEffect(venueName) {
        scope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val coder = Geocoder(context)
                    val searchString = if (venueName.contains("Singapore", ignoreCase = true)) venueName else "$venueName, Singapore"

                    @Suppress("DEPRECATION")
                    val addressList = coder.getFromLocationName(searchString, 1)

                    if (!addressList.isNullOrEmpty()) {
                        val location = addressList[0]
                        val latLng = LatLng(location.latitude, location.longitude)
                        val addressText = if (location.maxAddressLineIndex >= 0) location.getAddressLine(0) else venueName

                        withContext(Dispatchers.Main) {
                            venueLocation = latLng
                            venueAddress = addressText
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VenueMap", "Geocoder Error", e)
            }
        }
    }

    // --- 2. Request User Location ---
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- 3. Helper Function: Fetch Food ---
    fun fetchFood() {
        Toast.makeText(context, "Searching restaurants...", Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            try {
                val client = Places.createClient(context)
                val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.RATING)
                val request = SearchByTextRequest.builder("restaurants near $venueName", fields).setMaxResultCount(10).build()
                val response = client.searchByText(request).await()

                val pins = response.places.sortedByDescending { it.rating }.take(5).mapNotNull {
                    it.latLng?.let { loc -> MapPin(it.name ?: "", loc, "FOOD", it.rating) }
                }
                withContext(Dispatchers.Main) { foodPins = pins }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error finding food: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // --- 4. Helper Function: Fetch Transit ---
    fun fetchTransit() {
        Toast.makeText(context, "Searching bus stops...", Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            try {
                val client = Places.createClient(context)
                val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
                val request = SearchByTextRequest.builder("bus stop near $venueName", fields).setMaxResultCount(10).build()
                val response = client.searchByText(request).await()

                val pins = response.places.mapNotNull {
                    it.latLng?.let { loc ->
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(venueLocation.latitude, venueLocation.longitude, loc.latitude, loc.longitude, results)
                        if (results[0] <= 300) MapPin(it.name ?: "", loc, "TRANSIT") else null
                    }
                }
                withContext(Dispatchers.Main) { transitPins = pins }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error finding transit: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp)) {
                SmallFloatingActionButton(
                    onClick = onBackClick,
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {

            // --- MAP SECTION ---
            Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission,
                        mapType = mapType
                    ),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    // 1. Main Venue Pin (Red)
                    Marker(
                        state = MarkerState(position = venueLocation),
                        title = venueName,
                        snippet = venueAddress,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // 2. Food Pins (Green)
                    if (showFood) {
                        foodPins.forEach { pin ->
                            Marker(
                                state = MarkerState(pin.location),
                                title = pin.name,
                                snippet = "Rating: ${pin.rating} stars",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }

                    // 3. Transit Pins (Azure/Blue)
                    if (showTransit) {
                        transitPins.forEach { pin ->
                            Marker(
                                state = MarkerState(pin.location),
                                title = pin.name,
                                snippet = "Bus Stop",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }
                }
            }

            // --- CONTROLS COLUMN ---
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(venueName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(venueAddress, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Get Directions
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(venueName)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(venueName)}"))
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Get Directions")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Map Type Chips
                Text("Map Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    item { MapTypeChip(selected = mapType == MapType.NORMAL, text = "Normal") { mapType = MapType.NORMAL } }
                    item { MapTypeChip(selected = mapType == MapType.SATELLITE, text = "Satellite") { mapType = MapType.SATELLITE } }
                    item { MapTypeChip(selected = mapType == MapType.HYBRID, text = "Hybrid") { mapType = MapType.HYBRID } }
                }

                // Explore Nearby Chips (Toggle Pins)
                Text("Explore Nearby", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {

                    // Food Chip
                    FilterChip(
                        selected = showFood,
                        onClick = {
                            showFood = !showFood
                            if (showFood && foodPins.isEmpty()) fetchFood()
                        },
                        label = { Text("Food") },
                        leadingIcon = {
                            if(showFood) Icon(Icons.Default.Star, null, Modifier.size(18.dp))
                            else Icon(Icons.Rounded.Restaurant, null, Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    // Transit Chip
                    FilterChip(
                        selected = showTransit,
                        onClick = {
                            showTransit = !showTransit
                            if (showTransit && transitPins.isEmpty()) fetchTransit()
                        },
                        label = { Text("Transit") },
                        leadingIcon = { Icon(Icons.Rounded.DirectionsBus, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE1F5FE),
                            selectedLabelColor = Color(0xFF0277BD)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MapTypeChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}