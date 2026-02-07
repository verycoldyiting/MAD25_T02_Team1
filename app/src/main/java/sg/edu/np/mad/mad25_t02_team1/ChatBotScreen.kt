package sg.edu.np.mad.mad25_t02_team1


import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import sg.edu.np.mad.mad25_t02_team1.models.Event
import sg.edu.np.mad.mad25_t02_team1.models.FAQ
import sg.edu.np.mad.mad25_t02_team1.models.SeatCategory
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import kotlin.math.min
import androidx.activity.result.ActivityResult




// REPOSITORY
object ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage.Bot("Hi! I'm TicketLah! Assistant Bot. Ask me anything about events!"))
    )
    val messages = _messages.asStateFlow()

    private val _suggestedPrompts = MutableStateFlow<List<String>>(emptyList())
    val suggestedPrompts = _suggestedPrompts.asStateFlow()

    // Stores the Event currently being discussed
    var selectedEvent: Event? = null

    fun clearContext() {
        selectedEvent = null
    }

    fun addMessage(msg: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(msg)
        _messages.value = currentList
    }

    fun removeLoading() {
        _messages.value = _messages.value.filter { it !is ChatMessage.Loading }
    }

    fun setPrompts(prompts: List<String>) {
        _suggestedPrompts.value = prompts
    }

    fun updateMessage(
        messageId: String,
        updater: (ChatMessage) -> ChatMessage
    ) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) updater(msg) else msg
        }
    }
}


// UTILITIES
object LevenshteinUtils {
    // Calculates the "edit distance" between two strings (how many typos apart they are)
    fun calculate(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0) { it }
        var newCost = IntArray(len0)

        for (j in 1 until len1) {
            newCost[0] = j
            for (i in 1 until len0) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                newCost[i] = min(min(cost[i] + 1, newCost[i - 1] + 1), cost[i - 1] + match)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[len0 - 1]
    }

//    fun isCloseMatch(input: String, target: String, threshold: Int = 2): Boolean {
//        val normalizedInput = input.lowercase().trim()
//        val normalizedTarget = target.lowercase().trim()
//
//        if (normalizedInput == normalizedTarget) return true
//        if (Math.abs(normalizedInput.length - normalizedTarget.length) > threshold) return false
//
//        return calculate(normalizedInput, normalizedTarget) <= threshold
//    }
}

object TextFuzzy {

    fun normalize(s: String): String {
        return s
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), " ")   // keep letters/numbers/spaces
            .replace(Regex("\\s+"), " ")          // collapse spaces
    }

    fun tokens(s: String): List<String> =
        normalize(s).split(" ").filter { it.isNotBlank() }

    // Levenshtein distance
    fun similarity(a: String, b: String): Double {
        val aa = normalize(a).replace(" ", "")
        val bb = normalize(b).replace(" ", "")
        if (aa.isBlank() || bb.isBlank()) return 0.0
        if (aa == bb) return 1.0
        val dist = LevenshteinUtils.calculate(aa, bb)
        val maxLen = maxOf(aa.length, bb.length).toDouble()
        return (1.0 - dist / maxLen).coerceIn(0.0, 1.0)
    }

    fun fuzzyContains(input: String, target: String, threshold: Double = 0.78): Boolean {
        val inTokens = tokens(input)
        val t = normalize(target)
        if (t.isBlank()) return false

        if (inTokens.any { similarity(it, t) >= threshold }) return true

        val windowSize = minOf(2, inTokens.size)
        if (windowSize >= 2) {
            for (i in 0..inTokens.size - 2) {
                val phrase = inTokens[i] + " " + inTokens[i + 1]
                if (similarity(phrase, t) >= threshold) return true
            }
        }
        return false
    }

    fun bestKeywordScore(input: String, keywords: List<String>, threshold: Double = 0.78): Double {
        var best = 0.0
        val inTokens = tokens(input)

        for (k in keywords) {
            val kk = normalize(k)
            for (t in inTokens) {
                best = maxOf(best, similarity(t, kk))
            }
            if (inTokens.size >= 2) {
                for (i in 0..inTokens.size - 2) {
                    val phrase = inTokens[i] + " " + inTokens[i + 1]
                    best = maxOf(best, similarity(phrase, kk))
                }
            }
        }
        return best
    }
}


class TranslationHelper {
    private val apiKey = BuildConfig.MY_API_KEY
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d("API_KEY_CHECK", "Key length=${apiKey.length}")
            val url = "https://translation.googleapis.com/language/translate/v2/detect?key=$apiKey"
            val body = JSONObject().put("q", text)
            val response = postJson(url, body.toString())

