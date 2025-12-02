package sg.edu.np.mad.mad25_t02_team1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.TicketLahHeader
import sg.edu.np.mad.mad25_t02_team1.BottomNavItem
import sg.edu.np.mad.mad25_t02_team1.BottomNavigationBar

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Fetch from Firestore
    LaunchedEffect(uid) {
        FirebaseFirestore.getInstance()
            .collection("Account")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                name = doc.getString("Name") ?: ""
                phone = doc.getString("Phone Number") ?: ""
                email = doc.getString("Email") ?: ""
                profileUrl = doc.getString("ProfileImage") ?: ""
            }
    }

    Scaffold(
        topBar = { TicketLahHeader() },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = BottomNavItem.Profile,
                onItemSelected = {}
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // PROFILE IMAGE
            Box(
                modifier = Modifier
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profileUrl.ifEmpty { Icons.Default.Person },
                    contentDescription = "",
                    modifier = Modifier.size(140.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            ProfileReadOnlyField("Name", name)
            ProfileReadOnlyField("Phone Number", phone)
            ProfileReadOnlyField("Email Address", email)
            ProfileReadOnlyField("Password", "********")

            Spacer(Modifier.height(20.dp))

            Button(onClick = onEditProfile, modifier = Modifier.width(160.dp)) {
                Text("Edit profile")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.width(160.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
            ) {
                Text("logout")
            }
        }
    }
}

@Composable
fun ProfileReadOnlyField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
