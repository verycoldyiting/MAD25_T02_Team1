package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import androidx.compose.ui.unit.sp


@Composable
fun ProfileScreen() {

    // firebase setup
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser ?: return // Handle case where user is null
    val db = FirebaseFirestore.getInstance()

    // will recompose the screen whenever these values change
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    val email = user.email ?: ""

    val context = LocalContext.current

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var biometricEnabled by remember {
        mutableStateOf(
            prefs.getBoolean("biometric_enabled", true)
        )
    }


    // auto refresh with real-time updates
    LaunchedEffect(user.uid) {
        db.collection("Account")
            .whereEqualTo("uid", user.uid)
            .addSnapshotListener { snap, _ ->
                //listen to firestore account document
                if (snap != null && !snap.isEmpty) {
                    val doc = snap.documents[0]
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                    profileImageUrl = doc.getString("profileImageUrl")
                    android.util.Log.d(
                        "ProfileScreen",
                        "Profile loaded. Image URL: $profileImageUrl"
                    )
                }
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(20.dp))

        // profile picture with proper landing
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // default grey icon being used as placeholder
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Profile",
                    modifier = Modifier.size(65.dp),
                    tint = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(email, color = Color.Gray)
        Text(phone, color = Color.Gray)

        Spacer(Modifier.height(35.dp))

        // Biometric login toggle
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFF2B705),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "Biometric login",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        color = Color.Black
                    )

                    Text(
                        text = if (biometricEnabled) "Enabled" else "Disabled",
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }


                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { enabled ->
                        biometricEnabled = enabled
                        prefs.edit()
                            .putBoolean("biometric_enabled", enabled)
                            .apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        uncheckedThumbColor = Color.DarkGray,
                        checkedTrackColor = Color.Black.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color.Black.copy(alpha = 0.2f)
                    )
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        // Edit Profile Button
        Button(
            onClick = {
                val intent = Intent(context, EditProfileActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2B705),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Edit Profile", fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        // Logout button
        Button(
            onClick = {
                // Sign out from Firebase
                auth.signOut()

                // Navigate to login screen and clear back stack
                val intent = Intent(context, LoginScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2B705),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Logout", fontWeight = FontWeight.Medium)
        }
    }
}