            JSONObject(response)
                .getJSONObject("data")
                .getJSONArray("detections")
                .getJSONArray(0)
                .getJSONObject(0)
                .getString("language")
        } catch (e: Exception) {
            "en"
        }
    }

    suspend fun translate(text: String, targetLang: String, sourceLang: String? = null): String {
        if (targetLang == sourceLang) return text

        return withContext(Dispatchers.IO) {
            try {
                val urlString =
                    "https://translation.googleapis.com/language/translate/v2?key=$apiKey"
                val jsonBody = JSONObject().apply {
                    put("q", text)
                    put("target", targetLang)
                    sourceLang?.let { put("source", it) }
                }
                val response = postJson(urlString, jsonBody.toString())
                Log.d("ChatBot", "Raw Translation Response: $response")

                val data = JSONObject(response).getJSONObject("data")
                val translations = data.getJSONArray("translations")
                val rawText = translations.getJSONObject(0).getString("translatedText")

                // Decode the HTML entities back to normal text
                HtmlCompat.fromHtml(rawText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            } catch (e: Exception) {
                Log.e("ChatBot", "Translation Error", e)
                text
            }
        }
    }

    private fun postJson(urlString: String, jsonBody: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            Log.e("TRANSLATE_HTTP", "HTTP $code: $text")
        }
        return text
    }
}


// VIEWMODEL
sealed class ChatMessage(val id: String = UUID.randomUUID().toString()) {
    data class User(val text: String) : ChatMessage()
    data class Bot(
        val text: String,
        val language: String = "en",
        val translatedText: String? = null,
        val showTranslateButton: Boolean = false
    ) : ChatMessage()

