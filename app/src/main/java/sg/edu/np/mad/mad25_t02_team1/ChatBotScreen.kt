package sg.edu.np.mad.mad25_t02_team1

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.min
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale

// ==========================================
// 1. REPOSITORY (The Memory Vault)
// ==========================================
object ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage.Bot("Hi! I'm TicketLah! Assistant Bot. Ask me anything about events!"))
    )
    val messages = _messages.asStateFlow()

    private val _suggestedPrompts = MutableStateFlow<List<String>>(emptyList())
    val suggestedPrompts = _suggestedPrompts.asStateFlow()

    // Context Memory
    var selectedEvent: Event? = null

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
}

// ==========================================
// 2. UTILITIES (Math & Online Translation)
// ==========================================

object LevenshteinUtils {
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

    // ⚠️ PASTE YOUR COPIED API KEY HERE!
    private val API_KEY = "AIzaSyAVrdNkbQNSGypxY1uVDoS5LygCLkwTG4U"

    // Online Detection using Google Cloud API
    suspend fun detectLanguage(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://translation.googleapis.com/language/translate/v2/detect?key=$API_KEY"
                val jsonBody = JSONObject().put("q", text)

                val response = postJson(urlString, jsonBody.toString())
                val data = JSONObject(response).getJSONObject("data")
                val detections = data.getJSONArray("detections")
                if (detections.length() > 0) {
                    val firstItem = detections.getJSONArray(0).getJSONObject(0)
                    firstItem.getString("language")
                } else {
                    "en"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "en" // Default to English on error
            }
        }
    }

    // Online Translation using Google Cloud API
    suspend fun translate(text: String, targetLang: String, sourceLang: String? = null): String {
        if (targetLang == "en" && sourceLang == "en") return text

        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://translation.googleapis.com/language/translate/v2?key=$API_KEY"
                val jsonBody = JSONObject()
                jsonBody.put("q", text)
                jsonBody.put("target", targetLang)
                jsonBody.put("format", "text")
                if (sourceLang != null) {
                    jsonBody.put("source", sourceLang)
                }

                val response = postJson(urlString, jsonBody.toString())
                val data = JSONObject(response).getJSONObject("data")
                val translations = data.getJSONArray("translations")
                if (translations.length() > 0) {
                    translations.getJSONObject(0).getString("translatedText")
                } else {
                    text
                }
            } catch (e: Exception) {
                e.printStackTrace()
                text // Return original text on error
            }
        }
    }

    // Helper to send network request without extra libraries
    private fun postJson(urlString: String, jsonBody: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }

        return conn.inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
}

// ==========================================
// 3. VIEWMODEL (The Brain)
// ==========================================

sealed class ChatMessage(val id: String = UUID.randomUUID().toString()) {
    data class User(val text: String) : ChatMessage()
    data class Bot(val text: String) : ChatMessage()
    data class BotEvent(val event: Event) : ChatMessage()
    // --- NEW: A message type that holds a LIST of events ---
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

