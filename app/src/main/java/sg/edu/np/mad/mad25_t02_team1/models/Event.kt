package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Event(
    val id: String = "",
    @get:PropertyName("Name") @set:PropertyName("Name")
    var name: String = "",
    @get:PropertyName("Artist") @set:PropertyName("Artist")
    var artist: String = "",
    @get:PropertyName("Caption") @set:PropertyName("Caption")
    var caption: String = "",
    @get:PropertyName("Date") @set:PropertyName("Date")
    var date: Timestamp? = null,
    @get:PropertyName("Description") @set:PropertyName("Description")
    var description: String = "",
    @get:PropertyName("Genre") @set:PropertyName("Genre")
    var genre: String = "",
    @get:PropertyName("Venue") @set:PropertyName("Venue")
    var venue: String = "",
    @get:PropertyName("SeatCategory") @set:PropertyName("SeatCategory")
    var seatCategory: Map<String, SeatCategory> = emptyMap(),
    @get:PropertyName("EventImage") @set:PropertyName("EventImage")
    var eventImage: String = ""
)

data class SeatCategory(
    @get:PropertyName("Category") @set:PropertyName("Category")
    var category: String = "",
    @get:PropertyName("Price") @set:PropertyName("Price")
    var price: Double = 0.0,
    @get:PropertyName("Section") @set:PropertyName("Section")
    var section: List<String> = emptyList()
)
