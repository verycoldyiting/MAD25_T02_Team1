package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Event(
    @DocumentId
    val id: String = "",

    @get:PropertyName("Artist")
    @set:PropertyName("Artist")
    var artist: String? = null,

    @get:PropertyName("Caption")
    @set:PropertyName("Caption")
    var caption: String? = null,

    @get:PropertyName("Date")
    @set:PropertyName("Date")
    var date: Timestamp? = null,

    @get:PropertyName("Description")
    @set:PropertyName("Description")
    var description: String? = null,

    @get:PropertyName("Event Image")
    @set:PropertyName("Event Image")
    var eventImage: String? = null,

    @get:PropertyName("Genre")
    @set:PropertyName("Genre")
    var genre: String? = null,

    @get:PropertyName("Name")
    @set:PropertyName("Name")
    var name: String? = null,

    @get:PropertyName("Price")
    @set:PropertyName("Price")
    var price: Double? = null,

    @get:PropertyName("Venue")
    @set:PropertyName("Venue")
    var venue: String? = null
)
data class SeatCategory(
    @get:PropertyName("Category") @set:PropertyName("Category")
    var category: String = "",
    @get:PropertyName("Price") @set:PropertyName("Price")
    var price: Double = 0.0,
    @get:PropertyName("Section") @set:PropertyName("Section")
    var section: List<String> = emptyList()
)