    init {
        viewModelScope.launch {
            try {
                val eventSnapshot = db.collection("Events").get().await()
                eventsList = eventSnapshot.documents.mapNotNull { it.toObject(Event::class.java) }
                val faqSnapshot = db.collection("FAQ").get().await()
                faqList = faqSnapshot.documents.mapNotNull { it.toObject(FAQ::class.java) }

                if (ChatRepository.suggestedPrompts.value.isEmpty()) {
                    updateSuggestedPrompts("en")
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun processUserMessage(inputText: String) {
        if (inputText.isBlank()) return
        addMessage(ChatMessage.User(inputText))
        addMessage(ChatMessage.Loading)

        viewModelScope.launch {
            // 1. Detect & Translate
            val detectedLang = translator.detectLanguage(inputText)
            val englishInput = if (detectedLang != "en") {
                translator.translate(inputText, "en", detectedLang)
            } else inputText

            val clean = englishInput.lowercase()

            // --- NEW: INTERCEPT "UPCOMING" REQUESTS HERE ---
            if (clean.contains("upcoming") || (clean.contains("show") && clean.contains("event")) || clean.contains("list event") || clean.contains("what event")) {
                removeLoading()
                if (eventsList.isEmpty()) {
                    addMessage(ChatMessage.Bot("I'm currently updating my database. Please check back later!"))
                } else {
                    addMessage(ChatMessage.Bot("Here are some events you might like:"))
                    // Show top 5 events in a fancy carousel
                    addMessage(ChatMessage.BotCarousel(eventsList.take(5)))
                }
                updateSuggestedPrompts(detectedLang)
                return@launch
            }
            // ------------------------------------------------

            // 2. Standard Logic
            val responseEnglish = generateBotResponse(englishInput)

            // 3. Translate Back
            val finalResponse = if (detectedLang != "en") {
                translator.translate(responseEnglish, detectedLang, "en")
            } else responseEnglish

            removeLoading()
            addMessage(ChatMessage.Bot(finalResponse))

            checkForEventMatch(englishInput)?.let { event ->
                // Don't duplicate if we just showed it in a carousel, but for search it's fine
                addMessage(ChatMessage.BotEvent(event))
            }
            updateSuggestedPrompts(detectedLang)
        }
    }

    private fun generateBotResponse(input: String): String {
        val clean = input.lowercase().trim().replace("&#39;", "'").replace("&quot;", "\"")
        val userWords = clean.split("\\s+".toRegex())

        val newEvent = checkForEventMatch(clean)
        var contextEvent = ChatRepository.selectedEvent
        var prefix = ""

        if (newEvent != null) {
            ChatRepository.selectedEvent = newEvent
            contextEvent = newEvent
            prefix = "I found ${newEvent.name}. "
        }

        if (clean.contains("other event") || clean.contains("another event") || clean.contains("general")) {
            ChatRepository.selectedEvent = null
            return "Sure, what else can I help you with?"
        }

        val answerParts = mutableListOf<String>()

        if (contextEvent != null) {
            if (clean.contains("wheelchair") || clean.contains("accessible") || clean.contains("disability")) {
                val isAccessible = contextEvent.isWheelchairAccessible == true
                if (isAccessible) answerParts.add("Yes! It is wheelchair accessible.")
                else answerParts.add("I'm sorry, it is not listed as wheelchair accessible.")
            }
            if (clean.contains("rain") || clean.contains("weather") || clean.contains("outdoor") || clean.contains("indoor")) {
                val isOutdoor = contextEvent.isOutdoor == true
                if (isOutdoor) answerParts.add("This is an outdoor event. Bring an umbrella!")
                else answerParts.add("Don't worry, it is an indoor event.")
            }
            if (clean.contains("kid") || clean.contains("child") || clean.contains("age") ||
                clean.contains("18") || clean.contains("baby") || clean.contains("girl") ||
                clean.contains("boy") || clean.contains("yo") || clean.contains("year old")) {
                val restriction = contextEvent.ageRestriction ?: "No specific age restriction listed."
                answerParts.add("The age policy is: $restriction.")
            }
            if (clean.contains("refund") || clean.contains("cancel") || clean.contains("change")) {
                answerParts.add(contextEvent.refundPolicy ?: "Check ticket page for refund info.")
            }
            if (clean.contains("venue") || clean.contains("where") || clean.contains("location")) {
                answerParts.add("It's happening at ${contextEvent.venue}.")
            }
            if (clean.contains("price") || clean.contains("cost") || clean.contains("how much")) {
                answerParts.add("Tickets start from $${contextEvent.price}.")
            }
            if (clean.contains("when") || clean.contains("time") || clean.contains("start")) {
                answerParts.add("Scheduled for: ${contextEvent.date?.toDate() ?: "TBA"}.")
            }

            // Food / Drink logic from FAQ db
            if (clean.contains("food") || clean.contains("drink") || clean.contains("water")) {
                // This will be caught by the FAQ check below, so we let it pass through
            }
        }

        val matchedFaq = faqList.sortedByDescending { it.priority }.find { faq ->
            faq.keywords.any { k ->
                val keywordLower = k.lowercase()
                userWords.contains(keywordLower) || LevenshteinUtils.isCloseMatch(keywordLower, clean)
            }
        }

        if (matchedFaq != null) {
            val alreadyAnsweredRefunds = answerParts.any { it.contains("refund", ignoreCase = true) }
            val faqIsAboutRefunds = matchedFaq.keywords.any { it.contains("refund") || it.contains("cancel") }
            if (!(alreadyAnsweredRefunds && faqIsAboutRefunds)) {
                answerParts.add(matchedFaq.answer ?: "")
            }
        }

        if (answerParts.isNotEmpty()) {
            return prefix + answerParts.joinToString(" ")
        }

        if (clean == "hi" || clean == "hello" || clean.startsWith("hi ") || clean.startsWith("hello ")) {
            return "Hello! How can I help?"
        }

        if (prefix.isNotEmpty()) {
            return "I found an event featuring ${contextEvent?.artist}! You can ask me about tickets, venue, age limits, or weather."
        }

        return "I'm not sure. Try asking about tickets, venues, or specific artists!"
    }

    private fun checkForEventMatch(input: String): Event? {
        val lower = input.lowercase()
        return eventsList.find { e ->
            e.artistNormalized.any { LevenshteinUtils.isCloseMatch(lower, it, 2) || lower.contains(it) } ||
                    (e.name?.lowercase()?.contains(lower) == true)
        }
    }

    private suspend fun updateSuggestedPrompts(targetLang: String) {
        val raw = faqList.mapNotNull { it.question }.shuffled().take(3)
        val finalPrompts = if (targetLang == "en") raw else raw.map { translator.translate(it, targetLang, "en") }
        ChatRepository.setPrompts(finalPrompts)
    }

    private fun addMessage(msg: ChatMessage) = ChatRepository.addMessage(msg)
    private fun removeLoading() = ChatRepository.removeLoading()
}

// ==========================================
// 4. UI SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(navController: NavController, viewModel: ChatbotViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val prompts by viewModel.suggestedPrompts.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text ->
                viewModel.processUserMessage(text)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Joe Yi (Bot)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(messages) { msg ->
                    when (msg) {
                        is ChatMessage.User -> UserMessageBubble(msg.text)
                        is ChatMessage.Bot -> BotMessageBubble(msg.text)
                        is ChatMessage.BotEvent -> EventCardBubble(msg.event)
                        is ChatMessage.BotCarousel -> EventsCarousel(msg.events, navController) // NEW UI
                        is ChatMessage.Loading -> LoadingAnimationBubble()
                    }
                }
            }
            if (prompts.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(prompts) { p -> SuggestionChip(onClick = { viewModel.processUserMessage(p) }, label = { Text(p) }) }
                }
            }
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    try { speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) } catch (_: Exception) {}
                }) { Icon(Icons.Default.Mic, "Mic") }
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    placeholder = { Text("Ask something...") }, shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = { if (inputText.isNotBlank()) { viewModel.processUserMessage(inputText); inputText = "" } }) { Icon(Icons.Default.Send, "Send") }
            }
        }
    }
}

