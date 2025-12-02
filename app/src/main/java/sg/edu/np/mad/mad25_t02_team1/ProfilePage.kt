package sg.edu.np.mad.mad25_t02_team1.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.models.Account
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAD25_T02_Team1Theme {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen() {

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    var account by remember { mutableStateOf<Account?>(null) }

    // 🔥 Load from Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("Account")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    account = snap.documents.first().toObject(Account::class.java)
                }
            }
    }



    if (account == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(20.dp))

        ProfileText("Name", account!!.name)
        ProfileText("Phone Number", account!!.phone)
        ProfileText("Email", account!!.email)

        Spacer(Modifier.height(30.dp))
        val context = LocalContext.current
        Button(
            onClick = {
                context.startActivity(Intent(context, EditProfileActivity::class.java))
            },
            modifier = Modifier.width(180.dp)
        ) {
            Text("Edit Profile")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { FirebaseAuth.getInstance().signOut() },
            modifier = Modifier.width(180.dp)
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun ProfileText(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(4.dp)) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(12.dp),
            color = Color.Black
        )
    }
}
