package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.PropertyName

data class Booking(
    val id: String = "",
    @get:PropertyName("BookingDetailsAccID") @set:PropertyName("BookingDetailsAccID")
    var bookingDetailsAccID: DocumentReference? = null,
    @get:PropertyName("Category") @set:PropertyName("Category")
    var category: String = "",
    @get:PropertyName("ConcertTitle") @set:PropertyName("ConcertTitle")
    var concertTitle: String = "",
    @get:PropertyName("EventID") @set:PropertyName("EventID")
    var eventID: String = "",
    @get:PropertyName("EventTime") @set:PropertyName("EventTime")
    var eventTime: Timestamp? = null,
    @get:PropertyName("PaymentMethod") @set:PropertyName("PaymentMethod")
    var paymentMethod: String = "",
    @get:PropertyName("PurchaseTime") @set:PropertyName("PurchaseTime")
    var purchaseTime: Timestamp? = null,
    @get:PropertyName("Quantity") @set:PropertyName("Quantity")
    var quantity: Int = 0,
    @get:PropertyName("Section") @set:PropertyName("Section")
    var section: String = "",
    @get:PropertyName("TotalPrice") @set:PropertyName("TotalPrice")
    var totalPrice: Double = 0.0
)