    data class BotEvent(val event: Event) : ChatMessage()
    data class BotCarousel(val events: List<Event>) : ChatMessage()
    object Loading : ChatMessage()
}

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val translator = TranslationHelper()

    val messages = ChatRepository.messages
    val suggestedPrompts = ChatRepository.suggestedPrompts

    private var eventsList: List<Event> = emptyList()
    private var faqList: List<FAQ> = emptyList()
    private var seatCategoryList: List<SeatCategory> = emptyList()


    // Time helpers

    private fun eventMillis(e: Event): Long? = e.date?.toDate()?.time

    private fun isUpcoming(e: Event): Boolean {
        val t = eventMillis(e) ?: return false
        return t >= System.currentTimeMillis()
    }

    private fun upcomingEvents(): List<Event> {
        return eventsList
            .filter { isUpcoming(it) }
            .sortedBy { it.date }
    }

    private fun getSgMonthIndex(date: java.util.Date): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Singapore"))
        cal.time = date
        return cal.get(java.util.Calendar.MONTH)
    }

    init {
        ChatRepository.clearContext()
        viewModelScope.launch {
            try {
                // Fetch All Data
                val eventSnapshot = db.collection("Events").get().await()
                eventsList = eventSnapshot.documents.mapNotNull { it.toObject(Event::class.java) }

                val faqSnapshot = db.collection("FAQ").get().await()
                faqList = faqSnapshot.documents.mapNotNull { it.toObject(FAQ::class.java) }

                val seatSnapshot = db.collection("SeatCategory").get().await()
                seatCategoryList =
                    seatSnapshot.documents.mapNotNull { it.toObject(SeatCategory::class.java) }

                if (ChatRepository.suggestedPrompts.value.isEmpty()) {
                    updateSuggestedPrompts("en")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isEventSpecificQuestion(input: String): Boolean {
        val keywords = listOf(
            "rain", "weather", "outdoor",
            "wheelchair", "accessible", "accessibility",
            "refund", "policy",
            "age", "kids", "child", "children",
            "time", "late", "parking",
            "food", "drink", "beverage", "alcohol",
            "venue", "location"
        )

        // if any keyword matches fuzzily, treat as event-specific
        return keywords.any { k -> TextFuzzy.fuzzyContains(input, k, threshold = 0.75) }
    }


    fun translateBotMessage(messageId: String, text: String, sourceLang: String) {
        viewModelScope.launch {
            val translated = translator.translate(
                text = text,
                targetLang = "en",
                sourceLang = sourceLang
            )
            ChatRepository.updateMessage(messageId) { msg ->
                if (msg is ChatMessage.Bot) {
                    msg.copy(translatedText = translated)
                } else msg
            }
        }
    }

    fun processUserMessage(inputText: String) {
        if (inputText.isBlank()) return
        addMessage(ChatMessage.User(inputText))
        addMessage(ChatMessage.Loading)

        viewModelScope.launch {
            // Detect & Translate
            val detectedLang = translator.detectLanguage(inputText)
            val englishInput = if (detectedLang != "en") {
                translator.translate(inputText, "en", detectedLang)
            } else inputText

            val clean = englishInput.lowercase()
            val normalizedInput = clean.replace("-", "").replace(" ", "")

            val upcomingSorted = upcomingEvents()

            // genre search

            val allGenreTags = eventsList.flatMap {
                it.genreNormalized.map { t -> t.lowercase().replace("-", "").replace(" ", "") }
            }.distinct()

            val foundGenre = allGenreTags.find { tag -> normalizedInput.contains(tag) }

            if (foundGenre != null) {
                removeLoading()

                // Only upcoming events in genre results
                val genreEvents = upcomingSorted.filter { event ->
                    event.genreNormalized.any {
                        it.lowercase().replace("-", "").replace(" ", "") == foundGenre
                    }
                }

                ChatRepository.selectedEvent = null

                if (genreEvents.isNotEmpty()) {
                    addMessage(
                        ChatMessage.Bot(
                            "Here are the ${foundGenre.replaceFirstChar { it.uppercase() }} events:"
                        )
                    )
                    addMessage(ChatMessage.BotCarousel(genreEvents))
                } else {
                    addMessage(ChatMessage.Bot("I couldn't find any upcoming events for $foundGenre."))
                }
                return@launch
            }

            // Month/Upcoming Filtering (UPCOMING ONLY)
            val isMonthRequest =
                clean.contains("jan") || clean.contains("feb") || clean.contains("mar") || clean.contains("apr") ||
                        clean.contains("may") || clean.contains("jun") || clean.contains("jul") || clean.contains("aug") ||
                        clean.contains("sep") || clean.contains("oct") || clean.contains("nov") || clean.contains("dec")

            val isGeneralRequest =
                TextFuzzy.fuzzyContains(clean, "upcoming", 0.75) ||
                        TextFuzzy.fuzzyContains(clean, "coming", 0.75) ||
                        TextFuzzy.fuzzyContains(clean, "next", 0.75) ||
                        (TextFuzzy.fuzzyContains(clean, "show", 0.75) && TextFuzzy.fuzzyContains(clean, "event", 0.75))


            if (isMonthRequest || isGeneralRequest) {
                removeLoading()

                if (upcomingSorted.isEmpty()) {
                    addMessage(ChatMessage.Bot("There are no upcoming events right now."))
                    return@launch
                }

                // Month Logic
                val monthMap = mapOf(
                    "jan" to 0, "january" to 0,
                    "feb" to 1, "february" to 1,
                    "mar" to 2, "march" to 2,
                    "apr" to 3, "april" to 3,
                    "may" to 4,
                    "jun" to 5, "june" to 5,
                    "jul" to 6, "july" to 6,
                    "aug" to 7, "august" to 7,
                    "sep" to 8, "september" to 8,
                    "oct" to 9, "october" to 9,
                    "nov" to 10, "november" to 10,
                    "dec" to 11, "december" to 11
                )

                val foundMonth = monthMap.entries.find { clean.contains(it.key) }
                if (foundMonth != null) {
                    val targetMonthIndex = foundMonth.value
                    val monthName = foundMonth.key.replaceFirstChar { it.uppercase() }

                    val filteredEvents = upcomingSorted.filter { event ->
                        event.date?.toDate()?.let { getSgMonthIndex(it) == targetMonthIndex } == true
                    }

                    if (filteredEvents.isNotEmpty()) {
                        addMessage(ChatMessage.Bot("Here are the upcoming events happening in $monthName:"))
                        addMessage(ChatMessage.BotCarousel(filteredEvents))
                    } else {
                        addMessage(ChatMessage.Bot("I couldn't find any upcoming events in $monthName."))
                    }
                    return@launch
                }

                // General "Upcoming" Logic
                val numberMatch = Regex("\\d+").find(clean)
                val countToTake = numberMatch?.value?.toIntOrNull()?.coerceIn(1, 10) ?: 5

                addMessage(ChatMessage.Bot("Here are the next $countToTake upcoming events:"))
                addMessage(ChatMessage.BotCarousel(upcomingSorted.take(countToTake)))
                return@launch
            }

            // Event match + FAQ
            //  Ensure event matching also ignores expired events

            val newEventFound =
                if (ChatRepository.selectedEvent != null && isEventSpecificQuestion(englishInput)) {
                    null
                } else {
                    checkForEventMatch(englishInput)
                }

            if (newEventFound != null) {
                ChatRepository.selectedEvent = newEventFound
            }

            val responseEnglish = generateSmartResponse(englishInput, newEventFound)

            val finalResponse = if (detectedLang != "en") {
                translator.translate(responseEnglish, detectedLang, "en")
            } else responseEnglish

            removeLoading()

            addMessage(
                ChatMessage.Bot(
                    text = finalResponse,
                    language = detectedLang,
                    showTranslateButton = detectedLang != "en"
                )
            )

            if (newEventFound != null && !isEventSpecificQuestion(englishInput)) {
                addMessage(ChatMessage.BotEvent(newEventFound))
            }

            updateSuggestedPrompts(detectedLang)
        }
    }

    // Response Generator
    private fun generateSmartResponse(input: String, newEvent: Event?): String {
        val clean = input.lowercase()
        val targetEvent = newEvent ?: ChatRepository.selectedEvent

        if (Regex("\\bhow are you\\b").containsMatchIn(clean) || Regex("\\bhow r u\\b").containsMatchIn(clean)) {
            return "I’m doing good! Tell me what event you’re looking for, or type 'upcoming events'."
        }

        if (targetEvent == null) {
            if (clean.contains("rain") || clean.contains("weather")) {
                return "It depends on whether the event is indoor or outdoor. Tell me the event name, or type 'upcoming events' to choose one."
            }
            if (clean.contains("wheelchair") || clean.contains("accessible") || clean.contains("accessibility")) {
                return "Most venues provide accessibility options, but it depends on the venue. Tell me the event name so I can check."
            }
            if (clean.contains("refund") || clean.contains("policy")) {
                return "Refund policies vary by organiser/event. Tell me the event name, or open an event to see its refund policy."
            }
        }

        if (targetEvent != null && isEventSpecificQuestion(clean)) {
            if (clean.contains("rain") || clean.contains("weather")) {
                return when (targetEvent.isOutdoor) {
                    true -> "This is an outdoor event, so weather conditions may affect the show. Please check official announcements closer to the event date."
                    false -> "This is an indoor event, so weather conditions will not affect the event."
                    else -> "Please check official announcements for weather-related updates for this event."
                }
            }

            if (clean.contains("wheelchair") || clean.contains("accessible") || clean.contains("accessibility")) {
                return when (targetEvent.isWheelchairAccessible) {
                    true -> "Yes, ${targetEvent.venue ?: "this venue"} is wheelchair accessible. If you need help on-site, please approach the venue staff."
                    false -> "Unfortunately, ${targetEvent.venue ?: "this venue"} is not wheelchair accessible based on current info. You may want to contact the venue to confirm available assistance."
                    else -> "I don’t have confirmed accessibility info for ${targetEvent.venue ?: "the venue"}. Please check with the venue directly for the latest details."
                }
            }
        }

        val matchedFaq = faqList
            .map { faq ->
                val score = TextFuzzy.bestKeywordScore(
                    input = clean,
                    keywords = faq.keywords,
                    threshold = 0.75
                )
                faq to score
            }
            .filter { it.second >= 0.75 }
            .maxWithOrNull(compareBy<Pair<FAQ, Double>> { it.second }.thenBy { it.first.priority })
            ?.first


        if (matchedFaq != null) {
            val genericAnswer = matchedFaq.answer ?: "I'm not sure."
            return if (targetEvent != null) {
                customizeAnswerForEvent(matchedFaq.category, genericAnswer, targetEvent)
            } else genericAnswer
        }

        if (newEvent != null) {
            return "I found ${newEvent.name}! You can ask me about tickets, venue, timing, or accessibility."
        }

        if (Regex("^(hi|hello|hey)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clean)) {
            return "Hello! I can help answer questions about upcoming concerts and events."
        }

        return "I'm not sure. Try asking about a specific artist, genre (like 'Pop'), or event details."
    }

    private fun customizeAnswerForEvent(category: String?, genericAnswer: String, event: Event): String {
        return when (category) {
            "tickets", "payment" -> {
                val prices = seatCategoryList.map { it.price }
                if (prices.isNotEmpty()) {
                    val minStr = String.format("%.2f", prices.minOrNull() ?: 0.0)
                    val maxStr = String.format("%.2f", prices.maxOrNull() ?: 0.0)
                    "For ${event.artist}, ticket prices range from $$minStr to $$maxStr. $genericAnswer"
                } else {
                    "For ${event.artist}, check the app for specific pricing. $genericAnswer"
                }
            }

            "venue", "parking", "food" -> "This event is at ${event.venue}. $genericAnswer"
            "timing", "late_entry" -> "The event starts on ${formatDate(event.date)}. $genericAnswer"

            "refunds", "policy" -> {
                val policy = event.refundPolicy ?: "Standard rules apply"
                "For ${event.artist}, the policy is: $policy. $genericAnswer"
            }

            "accessibility", "wheelchair", "disability", "handicap" -> {
                val accessText = when (event.isWheelchairAccessible) {
                    true -> "Yes, ${event.venue ?: "the venue"} is wheelchair accessible."
                    false -> "Unfortunately, this event has limited wheelchair accessibility."
                    else -> "Please check directly with the venue regarding accessibility."
                }
                "$accessText $genericAnswer"
            }

            "age", "child", "children", "kids", "restriction", "baby" -> {
                val restriction = event.ageRestriction
                val ageText = if (!restriction.isNullOrBlank()) {
                    "The age policy for this event is: $restriction."
                } else {
                    "There is no specific age restriction listed for this event."
                }
                "$ageText $genericAnswer"
            }

            "genre", "style", "type" -> {
                val genreText = event.genre ?: "General"
                "This is a $genreText event. $genericAnswer"
            }

            else -> genericAnswer
        }
    }

    // only match upcoming events (prevents selecting expired events)
    private fun checkForEventMatch(input: String): Event? {
        val inputNorm = TextFuzzy.normalize(input)
        val inputTokens = TextFuzzy.tokens(inputNorm)

        fun scoreTitle(title: String?): Double {
            val t = title ?: return 0.0
            val titleTokens = TextFuzzy.tokens(t)
            if (titleTokens.isEmpty() || inputTokens.isEmpty()) return 0.0

            // token overlap score (fuzzy)
            var hits = 0
            for (tt in titleTokens) {
                if (inputTokens.any { itTok -> TextFuzzy.similarity(itTok, tt) >= 0.78 }) {
                    hits++
                }
            }
            val overlap = hits.toDouble() / titleTokens.size.toDouble()

            // also check full-string similarity
            val full = TextFuzzy.similarity(inputNorm, t)

            return maxOf(overlap, full)
        }

        val scored = upcomingEvents().map { e ->
            val s1 = scoreTitle(e.artist)
            val s2 = scoreTitle(e.name)
            e to maxOf(s1, s2)
        }

        val best = scored.maxByOrNull { it.second }
        return if (best != null && best.second >= 0.60) best.first else null
    }


    private fun calculateMatchScore(target: String, userInput: String, userSquashed: String): Double {
        val targetLower = target.lowercase()
        val targetSquashed = targetLower.replace(Regex("[^a-z0-9]"), "")

        if (targetSquashed.isEmpty()) return 0.0
        if (userInput == targetLower) return 1.0
        if (userInput.contains(targetLower)) return 1.0
        if (userSquashed.contains(targetSquashed)) return 0.9

        val len = targetSquashed.length
        if (userSquashed.length >= len) {
            var minDistance = Int.MAX_VALUE
            for (i in 0..userSquashed.length - len) {
                val chunk = userSquashed.substring(i, i + len)
                val distance = LevenshteinUtils.calculate(chunk, targetSquashed)
                if (distance < minDistance) minDistance = distance
            }
            val errorRatio = minDistance.toDouble() / len.toDouble()
            if (errorRatio < 0.35) return 1.0 - errorRatio
        } else {
            if (isSubsequence(userSquashed, targetSquashed)) {
                if (userSquashed.length.toDouble() / targetSquashed.length.toDouble() > 0.5) return 0.85
            }
        }
        return 0.0
    }

    private fun isSubsequence(s1: String, s2: String): Boolean {
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            if (s1[i] == s2[j]) i++
            j++
        }
        return i == s1.length
    }

    private suspend fun updateSuggestedPrompts(targetLang: String) {
        val raw = faqList.mapNotNull { it.question }.shuffled().take(3)
        val finalPrompts = if (targetLang == "en") raw else raw.map {
            translator.translate(it, targetLang, "en")
        }
        ChatRepository.setPrompts(finalPrompts)
    }

    private fun addMessage(msg: ChatMessage) = ChatRepository.addMessage(msg)
    private fun removeLoading() = ChatRepository.removeLoading()
}


fun launchSpeechRecognizer(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    languageTag: String? = null
) {
    val lang = languageTag ?: Locale.getDefault().toLanguageTag()

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        // Force recognition language
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)

        // Improve chance the recognizer respects your language
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)

        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
    }

    try {
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
    }
}





