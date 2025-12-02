package sg.edu.np.mad.mad25_t02_team1.ui.screens

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import sg.edu.np.mad.mad25_t02_team1.models.Account
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAD25_T02_Team1Theme {
                EditProfileScreen()
            }
        }
    }
}

@Composable
fun EditProfileScreen() {

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    var accountId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔥 Pick Image
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        profileImageUri = uri
    }

    // 🔥 Load Account
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("Account")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    val acc = doc.toObject(Account::class.java)
                    if (acc != null) {
                        accountId = doc.id
                        name = acc.name
                        phone = acc.phone
                        email = acc.email
                    }
                }
            }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Profile Image
        AsyncImage(
            model = profileImageUri ?: "https://via.placeholder.com/150",
            contentDescription = "",
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { pickImageLauncher.launch("image/*") },
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })

        Spacer(Modifier.height(25.dp))

        Button(
            modifier = Modifier.width(180.dp),
            onClick = {
                val updateData = mapOf(
                    "name" to name,
                    "phone" to phone,
                    "email" to email
                )

                FirebaseFirestore.getInstance()
                    .collection("Account")
                    .document(accountId)
                    .update(updateData)

                profileImageUri?.let { uri ->
                    FirebaseStorage.getInstance()
                        .reference
                        .child("profile/$accountId.jpg")
                        .putFile(uri)
                }
            }
        ) {
            Text("Save Changes")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.width(180.dp),
            onClick = { FirebaseAuth.getInstance().sendPasswordResetEmail(email) }
        ) {
            Text("Reset Password")
        }
    }
}
