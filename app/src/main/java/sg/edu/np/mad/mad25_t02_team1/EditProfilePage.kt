package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EditProfileScreen() }
    }
}

@Composable
fun EditProfileScreen() {

    //firebase setup
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser!!
    val db = FirebaseFirestore.getInstance()

    //store value
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val email = user.email ?: "" // cannot change


    val context = LocalContext.current

    // load profile
    LaunchedEffect(Unit) {
        db.collection("Account")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    //load from firestore
                    val doc = snap.documents[0]
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                }
            }
    }

    Scaffold(
        topBar = { TicketLahHeader() }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(20.dp))

            //editable field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") }
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {},
                enabled = false,
                label = { Text("Email (cannot change)") }
            )

            Spacer(Modifier.height(25.dp))

            Button(
                onClick = {
                    if (!isValidPhone(phone)) {
                        Toast.makeText(context, "Phone must start with 8 or 9 and be 8 digits.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    //update firestore
                    db.collection("Account")
                        .whereEqualTo("uid", user.uid)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                                val docId = snap.documents[0].id

                                db.collection("Account").document(docId)
                                    .update(
                                        mapOf(
                                            "name" to name,
                                            "phone" to phone,
                                            "email" to email
                                        )
                                    )
                            }
                        }

                    //pop-up message
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()

                    (context as Activity).finish() //close the screen
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YELLOW,
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(160.dp)
            ) {
                Text("Save Changes")
            }
        }
    }
}