// 4. UI SCREEN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(navController: NavController, viewModel: ChatbotViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val prompts by viewModel.suggestedPrompts.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val context = LocalContext.current

    val speechLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    viewModel.processUserMessage(spokenText)
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                launchSpeechRecognizer(context, speechLauncher)
            } else {
                Toast.makeText(
                    context,
                    "Microphone permission required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TicketLah! Assistant Bot", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(messages) { msg ->
                    when (msg) {
                        is ChatMessage.User -> UserMessageBubble(msg.text)
                        is ChatMessage.Bot -> BotMessageBubble(
                            message = msg,
                            onTranslateClick = {
                                viewModel.translateBotMessage(
                                    msg.id,
                                    msg.text,
                                    msg.language
                                )
                            }
                        )

                        is ChatMessage.BotEvent -> EventCardBubble(msg.event)
                        is ChatMessage.BotCarousel -> EventsCarousel(msg.events, navController)
                        is ChatMessage.Loading -> LoadingAnimationBubble()
                    }
                }
            }

            if (prompts.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts) { p ->
                        SuggestionChip(
                            onClick = { viewModel.processUserMessage(p) },
                            label = { Text(p) })
                    }
                }
            }

            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        launchSpeechRecognizer(context, speechLauncher)
                    }
                }) {
                    Icon(Icons.Default.Mic, "Mic")
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Ask something...") },
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.processUserMessage(inputText); inputText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}


