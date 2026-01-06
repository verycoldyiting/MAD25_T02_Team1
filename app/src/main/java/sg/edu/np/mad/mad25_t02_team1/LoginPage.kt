package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class LoginScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            MAD25_T02_Team1Theme {
                // Main scaffold provides structure with padding for system bars
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Load the login content with proper padding
                    LoginContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LoginContent(modifier: Modifier = Modifier) {
    // State variables to hold user input
    var email by remember { mutableStateOf("") } // Stores email input
    var password by remember { mutableStateOf("") } // Stores password input
    var passwordVisible by remember { mutableStateOf(false) } // Controls password visibility toggle

    val context = LocalContext.current

    // Initialize Firebase services
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Main container column with scrolling support
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), // Enable vertical scrolling for small screens
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        TicketLahHeader()

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Welcome to TicketLah!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please Login",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // EMAIL INPUT FIELD
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true // Prevent multiline input
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PASSWORD INPUT FIELD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, // Lock icon on left
                trailingIcon = {
                    // Toggle button to show/hide password
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                // Show password as dots or plain text based on passwordVisible state
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true // Prevent multiline input
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot password
            TextButton(
                onClick = {
                    // Navigate to ForgotPasswordActivity when clicked
                    val intent = Intent(context, ForgotPasswordActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Forgot Password?",
                    color = Color.Black,
                    fontSize = 14.sp,
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    // VALIDATION: Check if fields are empty
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button // Exit if validation fails
                    }

                    //  Authenticate with Firebase Authentication
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            // Get user ID from authentication result
                            val uid = authResult.user?.uid

                            // Check if UID exists
                            if (uid == null) {
                                Toast.makeText(context, "User not found. Please register.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // Verify user exists in Firestore database
                            db.collection("Account")
                                .whereEqualTo("uid", uid) // Query by user ID
                                .get()
                                .addOnSuccessListener { snap ->
                                    // Check if user document exists in Firestore
                                    if (snap.isEmpty) {
                                        // User authenticated but no account in database
                                        Toast.makeText(context, "User not found. Please register.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Login successful - navigate to home page
                                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(context, HomePage::class.java)
                                        // Clear back stack so user can't go back to login
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish() // Close login activity
                                    }
                                }
                                .addOnFailureListener {
                                    // Handle Firestore query error
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            // Handle authentication error (wrong password, user not found, etc.)
                            Toast.makeText(context, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Don't have an account? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                TextButton(
                    onClick = {
                        // Navigate to RegisterPage when clicked
                        val intent = Intent(context, RegisterPage::class.java)
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Register",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        style = TextStyle(textDecoration = TextDecoration.Underline)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}