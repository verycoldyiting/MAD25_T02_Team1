package sg.edu.np.mad.mad25_t02_team1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import sg.edu.np.mad.mad25_t02_team1.screens.ProfileScreen
import sg.edu.np.mad.mad25_t02_team1.screens.EditProfileScreen
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {

                // simple nav logic between 2 pages
                val showEdit = androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }

                if (!showEdit.value) {

                    ProfileScreen(
                        onEditProfile = { showEdit.value = true },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            startActivity(Intent(this, LoginScreen::class.java))
                            finish()
                        }
                    )

                } else {

                    EditProfileScreen(
                        onSave = { showEdit.value = false },
                        onCancel = { showEdit.value = false }
                    )
                }
            }
        }
    }
}

