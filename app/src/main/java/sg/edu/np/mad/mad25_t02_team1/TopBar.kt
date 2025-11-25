package sg.edu.np.mad.mad25_t02_team1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage


@Composable
fun TicketLahHeader() {

    val logoUrl =
        "https://firebasestorage.googleapis.com/v0/b/mad25t02team1.firebasestorage.app/o/image-removebg-preview.png?alt=media&token=3b068498-aeb6-4491-8ab2-17c10f807a2d"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)               // ⭐ Taller header (more professional)
            .background(Color(0xFF007BFF)),
        contentAlignment = Alignment.Center   // ⭐ Center everything
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "TicketLah Logo",
                modifier = Modifier.size(40.dp)   // ⭐ Larger icon
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "TicketLah!",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
