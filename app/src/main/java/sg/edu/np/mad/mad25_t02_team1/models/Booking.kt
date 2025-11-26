package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.PropertyName

data class Booking(
    @DocumentId
    val id: String = "",

    @get:PropertyName("ConcertTitle")
    @set:PropertyName("ConcertTitle")
    var concertTitle: String? = null,

    @get:PropertyName("Name")
    @set:PropertyName("Name")
    var name: String? = null,

    // This field stores the "Event Image" string if present on the Booking doc (optional)
    @get:PropertyName("Event Image")
    @set:PropertyName("Event Image")
    var eventImage: String? = null,

    @get:PropertyName("Quantity")
    @set:PropertyName("Quantity")
    var quantity: Int? = null,

    @get:PropertyName("Category")
    @set:PropertyName("Category")
    var category: String? = null,

    @get:PropertyName("Section")
    @set:PropertyName("Section")
    var section: String? = null,

    @get:PropertyName("PaymentMethod")
    @set:PropertyName("PaymentMethod")
    var paymentMethod: String? = null,

    @get:PropertyName("TotalPrice")
    @set:PropertyName("TotalPrice")
    var totalPrice: Double? = null,

    @get:PropertyName("PurchaseTime")
    @set:PropertyName("PurchaseTime")
    var purchaseTime: Timestamp? = null,

    // NEW: EventID stored as string in BookingDetails (e.g., "Event003")
    @get:PropertyName("EventID")
    @set:PropertyName("EventID")
    var eventId: String? = null,

    // NEW: BookingDetailsAccID stored as a DocumentReference to /Account/A001
    @get:PropertyName("BookingDetailsAccID")
    @set:PropertyName("BookingDetailsAccID")
    var accountRef: DocumentReference? = null
)
