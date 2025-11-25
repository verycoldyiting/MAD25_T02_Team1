package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    @get:PropertyName("Name") @set:PropertyName("Name")
    var name: String = "",
    @get:PropertyName("Email") @set:PropertyName("Email")
    var email: String = "",
    @get:PropertyName("Phone Number") @set:PropertyName("Phone Number")
    var phoneNumber: String = "",
    @get:PropertyName("CreatedAt") @set:PropertyName("CreatedAt")
    var createdAt: Timestamp? = null
)
