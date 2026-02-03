package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import sg.edu.np.mad.mad25_t02_team1.auth.BiometricAuthHelper
import sg.edu.np.mad.mad25_t02_team1.ui.theme.MAD25_T02_Team1Theme

class LoginScreen : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()

        // Read biometric preference (default = enabled)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean("biometric_enabled", true)

        // Biometric gate (ONLY if user logged in AND enabled)
        if (auth.currentUser != null && biometricEnabled) {
            val biometricHelper = BiometricAuthHelper(this)

            if (biometricHelper.isBiometricAvailable()) {
                biometricHelper.authenticate(
                    onSuccess = {
                        startActivity(Intent(this, HomePage::class.java))
                        finish()
                    },
                    onError = {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                )
                return
            }
        }

        //
        // Normal email/password login UI
        enableEdgeToEdge()
        setContent {
            MAD25_T02_Team1Theme {
                LoginContent()
            }
        }
    }
}

@Composable
fun LoginContent() {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        TicketLahHeader()

        Spacer(Modifier.height(40.dp))

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

            Text(
                text = "Please Login",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector =
                                if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(context, ForgotPasswordActivity::class.java)
                    )
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Forgot Password?",
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(
                            context,
                            "Please fill in all fields",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: return@addOnSuccessListener

                            db.collection("Account")
                                .whereEqualTo("uid", uid)
                                .get()
                                .addOnSuccessListener { snap ->
                                    if (snap.isEmpty) {
                                        Toast.makeText(
                                            context,
                                            "User not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val intent =
                                            Intent(context, HomePage::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish()
                                    }
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                it.message ?: "Login failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2B705),
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

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color.Gray
                )

                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(context, RegisterPage::class.java)
                        )
                    },
                    contentPadding = PaddingValues(0.dp), // key fix
                    modifier = Modifier.height(18.dp)     // optional: tighter alignment
                ) {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDecoration = TextDecoration.Underline)
                    )
                }
            }

        }
    }
}
