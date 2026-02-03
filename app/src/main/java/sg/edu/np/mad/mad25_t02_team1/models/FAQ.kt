package sg.edu.np.mad.mad25_t02_team1.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class FAQ(

    @DocumentId
    val id: String = "",

    @get:PropertyName("Question")
    @set:PropertyName("Question")
    var question: String? = null,

    @get:PropertyName("Answer")
    @set:PropertyName("Answer")
    var answer: String? = null,

    @get:PropertyName("Category")
    @set:PropertyName("Category")
    var category: String? = null,

    @get:PropertyName("Keywords")
    @set:PropertyName("Keywords")
    var keywords: List<String> = emptyList(),

    @get:PropertyName("Priority")
    @set:PropertyName("Priority")
    var priority: Int = 0
)