// UI Components
@Composable
fun EventsCarousel(events: List<Event>, navController: NavController) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(events) { event ->
            CompactEventCard(event)
        }
    }
}

@Composable
fun CompactEventCard(event: Event) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // Image Loader for the Carousel Cards
    LaunchedEffect(event.eventImage) {
        val rawUrl = event.eventImage?.trim().orEmpty()
        if (rawUrl.startsWith("gs://")) {
            try {
                imageUrl = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(rawUrl)
                    .downloadUrl.await().toString()
            } catch (e: Exception) {
                imageUrl = null
            }
        } else {
            imageUrl = rawUrl
        }
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable {
                val intent = Intent(context, EventDetailsActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                Modifier
                    .height(120.dp)
                    .fillMaxWidth()
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Gray))
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    text = event.artist ?: "Event",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.date?.toDate().toString().take(10) ?: "Upcoming",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "View >",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun EventCardBubble(event: Event) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(event.eventImage) {
        val rawUrl = event.eventImage?.trim().orEmpty()
        if (rawUrl.startsWith("gs://")) {
            try {
                imageUrl =
                    FirebaseStorage.getInstance()
                        .getReferenceFromUrl(rawUrl).downloadUrl.await()
                        .toString()
            } catch (e: Exception) {
                imageUrl = null
            }
        } else {
            imageUrl = rawUrl
        }
    }

    Card(
        Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true)
                        .build(),
                    contentDescription = "Event Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(bottom = 8.dp)
                )
            }
            Text(
                "EVENT FOUND",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                event.artist ?: "Artist",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(event.name ?: "Event")
            Spacer(Modifier.height(8.dp))
            Text("📍 " + (event.venue ?: "Venue"))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(context, EventDetailsActivity::class.java)
                    intent.putExtra("EVENT_ID", event.id)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Details")
            }
        }
    }
}

@Composable
fun UserMessageBubble(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(4.dp), contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(text, Modifier.padding(12.dp), color = Color.White)
        }
    }
}

@Composable
fun BotMessageBubble(
    message: ChatMessage.Bot,
    onTranslateClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart

    ) {
        Surface(
            shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = Color.Black
                )

                if (message.translatedText != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${message.translatedText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                if (message.showTranslateButton && message.translatedText == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "See original (English)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onTranslateClick() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingAnimationBubble() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            sg.edu.np.mad.mad25_t02_team1.R.raw.loading_dots
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(50.dp)
                    )
                } else {
                    Text("Thinking...")
                }
            }
        }
    }
}