// --- UI Components ---

@Composable
fun EventsCarousel(events: List<Event>, navController: NavController) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(events) { event ->
            CompactEventCard(event) // Defined below
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
            } catch (e: Exception) { imageUrl = null }
        } else { imageUrl = rawUrl }
    }

    Card(
        modifier = Modifier
            .width(160.dp) // Fixed width for nice carousel look
            .height(220.dp)
            .clickable {
                val intent = Intent(context, EventDetailsActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(Modifier.height(120.dp).fillMaxWidth()) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Gray)) // Placeholder
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
                Text("View >", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
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
                imageUrl = FirebaseStorage.getInstance().getReferenceFromUrl(rawUrl).downloadUrl.await().toString()
            } catch (e: Exception) { imageUrl = null }
        } else { imageUrl = rawUrl }
    }

    Card(Modifier.padding(8.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Event Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(bottom = 8.dp)
                )
            }
            Text("EVENT FOUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(event.artist ?: "Artist", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
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
            ) { Text("View Details") }
        }
    }
}

@Composable
fun UserMessageBubble(text: String) {
    Box(Modifier.fillMaxWidth().padding(4.dp), contentAlignment = Alignment.CenterEnd) {
        Surface(shape = RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp), color = MaterialTheme.colorScheme.primary) {
            Text(text, Modifier.padding(12.dp), color = Color.White)
        }
    }
}

@Composable
fun BotMessageBubble(text: String) {
    Box(Modifier.fillMaxWidth().padding(4.dp), contentAlignment = Alignment.CenterStart) {
        Surface(shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(text, Modifier.padding(12.dp), color = Color.Black)
        }
    }
}

@Composable
fun LoadingAnimationBubble() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(sg.edu.np.mad.mad25_t02_team1.R.raw.loading_dots))
    if (composition != null) {
        LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(50.dp))
    } else {
        Text("Thinking...", modifier = Modifier.padding(8.dp))
    }
}