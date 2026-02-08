package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme
import sg.edu.np.mad.mad25_t02_team1.ui.theme.YELLOW

class RegisterPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MAD25_T02_Team1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(modifier: Modifier = Modifier) {

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        TicketLahHeader()

        Spacer(modifier = Modifier.height(56.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Registration", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 8) phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    val icon =
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    val icon =
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {

                    if (name.isBlank() || email.isBlank() || phone.isBlank() ||
                        password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }

                    if (!isValidPhone(phone)) {
                        errorMessage = "Phone number must start with 8 or 9!"
                        return@Button
                    }

                    if (!isValidPassword(password)) {
                        errorMessage =
                            "Password must contain:\n• 1 uppercase\n• 1 number\n• 1 special character\n• Minimum 8 characters"
                        return@Button
                    }

                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->

                            val uid = result.user?.uid ?: return@addOnSuccessListener

                            db.collection("Account")
                                .orderBy("accountId", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { snap ->

                                    val lastId = if (!snap.isEmpty)
                                        snap.documents[0].getString("accountId") ?: "A000"
                                    else "A000"

                                    val nextNum = lastId.substring(1).toInt() + 1
                                    val newId = "A" + nextNum.toString().padStart(3, '0')

                                    val userData = hashMapOf(
                                        "accountId" to newId,
                                        "uid" to uid,
                                        "name" to name,
                                        "email" to email,
                                        "phone" to phone
                                    )

                                    db.collection("Account")
                                        .document(newId)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            showSuccessDialog = true
                                        }
                                        .addOnFailureListener {
                                            errorMessage = it.message
                                        }
                                }
                                .addOnFailureListener {
                                    errorMessage = it.message
                                }

                        }.addOnFailureListener {
                            errorMessage = it.message
                        }

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YELLOW,
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(180.dp)
            ) {
                Text("Register Now")
            }

            errorMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Error") },
                    text = { Text(msg) }
                )
            }

            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(onClick = {
                            showSuccessDialog = false
                            val intent = Intent(context, LoginScreen::class.java)
                            context.startActivity(intent)
                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Registration Successful") },
                    text = { Text("Your account has been created. Please login.") }
                )
            }
        }
    }
}

// phone validation
fun isValidPhone(phone: String): Boolean {
    return phone.matches(Regex("^[89][0-9]{7}$"))
}

// password validation
fun isValidPassword(password: String): Boolean {
    val regex = Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!])(?!.*\\s).{8,}$")
    return regex.matches(password)
}
