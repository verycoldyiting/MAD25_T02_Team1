package sg.edu.np.mad.mad25_t02_team1.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.TicketLahHeader

@Composable
fun EditProfileScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
        topBar = { TicketLahHeader() }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AsyncImage(
                model = profileUrl.ifEmpty { Icons.Default.Person },
                contentDescription = "",
                modifier = Modifier.size(140.dp)
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    FirebaseFirestore.getInstance()
                        .collection("Account")
                        .document(uid)
                        .update(
                            "Name", name,
                            "Phone Number", phone,
                            "Email", email
                        )
                    onSave()
                },
                modifier = Modifier.width(160.dp)
            ) {
                Text("Save changes")
            }
        }
    }
}
