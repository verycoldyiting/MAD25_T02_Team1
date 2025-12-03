package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProfileScreen() }
    }
}

@Composable
fun ProfileScreen() {

    //firebase setup
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser!!
    val db = FirebaseFirestore.getInstance()

    //will recompose the screen whenever these values change
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val email = user.email ?: ""
    var imageUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    //auto refresh
    LaunchedEffect(user.uid) {

        db.collection("Account")
            .whereEqualTo("uid", user.uid)
            .addSnapshotListener { snap, _ ->

                //listen to firestore account document
                if (snap != null && !snap.isEmpty) {
                    val doc = snap.documents[0]
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                }
            }
        //load profile image from firebase
        FirebaseStorage.getInstance()
            .reference.child("profile/${user.uid}.jpg")
            .downloadUrl
            .addOnSuccessListener { imageUrl = it.toString() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AsyncImage(
            model = imageUrl.ifEmpty { "https://via.placeholder.com/150" },
            contentDescription = "",
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )

        Spacer(Modifier.height(20.dp))

        Text(name, style = MaterialTheme.typography.headlineSmall)
        Text(email)
        Text(phone)

        Spacer(Modifier.height(35.dp))

        Button(
            onClick = {
                //brings u to edit profile page
                context.startActivity(Intent(context, EditProfileActivity::class.java))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = YELLOW,
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(160.dp)
        ) {
            Text("Edit Profile")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                auth.signOut() //sign user out of firebase auth
                context.startActivity(Intent(context, LoginScreen::class.java)) //redirect to login page
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = YELLOW,
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(160.dp)
        ) {
            Text("Logout")
        }
    }
}

