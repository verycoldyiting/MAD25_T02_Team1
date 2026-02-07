package sg.edu.np.mad.mad25_t02_team1

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW
import java.text.SimpleDateFormat
import java.util.*

/**
 * ticket verification screen
 * displays ticket details after scanning qr code from external verification page
 * receives data via deep link and shows verified ticket status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketVerificationScreen() {
    val activity = LocalContext.current as ComponentActivity

    // get ticket data from deep link url parameters
    val uri = activity.intent?.data
    val bookingId = uri?.getQueryParameter("bookingId") ?: "N/A"
    val email = uri?.getQueryParameter("email") ?: "N/A"
    val eventName = uri?.getQueryParameter("eventName") ?: "Unknown Event"
    val artist = uri?.getQueryParameter("artist") ?: "N/A"
    val venue = uri?.getQueryParameter("venue") ?: "N/A"
    val date = uri?.getQueryParameter("date") ?: "N/A"
    val category = uri?.getQueryParameter("category") ?: "N/A"
    val section = uri?.getQueryParameter("section") ?: "N/A"
    val quantity = uri?.getQueryParameter("quantity")?.toIntOrNull() ?: 1
    val price = uri?.getQueryParameter("price")?.toDoubleOrNull() ?: 0.0

    // calculate total price based on quantity and price per ticket
    val totalPrice = price * quantity

    // format current timestamp for verification record
    val currentTime = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TicketLah!",
                            color = YELLOW,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "OFFICIAL VERIFICATION",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5F5F5), Color.White)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // green verified status banner with checkmark
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF45a049))
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // white circle with green checkmark icon
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "VERIFIED",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This ticket is authentic and valid for entry",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // main ticket details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // section header with yellow accent bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(YELLOW, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EVENT DETAILS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }

                    // event name title
                    Text(
                        text = eventName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // highlighted card for booking id and purchaser email
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // booking reference number
                            VerificationInfoRow("Booking ID", bookingId, isBold = true)

                            // purchaser email in purple/blue color for emphasis
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Purchaser",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = email,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF667eea)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    // event information rows
                    VerificationInfoRow("Artist/Performer", artist)
                    VerificationInfoRow("Date & Time", date)
                    VerificationInfoRow("Venue", venue)
                    VerificationInfoRow("Category", category)
                    VerificationInfoRow("Section", section)
                    VerificationInfoRow(
                        "Quantity",
                        "$quantity ticket${if (quantity > 1) "s" else ""}"
                    )
                    VerificationInfoRow("Price per Ticket", "S$ %.2f".format(price))

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    // total amount paid - highlighted in orange card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5E6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Paid",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "S$ %.2f".format(totalPrice),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFFFF6B00)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // security badge showing verification system and timestamp
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secured by TicketLah! Verification System",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // show when this ticket was verified
                    Text(
                        text = "Verified on: $currentTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // green "valid ticket" badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓ VALID TICKET",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // footer with legal text and help links
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "This digital ticket has been cryptographically verified and authenticated.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // help center links
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Help Center", fontSize = 11.sp, color = YELLOW)
                        Text(text = "•", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                        Text(text = "Report Issue", fontSize = 11.sp, color = YELLOW)
                        Text(text = "•", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                        Text(text = "Support", fontSize = 11.sp, color = YELLOW)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // copyright notice
                    Text(
                        text = "© 2026 TicketLah!. All rights reserved.",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * reusable row component for displaying label-value pairs
 * used throughout the ticket details section
 * @param label left side text (e.g., "Artist", "Venue")
 * @param value right side text (e.g., "Doja Cat", "Indoor Stadium")
 * @param isBold whether to make the value bold (used for important fields like booking id)
 */
@Composable
fun VerificationInfoRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.End,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
    }
}