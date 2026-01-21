package sg.edu.np.mad.mad25_t02_team1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
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
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme // Ensure this import matches your actual theme package

class VenueMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Get the venue name passed from EventDetails
        val venueName = intent.getStringExtra("VENUE_NAME") ?: "Singapore"

        setContent {
            MAD25_T02_Team1Theme {
                // 2. Call the internal screen content directly
                VenueMapScreenContent(
                    venueName = venueName,
                    onBackClick = { finish() } // Close activity when back is clicked
                )
            }
        }
    }
}

// ==========================================
// THE UI LOGIC (Formerly in a separate file)
// ==========================================

@Composable
private fun VenueMapScreenContent(
    venueName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State Variables ---
    var venueLocation by remember { mutableStateOf<LatLng?>(null) }
    var venueAddress by remember { mutableStateOf("Locating...") }
    var isLoading by remember { mutableStateOf(true) }

    // Map Configuration
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val cameraPositionState = rememberCameraPositionState()

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

    // --- 1. Fetch Venue Coordinates ---
    LaunchedEffect(venueName) {
        scope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val coder = Geocoder(context)
                    val searchString = if (venueName.contains("Singapore", ignoreCase = true)) {
                        venueName
                    } else {
                        "$venueName, Singapore"
                    }
                    // Use standard synchronous call inside IO dispatcher
                    @Suppress("DEPRECATION")
                    val addressList = coder.getFromLocationName(searchString, 1)

                    if (!addressList.isNullOrEmpty()) {
                        val location = addressList[0]
                        val latLng = LatLng(location.latitude, location.longitude)
                        // Get safe address line
                        val addressText = if (location.maxAddressLineIndex >= 0) {
                            location.getAddressLine(0)
                        } else {
                            venueName
                        }

                        withContext(Dispatchers.Main) {
                            venueLocation = latLng
                            venueAddress = addressText
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                            isLoading = false
                        }
                    } else {
                        // Handle not found
                        withContext(Dispatchers.Main) { isLoading = false; venueAddress = "Location not found" }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false; venueAddress = "Error finding location" }
            }
        }
    }

    // --- 2. Request User Location ---
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            // Floating Back Button
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
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission,
                            mapType = mapType
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = true
                        )
                    ) {
                        venueLocation?.let { loc ->
                            Marker(
                                state = MarkerState(position = loc),
                                title = venueName,
                                snippet = venueAddress,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
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

                // Get Directions Button
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(venueName)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        // Safety check if maps is installed
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to browser if app not found
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(venueName)}"))
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
                Row(Modifier.padding(vertical = 8.dp)) {
                    MapTypeChip(selected = mapType == MapType.NORMAL, text = "Normal") { mapType = MapType.NORMAL }
                    Spacer(Modifier.width(8.dp))
                    MapTypeChip(selected = mapType == MapType.SATELLITE, text = "Satellite") { mapType = MapType.SATELLITE }
                    Spacer(Modifier.width(8.dp))
                    MapTypeChip(selected = mapType == MapType.HYBRID, text = "Hybrid") { mapType = MapType.HYBRID }
                }

                // Nearby Buttons
                Text("Explore Nearby", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(vertical = 8.dp)) {
                    NearbyOptionButton(Icons.Rounded.Restaurant, "Food", "restaurants near $venueName", context)
                    Spacer(Modifier.width(12.dp))
                    NearbyOptionButton(Icons.Rounded.DirectionsBus, "Transit", "bus stops near $venueName", context)
                }
            }
        }
    }
}

// --- Helper Composables ---

@Composable
fun MapTypeChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
fun NearbyOptionButton(icon: ImageVector, label: String, query: String, context: Context) {
    OutlinedButton(
        onClick = {
            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}"))
                context.startActivity(browserIntent)
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}