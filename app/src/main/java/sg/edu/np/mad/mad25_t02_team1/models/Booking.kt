package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

data class Booking(
    val id: String = "",
    val BookingDetailsAccID: DocumentReference? = null,
    val Category: String = "",
    val ConcertTitle: String = "",
    val EventID: String = "",
    val EventTime: Timestamp? = null,
    val PaymentMethod: String = "",
    val PurchaseTime: Timestamp? = null,
    val Quantity: Int = 0,
    val Section: String = "",
    val TotalPrice: Double = 0.0
)
