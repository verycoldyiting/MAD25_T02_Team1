package sg.edu.np.mad.mad25_t02_team1

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketVerificationScreen() {
    val activity = LocalContext.current as ComponentActivity

    // Parse data from deep link URI
    val uri = activity.intent?.data
    val bookingId = uri?.getQueryParameter("bookingId") ?: ""
    val email = uri?.getQueryParameter("email") ?: ""
    val eventName = uri?.getQueryParameter("eventName") ?: ""
    val artist = uri?.getQueryParameter("artist") ?: ""
    val venue = uri?.getQueryParameter("venue") ?: ""
    val date = uri?.getQueryParameter("date") ?: ""
    val category = uri?.getQueryParameter("category") ?: ""
    val section = uri?.getQueryParameter("section") ?: ""
    val quantity = uri?.getQueryParameter("quantity")?.toIntOrNull() ?: 1
    val price = uri?.getQueryParameter("price")?.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ticket Verification",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Success Icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Ticket Verified!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ticket Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ticket Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Divider()

                    VerificationInfoRow("Booking ID", bookingId)
                    VerificationInfoRow("Email", email)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = eventName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider()

                    VerificationInfoRow("Artist", artist)
                    VerificationInfoRow("Date", date)
                    VerificationInfoRow("Venue", venue)
                    VerificationInfoRow("Category", category)
                    VerificationInfoRow("Section", section)
                    VerificationInfoRow("Quantity", "$quantity ticket(s)")
                    VerificationInfoRow("Price per Ticket", "S$ %.2f".format(price))

                    Divider()

                    // Total Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Paid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "S$ %.2f".format(price * quantity),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = YELLOW
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = "VALID TICKET",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun VerificationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontSize = 14.sp
        )
    }
}