package sg.edu.np.mad.mad25_t02_team1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage

@Composable
fun BuyTicketScreen() {
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var seatNumber by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("seating_plan.png")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            imageUrl = uri.toString()
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 60.dp))
        } else if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Seating Plan",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        } else {
            Text("Image could not be loaded.", modifier = Modifier.padding(vertical = 60.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = seatNumber,
            onValueChange = { seatNumber = it },
            label = { Text("Seat Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Handle booking logic here */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Book")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BuyTicketScreenPreview() {
    MAD25_T02_Team1Theme {
        BuyTicketScreen()
    }
}
