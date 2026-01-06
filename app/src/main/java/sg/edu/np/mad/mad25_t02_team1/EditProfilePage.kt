package sg.edu.np.mad.mad25_t02_team1

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EditProfileScreen() }
    }
}

@Composable
fun EditProfileScreen() {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser!!
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Password fields
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Photo Picker for Android 13+
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    // In the event there's a use of older Android versions, this is used as the fallback
    val legacyImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    // Permission launcher to read media images for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                legacyImagePickerLauncher.launch("image/*")
            }
        } else {
            Toast.makeText(context, "Permission denied. Cannot access photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to handle photo selection
    fun selectPhoto() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                when {
                    ContextCompat.checkSelfPermission(context, permission) ==
                            PackageManager.PERMISSION_GRANTED -> {
                        legacyImagePickerLauncher.launch("image/*")
                    }
                    else -> {
                        permissionLauncher.launch(permission)
                    }
                }
            }
            else -> {
                legacyImagePickerLauncher.launch("image/*")
            }
        }
    }

    // Load profile data
    LaunchedEffect(Unit) {
        db.collection("Account")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents[0]
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                    email = doc.getString("email") ?: user.email ?: ""
                    profileImageUrl = doc.getString("profileImageUrl")
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Profile Picture
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val displayImageUrl = selectedImageUri?.toString() ?: profileImageUrl

                if (displayImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(displayImageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { selectPhoto() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable { selectPhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = { selectPhoto() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Yellow, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = Color.Black
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Profile Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Change Password",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Leave blank if you don't want to change password",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            // Current Password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = if (showCurrentPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            imageVector = if (showCurrentPassword)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = if (showNewPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = if (showConfirmPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(25.dp))

            Button(
                onClick = {
                    // Validation
                    if (!isValidPhone(phone)) {
                        Toast.makeText(context, "Phone must start with 8 or 9 and be 8 digits.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val isChangingPassword = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

                    if (isChangingPassword) {
                        if (currentPassword.isBlank()) {
                            Toast.makeText(context, "Please enter current password.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    isLoading = true

                    updateProfile(
                        context = context,
                        auth = auth,
                        db = db,
                        storage = storage,
                        name = name,
                        phone = phone,
                        email = email,
                        selectedImageUri = selectedImageUri,
                        currentPassword = if (isChangingPassword) currentPassword else null,
                        newPassword = if (isChangingPassword) newPassword else null,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            (context as Activity).finish()
                        },
                        onError = { error ->
                            isLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(200.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black
                    )
                } else {
                    Text("Save Changes")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// Function to update profile
private fun updateProfile(
    context: android.content.Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    name: String,
    phone: String,
    email: String,
    selectedImageUri: Uri?,
    currentPassword: String?,
    newPassword: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val user = auth.currentUser ?: run {
        onError("User not logged in")
        return
    }

    //  Define updateFirestore first (used by uploadImageAndUpdate)
    fun updateFirestore(imageUrl: String?) {
        db.collection("Account")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val docId = snap.documents[0].id
                    val updates = mutableMapOf<String, Any>(
                        "name" to name,
                        "phone" to phone,
                        "email" to email
                    )
                    if (imageUrl != null) {
                        updates["profileImageUrl"] = imageUrl
                    }

                    db.collection("Account").document(docId)
                        .update(updates)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError("Failed to update profile: ${e.message}")
                        }
                } else {
                    onError("Account not found")
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to fetch account: ${e.message}")
            }
    }

    //  Define uploadImageAndUpdate (used by updateEmail)
    fun uploadImageAndUpdate() {
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_images/${user.uid}/${UUID.randomUUID()}.jpg")
            storageRef.putFile(selectedImageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updateFirestore(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    onError("Failed to upload image: ${e.message}")
                }
        } else {
            updateFirestore(null)
        }
    }

    //  Define updateEmail next (used by updatePassword)
    fun updateEmail() {
        if (email != user.email) {
            user.verifyBeforeUpdateEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(context, "Verification email sent. Please verify to complete email change.", Toast.LENGTH_LONG).show()
                    uploadImageAndUpdate()
                }
                .addOnFailureListener { e ->
                    onError("Failed to update email: ${e.message}")
                }
        } else {
            uploadImageAndUpdate()
        }
    }

    //  Entry point
    if (currentPassword != null && newPassword != null) {
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        updateEmail()
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to update password: ${e.message}")
                    }
            }
            .addOnFailureListener {
                onError("Current password is incorrect")
            }
    } else {
        updateEmail()
    }
}
