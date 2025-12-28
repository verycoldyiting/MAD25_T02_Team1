package sg.edu.np.mad.mad25_t02_team1
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage



@Composable
fun TicketLahHeader() {

    val logoUrl =
        "https://firebasestorage.googleapis.com/v0/b/mad25t02team1.firebasestorage.app/o/6129816439578364825-removebg-preview.png?alt=media&token=4c6d8395-e830-4b26-8c1e-1273998283c0"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Bigger header
            .background(Color(0xFF00A2FF)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "TicketLah Logo",
                modifier = Modifier.size(150.dp) // Perfect size
            )

        }
    }
}
