

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

    fun isCloseMatch(input: String, target: String, threshold: Int = 2): Boolean {
        val normalizedInput = input.lowercase().trim()
        val normalizedTarget = target.lowercase().trim()

        if (normalizedInput == normalizedTarget) return true
        if (Math.abs(normalizedInput.length - normalizedTarget.length) > threshold) return false

        return calculate(normalizedInput, normalizedTarget) <= threshold
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
            "wheelchair", "accessible",
            "refund", "policy",
            "age", "kids", "child",
            "time", "late", "parking",
            "food", "drink", "beverage", "alcohol"
        )
        return keywords.any { input.contains(it) }
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
            val normalizedInput = clean
                .replace("-", "")
                .replace(" ", "")

            // GENRE SEARCH
            val allGenreTags = eventsList.flatMap {
                it.genreNormalized.map { t -> t.lowercase().replace("-", "").replace(" ", "") }
            }.distinct()

            // Find if any of the tags appear in user input
            val foundGenre = allGenreTags.find { tag -> normalizedInput.contains(tag) }

            if (foundGenre != null) {
                removeLoading()
                // Filter events matching the genre found
                val genreEvents = eventsList.filter { event ->
                    event.genreNormalized.any {
                        it.lowercase().replace("-", "").replace(" ", "") == foundGenre
                    }
                }

                ChatRepository.selectedEvent = null


                if (genreEvents.isNotEmpty()) {
                    addMessage(ChatMessage.Bot("Here are the ${foundGenre.replaceFirstChar { it.uppercase() }} events:"))
                    addMessage(ChatMessage.BotCarousel(genreEvents))
                } else {
                    addMessage(ChatMessage.Bot("I couldn't find any events for $foundGenre."))
                }
                return@launch
            }

            // Month/Upcoming Filtering

            val isMonthRequest =
                clean.contains("jan") || clean.contains("feb") || clean.contains("mar") || clean.contains(
                    "apr"
                ) ||
                        clean.contains("may") || clean.contains("jun") || clean.contains("jul") || clean.contains(
                    "aug"
                ) ||
                        clean.contains("sep") || clean.contains("oct") || clean.contains("nov") || clean.contains(
                    "dec"
                )

            // Updated check to catch "coming", "next", "show me events"
            val isGeneralRequest =
                clean.contains("upcoming") || clean.contains("coming") || clean.contains("next") || (clean.contains(
                    "show"
                ) && clean.contains("event"))

            if (isMonthRequest || isGeneralRequest) {
                removeLoading()
                if (eventsList.isEmpty()) {
                    addMessage(ChatMessage.Bot("I'm currently updating my database. Please check back later!"))
                    return@launch
                }

                val allEventsSorted = eventsList.sortedBy { it.date }

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

                    val filteredEvents = allEventsSorted.filter { event ->
                        event.date?.toDate()?.month == targetMonthIndex
                    }

                    if (filteredEvents.isNotEmpty()) {
                        addMessage(ChatMessage.Bot("Here are the events happening in $monthName:"))
                        addMessage(ChatMessage.BotCarousel(filteredEvents))
                    } else {
                        addMessage(ChatMessage.Bot("I couldn't find any events in $monthName."))
                    }
                    return@launch
                }

                // General "Upcoming" Logic 
                // Find a digit in the user's string
                val numberRegex = Regex("\\d+")
                val numberMatch = numberRegex.find(clean)

                val countToTake = numberMatch?.value?.toIntOrNull()?.coerceIn(1, 10) ?: 5

                addMessage(ChatMessage.Bot("Here are the next $countToTake upcoming events:"))
                addMessage(ChatMessage.BotCarousel(allEventsSorted.take(countToTake)))
                return@launch
            }


            val newEventFound =
                if (ChatRepository.selectedEvent != null && isEventSpecificQuestion(englishInput)) {
                    null
                } else {
                    checkForEventMatch(englishInput)
                }

            if (newEventFound != null) {
                ChatRepository.selectedEvent = newEventFound
            }


            // Smart Response (Context Injection)

            val responseEnglish = generateSmartResponse(englishInput, newEventFound)

            // Translate back
            val finalResponse = if (detectedLang != "en") {
                translator.translate(responseEnglish, detectedLang, "en")
            } else responseEnglish

            removeLoading()

            // Always add the bot text
            addMessage(
                ChatMessage.Bot(
                    text = finalResponse,
                    language = detectedLang,
                    showTranslateButton = detectedLang != "en"
                )
            )

            // Only show event card if this was an explicit event discovery
            if (newEventFound != null && !isEventSpecificQuestion(englishInput)) {
                addMessage(ChatMessage.BotEvent(newEventFound))
            }

            updateSuggestedPrompts(detectedLang)


        }
    }

    // Context-Aware Response Generator
    private fun generateSmartResponse(input: String, newEvent: Event?): String {
        val clean = input.lowercase()

        // Resolve context
        val targetEvent = newEvent ?: ChatRepository.selectedEvent

        // Event-aware answers (NO FAQ YET)

        if (targetEvent != null && isEventSpecificQuestion(clean)) {

            // Rain / Weather
            if (clean.contains("rain") || clean.contains("weather")) {
                return when (targetEvent.isOutdoor) {
                    true -> "This is an **outdoor event**, so weather conditions may affect the show. Please check official announcements closer to the event date."
                    false -> "This is an **indoor event**, so weather conditions will not affect the event."
                    else -> "Please check official announcements for weather-related updates for this event."
                }
            }

            // Wheelchair accessibility
            if (clean.contains("wheelchair") || clean.contains("accessible")) {
                return when (targetEvent.isWheelchairAccessible) {
                    true -> "Yes, **${targetEvent.venue ?: "this venue"}** is wheelchair accessible."
                    false -> "Unfortunately, **${targetEvent.venue ?: "this venue"}** is not wheelchair accessible based on current information."
                    else -> "Please check directly with **${targetEvent.venue ?: "the venue"}** regarding wheelchair accessibility."
                }
            }
        }


        // FAQ with event injection
        val matchedFaq = faqList
            .filter { faq ->
                faq.keywords.any { k ->
                    val kLower = k.lowercase()
                    Regex("\\b${Regex.escape(kLower)}\\b").containsMatchIn(clean) ||
                            LevenshteinUtils.isCloseMatch(kLower, clean, 1)
                }
            }
            .maxByOrNull { it.priority }

        if (matchedFaq != null) {
            val genericAnswer = matchedFaq.answer ?: "I'm not sure."

            if (targetEvent != null) {
                return customizeAnswerForEvent(
                    matchedFaq.category,
                    genericAnswer,
                    targetEvent
                )
            } else {
                return genericAnswer
            }
        }


        // Event discovery

        if (newEvent != null) {
            return "I found **${newEvent.name}**! You can ask me about tickets, venue, timing, or accessibility."
        }


        // Small talk

        if (Regex("^(hi|hello|hey)\\b", RegexOption.IGNORE_CASE).containsMatchIn(clean)) {
            return "Hello! I can help answer questions about upcoming concerts and events."
        }

        // Fallback

        return "I'm not sure. Try asking about a specific artist, genre (like 'Pop'), or event details."
    }


    private fun customizeAnswerForEvent(
        category: String?,
        genericAnswer: String,
        event: Event
    ): String {
        return when (category) {
            "tickets", "payment" -> {
                val prices = seatCategoryList.map { it.price }
                if (prices.isNotEmpty()) {
                    val minPrice = prices.minOrNull() ?: 0.0
                    val maxPrice = prices.maxOrNull() ?: 0.0
                    val minStr = String.format("%.2f", minPrice)
                    val maxStr = String.format("%.2f", maxPrice)
                    "For **${event.artist}**, ticket prices range from **$$minStr to $$maxStr**. $genericAnswer"
                } else {
                    "For ${event.artist}, check the app for specific pricing. $genericAnswer"
                }
            }

            "venue", "parking", "food" -> {
                "This event is at **${event.venue}**. $genericAnswer"
            }

            "timing", "late_entry" -> {
                "The event starts on **${formatDate(event.date)}**. $genericAnswer"
            }

            "refunds", "policy" -> {
                val policy = event.refundPolicy ?: "Standard rules apply"
                "For **${event.artist}**, the policy is: **$policy**. $genericAnswer"
            }

            "accessibility", "wheelchair", "disability", "handicap" -> {
                val accessText = when (event.isWheelchairAccessible) {
                    true -> "Yes, **${event.venue ?: "the venue"}** is wheelchair accessible."
                    false -> "Unfortunately, this event has limited wheelchair accessibility."
                    else -> "Please check directly with the venue regarding accessibility."
                }
                "$accessText $genericAnswer"
            }

            "age", "child", "children", "kids", "restriction", "baby" -> {
                // Check the String field from your Event.kt
                val restriction = event.ageRestriction
                val ageText = if (!restriction.isNullOrBlank()) {
                    "The age policy for this event is: **$restriction**."
                } else {
                    "There is no specific age restriction listed for this event."
                }
                "$ageText $genericAnswer"
            }

            "genre", "style", "type" -> {
                val genreText = event.genre ?: "General"
                "This is a **$genreText** event. $genericAnswer"
            }

            else -> genericAnswer
        }
    }


    private fun checkForEventMatch(input: String): Event? {
        val lowerInput = input.lowercase()
        val inputSquashed = lowerInput.replace(Regex("[^a-z0-9]"), "")

        val scoredEvents = eventsList.map { event ->
            // 1. Exact/Close Artist Match 
            val artistScore = calculateMatchScore(event.artist ?: "", lowerInput, inputSquashed)

            // 2. Exact/Close Event Name Match 
            val nameScore = calculateMatchScore(event.name ?: "", lowerInput, inputSquashed)


            val bestScore = maxOf(artistScore, nameScore)
            Pair(event, bestScore)
        }

        // Increased threshold slightly to prevent false positives
        val bestMatch = scoredEvents.maxByOrNull { it.second }
        return if (bestMatch != null && bestMatch.second > 0.7) bestMatch.first else null
    }


    private fun calculateMatchScore(
        target: String,
        userInput: String,
        userSquashed: String
    ): Double {
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
            // For abbreviations like "blk pnk"
            if (isSubsequence(userSquashed, targetSquashed)) {
                if (userSquashed.length.toDouble() / targetSquashed.length.toDouble() > 0.5) return 0.85
            }
        }
        return 0.0
    }

    // Helper for abbreviations
    private fun isSubsequence(s1: String, s2: String): Boolean {
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            if (s1[i] == s2[j]) {
                i++
            }
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
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
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

