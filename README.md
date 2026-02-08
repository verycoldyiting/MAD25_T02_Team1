# MAD25_T02_Team1
# Disclaimer
This is a student assignment project for the Kotlin App Development module at Ngee Ann Polytechnic. Developed for educational purposes.

# Introduction
TicketLah! is a dedicated platform for accessing event tickets in Singapore across various categories, designed to simplify the ticketing process. With its user-friendly interface and comprehensive event database, TicketLah! aims to enhance the experience of exploring and attending events in Singapore.

# Objective
To allow people in Singapore to be able to purchase event tickets easily.

# App Category
Entertainment

# Declaration of LLM used to assist in development 
ChatGPT,
Gemini,
Google AI Studio



# Student ID, Team Members and Git Username
1. (S10257861A) Natalie Wong- natalie-s10257861a

2. (S10262676E) Valerie Kho- verycoldyiting 

3. (S10258441F) Yuhong Gan- yuhong000 

4. (S10262840D) Daphne Cheng- daphne-17 


# TicketLah! App Stage 1 Features:
## Login/Register Page [Daphne Cheng]
1. Login/Register Fragment:
* Provides a page that allows users to choose to login or register.
* Offers a seamless transition between login and registration forms.

2. Password Visibility Toggle:

* Users can check the checkbox to show their password.
* Allows users to verify their input for accuracy, enhancing security and reducing login errors.

3. Firebase Integration:
* Account information is checked through Firebase.
* Newly created accounts are updated in Firebase for secure management using Firebase Authentication.
* Ensures user data is managed safely and efficiently, leveraging Firebase's robust authentication mechanisms.


## Profile Page [Daphne Cheng]
1. View Account Information:

* Displays the user's account information.
* Data is dynamically populated from Firebase, ensuring it is always up-to-date.
2. Edit Profile:

* Users can edit their profile information by pressing the edit profile button.
* Provides an intuitive and user-friendly way to make changes to personal details.

4. Logout Button:

* Allows users to log out of their account.
* Provides a secure way to end the session.
5. Save Changes Button:

* Allows users to save updated account information.
* Ensures changes are stored and reflected in their profile and stored in Firebase.

## Home Page [Valerie Kho]
1. Consistent Header:

* The header remains visible and consistent across all pages.
2. Side Scroll View for "Upcoming Events":

* Displays a horizontal scrolling list of upcoming events.
* The top 3 events are displayed based on their upcoming dates.
* Event information is dynamically extracted from Firebase and populated on the interface.
3. "Upcoming Events" Section:

* Shows a recycler view of events recommended for the user.
* Recommendations allows user to view the available list of events that are happening in singapore.

4. User-Friendly Footer:

* Contains navigation icons for easier movement through the app.
* Designed to be intuitive and enhance the user experience.

## Footer Navigation and Header Implementation [Valerie Kho/Daphne Cheng]
* The footer navigation includes icons that links to the Homepage, Explore Events, Booking History, and Profile Page for easy access and seamless user experience.

## Explore Event Page [Natalie Wong]
1. Event Search:

* Allows users to search events by title or artist.
* Provides a user-friendly interface for quick event discovery.
  
2. Filter Options:
  
* Users can filter events by genre.
* Price and event type options are dynamic and depend on the database.
* Users can clear the selected filters by using the "Clear Filters" button.
* Ensures users can find events that meet their preferences.
* Made scrollable to ensure responsiveness in landscape orientation.
  
3. View of Events:

* Displays a list of different events at the bottom of the page.
* Events are dynamically populated from Firebase.

## Event Details Page [Natalie Wong]
1. Dynamic Event Information:

* Event details are populated from data in Firebase.
    * Includes:
        * **Artist:** Name of the performing artist or group.
        * **Genre:** Type of music or performance.
        * **Date and Time:** Scheduled date and time of the event.
        * **Venue:** Location of the event.
        * **Description:** Detailed information about the event.
        * **Ticket Price:** Cost of attending the event.
        * **General Sales:** Date and time when tickets go on sale.
          
2. View Seat Map:

* A dialogue allows users to see the concert location map.
* Includes detailed seating arrangements and ticket pricing.
  
3. Buy Tickets Button:

* Directs users to the ticket purchasing page.
* Ensures a seamless process for ticket acquisition.

## Buy Tickets Page [Yu Hong]
1. Image View of Map:

* Displays a visual map of the venue with seating arrangements.
* Allows users to see the layout and select their preferred sections.
  
2. Dropdown for Different Categories and Seat Numbers:

* Populated from data stored in Firebase.
* Users can select a seat category first, which filters available seat numbers.
  
3. Seat Selection Logic:

* Users can only choose a seat number after selecting a seat category.
* Once both seat category and number are chosen, the bottom information will show total price.
  
4. Ticket Quantity:

* Allows users to enter the number of tickets they wish to purchase.
5. Toast Messages:

* Alerts users of their choices.
* Ensures users enter a quantity greater than 0.
* If the user changes the seat category, the previously selected seat number will disappear, and the book button will be hidden to enforce the selection of a new seat number.
  
6. Book Button:

* Lights up only when all information in the table is filled.
* Ensures all necessary selections are made before proceeding.

## Payment Details Page [Valerie Kho]
1. Card and Billing Information:

* Allows users to enter necessary card details and billing address.
* Dropdown to allow users to choose their preferred card type.
  
2. View Booking Button:

* Opens a dialogue that shows booking information made by the user from the previous page.
  
3. Total Price TextView:

* Displays the total price the user has to pay.
  
4. Buy Now Button:

* Allows the user to complete the purchase.
  
5. Toast Messages:

* Alerts users if they enter any field incorrectly.
  
6. Cancel Button:

* Leads users back to the buy tickets page.

## Booking History Page [Yu Hong]
1. View Booking History:

* Shows the booking history of the user, populated from Firebase.
* Provides a comprehensive view of all bookings made by the user.
  
2. Booking Information Cards:

* Each card displays detailed information about a booking, including:
    * **Seat Category**
    * **Seat Number**
    * **Total Price**
    * **Quantity**
    * **Payment Method**
* Ensures users have easy access to their booking details.
  
3. RecyclerView of Booking details

* Displays a list of booking details that user had booked
* Booking details are dynamically GET from the firebase for the specific user

## Responsiveness implementation [Team]
* Ensures the app is responsive and adapts well across various screen sizes and orientations.
## Logo Implementation [Valerie]

# Stage 2 Implemented features/enhancements:
## Enhanced Stage 1 Feature: Explore Page [Natalie Wong]
Enhanced Stage 1 Feature: Explore Pages
The Explore Page was enhanced to fix the incomplete filter logic highlighted during the stage 1 implementation and the broken try-catch logic.
Firstly, I fixed the incomplete filter logic. My original filter only had a basic dropdown that changed the icon colour with no other visual feedback. However, for stage 2, I have added the following:
* A filterchip that shows the active genre with a close button
* A result count indicator ("X events found")
* An "All Genres" option with a divider and visual highlighting of the selected genre in the dropdown
* An empty state view ("No events found") with a "Clear all filters" button when filters return no results

Secondly, I fixed broken try catch logic. My original code had availableGenres extraction (mapping, filtering, sorting) inside the try-catch block alongside the Firebase call. After carefully understanding and evaluating the code, I realised that my try catch logic was logically wrong, given Firebase successfully fetches the events, extracting genres from that already-fetched list can never fail in a meaningful way. You fixed this by:
* Keeping only Firebase network operations inside the try block
* Moving the genre extraction to the finally block, where it runs regardless of success/failure
* Adding a specific catch for FirebaseNetworkException (network errors) vs a generic Exception catch.

## Enhanced Stage 1 Feature: Change Of Password from the Edit Profile Page  [Natalie Wong]
The original Edit Profile page only had basic text fields for name, phone, and email, but password change functionality, so I added three password fields with visibility toggles, which included a Firebase re-authentication flow. I added currentPassword, newPassword, and confirmPassword fields, each with their own show "" Password boolean state. Each field has an eye icon (Visibility/VisibilityOff) that toggles between PasswordVisualTransformation and VisualTransformation.None, letting users peek at what they've typed. 
For security, Firebase requires the user to re-authenticate before sensitive operations like password changes. Your updateProfile function checks if currentPassword and newPassword are provided. If so, it creates an EmailAuthProvider credential with the user's email and current password, calls user.reauthenticate(credential), and only upon success calls user.updatePassword(newPassword). If re-authentication fails, it returns "Current password is incorrect."

## Enhanced Stage 1 Feature: Upload Of Profile Picture from the Edit Profile Page [Natalie Wong]
The original Edit Profile page only had basic text fields for name, phone, and email, but no ability to select or upload a profile picture. I added the full image selection and upload pipeline, which works in two stages:
Part 1: Selecting the photo from the device:
I implemented Android's photo picker to allow users to select a photo from their device's gallery (Google Photos). To ensure backward compatibility across different Android versions, I set up three separate launchers. For Android 13+ (Tiramisu), I used the modern PickVisualMedia API, which doesn't require runtime permissions. For older Android versions (6.0+), I used the legacy GetContent contract with "image/*" as a fallback, along with a RequestPermission launcher to handle READ_EXTERNAL_STORAGE at runtime. The selectPhoto() function acts as a router that checks Build.VERSION.SDK_INT and directs to the appropriate picker or permission request flow. When the user picks a photo, Android returns a local Uri(a temporary reference to that image file on the device.) I then store this in a selectedImageUri state and use it to show a local preview immediately in the circular profile picture area, without waiting for any upload. The display logic uses selectedImageUri?.toString() ?: profileImageUrl, meaning if a new photo has just been picked it shows the local preview, and if not it falls back to the existing Firestore URL.

Part 2: Uploading to Firebase Storage on save. The local Uri from Stage 1 is temporary and only accessible on the user's specific device if they logged in on another phone or cleared app data, the photo would be gone. So when "Save Changes" is pressed, the uploadImageAndUpdate() function checks if a selectedImageUri exists. If it does, it creates a reference in Firebase Storage under profile_images/{uid}/{randomUUID}.jpg, uploads the file with putFile(), retrieves the permanent download URL on success, and passes that URL to updateFirestore() which saves it to the profileImageUrl field in the Account document. This way, the profile picture persists across any device or session. If no new image was selected, it skips the upload entirely and proceeds with null.


## Enhanced Stage 1 Feature: Forget Password [Natalie Wong]
The original Login page had no password recovery option so if a user forgot their password, they had no way to regain access to their account. However, to ensure proper password recovery, I added a complete Forget Password flow using Firebase Authentication's built-in password reset mechanism.
I added an underlined "Forgot Password?" TextButton aligned to the right, positioned just below the password field. When tapped, it launches ForgotPasswordActivity via an Intent. I also restructured the bottom of the login page so that the Register option follows the same text-link style (a TextButton with underline reading "Don't have an account? Register") rather than being a full-sized yellow button, keeping the UI consistent and less cluttered.
I then created a separate ForgotPasswordActivity with its own Scaffold and a top app bar titled "Reset Password" styled with the app's yellow theme colour. The activity first checks if it was opened from an email link (via intent?.data?.toString() and FirebaseAuth.isSignInWithEmailLink()), routing to ResetPasswordFromLinkScreen if so, or the normal ForgotPasswordScreen otherwise. The main screen displays an email icon, a "Forgot Password?" heading, and an explanatory subtitle. Below that is an OutlinedTextField for the user's email address. I added two layers of validation before submission to check that the field isn't blank, and verifying the format using android.util.Patterns.EMAIL_ADDRESS.matcher(). The "Send Reset Link" button shows a CircularProgressIndicator while the request is in progress (controlled by an isLoading state that also disables the text field and button to prevent duplicate submissions). On press, it calls FirebaseAuth.getInstance().sendPasswordResetEmail(email). If the call fails, it displays the error message via a Toast.
Once the reset email is successfully sent, the UI switches from the email input form to a confirmation view (controlled by an emailSent boolean state). This confirmation screen displays a green CheckCircle success icon, a "Check Your Email" heading, and shows the email address the link was sent to. Below that is a Card styled with a light yellow background (Color(0xFFFFF9E6)) containing a "Next Steps" section that walks the user through the process: check inbox and spam folder, click the reset link, set a new password on the Firebase-hosted page, then return to the app and log in. At the bottom, there is a "Back to Login" button that navigates to LoginScreen with FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK to clear the back stack, and a "Resend Email" TextButton that resets both the emailSent flag and the email field so the user can try again.
This uses Firebase's secure, industry-standard password reset. Firebase sends an email containing a one-time reset link. The user clicks the link (which opens in their browser), enters their new password on Firebase's hosted page, and Firebase updates the password in Firebase Authentication. The user then returns to the app and logs in with their new password. This approach is secure because the app never handles raw password reset tokens where Firebase manages the entire verification and update process server-side.

## Add Event To Calendar - Stage 2 [Natalie Wong]
The original Booking History page had no way for users to save an event to their phone's calendar. I added full Android calendar integration so users can add their booked events to their device calendar with a single tap.
I added a clickable bordered box labelled "Add To Calendar" positioned on the same row as the event date, aligned to the right. It sits next to the date icon and formatted date text using a Row with Arrangement.SpaceBetween. The button has a subtle underline animationcwhen pressed. isPressed is set to true which applies TextDecoration.Underline, then a coroutine resets it after 300ms using delay() to give visual feedback. I used indication = null with a custom MutableInteractionSource to suppress the default Material ripple.
Since writing to the Android calendar requires WRITE_CALENDAR and READ_CALENDAR permissions, I implemented a full permission request flow. I set up a rememberLauncherForActivityResult with ActivityResultContracts.RequestMultiplePermissions() that requests both permissions simultaneously. A pendingEvent state variable stores the event the user wants to add if permissions aren't yet granted, the event is held in pendingEvent while the permission dialog shows, and once granted, the calendar insertion proceeds automatically using the stored event. If the user denies permission, a Toast informs them that calendar access is required. If permissions are already granted (checked via ContextCompat.checkSelfPermission), it skips the request and adds the event directly.
addEventToCalendarDirectly inserts it into the Android calendar. This private function handles the actual calendar insertion using Android's CalendarContract content provider. It first calls getCalendarId() to find the first writable calendar on the device by querying CalendarContract.Calendars.CONTENT_URI and filtering for calendars with at least CAL_ACCESS_CONTRIBUTOR level access. If no writable calendar exists (e.g. no Google account signed in), it shows a Toast and returns. It extracts the event date in milliseconds from the Firebase Timestamp, applies an 8-hour offset correction for the Singapore timezone, and sets a default event duration of 3 hours. I build a ContentValues object populated with the event title, a description containing the artist, venue, and event description, the event location, start/end times, calendar ID, and timezone set to "UTC". The event is inserted via context.contentResolver.insert(). On success, it also creates a 30-minute reminder by inserting into CalendarContract.Reminders.CONTENT_URI with METHOD_ALERT, and shows a " Event added to calendar!" Toast. The entire operation is wrapped in a try-catch that displays the error message if anything fails.

## QR Code Implementation - Stage 2 [Natalie Wong]
The original Booking History page had no QR code functionality — tickets were displayed as static cards with no way to verify them at an event venue. I added a full QR code generation and verification system spanning three components: an in-app QR code screen, a separate hosted verification web page, and the integration between them.
I created a dedicated QRCodeActivity and QRCodeScreen composable, accessible from each booking card via a "View QR Code" button. The booking card passes all ticket details (booking ID, event name, artist, venue, date, category, section, quantity, price per ticket) to the QR screen via Intent extras. The screen displays the event name, a ticket information card with all details laid out using QRTicketInfoRow composables, and the QR code image below.
I also implemented a LaunchedEffect that runs an infinite loop generating a new QR code every 60 seconds for security. Each cycle, it creates a timestamp-based token by concatenating the booking ID, user email, and current System.currentTimeMillis(), then hashing it with SHA-256 via MessageDigest and truncating to 32 characters using generateSecureToken(). This token is appended to a verification URL along with all the ticket parameters (event name, artist, venue, date, category, section, quantity, price, email, booking ID). The URL is then encoded into a QR code bitmap using the ZXing library's QRCodeWriter, rendering a 512×512 pixel black-and-white Bitmap with BarcodeFormat.QR_CODE. A countdown display shows "Refreshes in X seconds" and counts down from 60 to 1 before the QR regenerates.
On the booking card itself, I added logic that compares the event date against System.currentTimeMillis(). If the event has already passed (currentTime > eventTime), the "View QR Code" button is replaced with a grey non-clickable Card displaying "QR Code Expired" with a greyed-out icon, preventing users from generating QR codes for past events.
To show that a user's QR code is valid upon entry, prevent ticket reselling and impersonation, I created a standalone HTML/CSS/JavaScript verification page and hosted it on a separate GitHub Pages repository (https://natalie-s10257861.github.io/MAD_Assignment2_TicketLahVerification/). When someone scans the QR code, it opens this web page in their browser. The JavaScript init() function reads all ticket parameters from the URL query string using URLSearchParams, then populates the page with the event name, artist, venue, date, category, section, quantity, price per ticket, purchaser email, and booking ID. It calculates the total price (price × quantity) and formats everything in Singapore dollars. The page displays a green "VERIFIED" banner with a checkmark icon, all ticket details in a structured layout with labelled rows, a highlighted total amount in orange, a security badge reading "Secured by TicketLah!", and a verification timestamp. The page is fully responsive with mobile-specific CSS media queries that stack the layout vertically on screens under 480px.
Lastly, I also created a native Compose verification screen that handles deep link openings if the app intercepts the verification URL, it displays the same ticket information natively within the app using a Scaffold with a gradient background, green verified status card, event details with VerificationInfoRow composables, and a security footer. It reads the ticket data from activity.intent?.data URI query parameters.

## Chatbot with Translation and speech to text capabilities [Valerie Kho]
1. Multilingual Support - Google Cloud Translation API
* Automatically detects the language of the user input and responds accordingly.
* Provides translations for both questions and answers, ensuring seamless communication in various languages.
* Ensures accurate and relevant responses by processing the messages in the correct language.
* Provides an optional “See English version” toggle for translated bot replies.

2. Speech-to-Text Integration
* Allows users to interact with the chatbot using voice commands.
* Voice input via Android Speech Recognizer with microphone permission handling and user-friendly fallback messages.
* Converts spoken language into text for processing, enhancing accessibility and ease of use.

3. Event Information Retrieval
* Provides detailed information about various events, including start times, locations, and artist details.
* Can answer specific questions related to events such as ticket purchase, refund policies, and accessibility options.

4. Fuzzy Matching for Typos
* Uses token normalization + Levenshtein Distance algorithm to find the closest matching keyword from the user input.
* Ensures accurate responses even with minor typos or variations in the user's questions.

5. Suggested Prompts
* Displays a list of suggested prompts to guide users on what they can ask.
* Prompts are translated into the detected language for better user understanding.

6. FAQ Integration + Context-Aware Follow-up Questions
* Fetches frequently asked questions (FAQs) from the Firestore database.
* Provides quick and accurate answers to common queries related to events and services.
* Stores the selected event context to answer follow-up questions (refund policy, accessibility, venue, timing) without repeating the event name.

7. User-Friendly Interface
* Features an intuitive and easy-to-navigate user interface.
* Provides a smooth and engaging user experience with clear and concise responses.
* Smart auto-scroll with “New messages” button for better chat UX.

8. Robust Error Handling
* Graceful fallbacks for translation failures, missing mic permissions, and unavailable speech recognition.

## Stripe Third-Party Payment Integration [Valerie Kho]
The Payment Details Page has been enhanced to support a real third-party payment workflow using Stripe, improving checkout security, reliability, and overall user experience compared to Stage 1.
1. Secure Stripe Checkout (PaymentSheet)
* Integrated Stripe PaymentSheet for a native, secure card payment UX.
* Payment details are handled by Stripe; the app does not store card data and only stores booking metadata in Firestore.
* Displays clear payment states (processing indicator, cancel, error feedback via Snackbar).

2. Backend PaymentIntent Creation via Firebase Cloud Functions
* Uses Firebase Functions (asia-southeast1) to create a PaymentIntent and return a clientSecret.
* Refreshes Firebase Auth token before calling the function to ensure authenticated requests.

## Enhanced Stage 1 Feature: Booking History with 3-Tab Ticket Management [Valerie Kho]
The Booking History feature was enhanced to improve usability and ticket management by introducing three ticket views (tabs) with smart filtering
1. 3-Tab Ticket Filtering (Upcoming / Recently Purchased / Past)
* Implemented a 3-tab filter system using TicketViewMode:
    * **Upcoming: shows tickets for events that are happening later (or Date TBA), sorted by soonest event first.**
    * **Recently Purchased: shows upcoming tickets sorted by latest purchase time first.**
    * **Past: shows tickets for events that have already ended, sorted by most recent past event first.**
* Each tab has a meaningful empty state message (e.g., “No upcoming tickets.”).

## Enhanced Stage 1 Feature: Event Details Page with Dynamic Event Information [Valerie Kho]
The Event Details page was enhanced to provide users with richer event context and safer interactions, by dynamically loading event data from Firestore and displaying structured event information.
1. Structured “Event Information” Section (More Details for Users)
* Adds a dedicated Event Information card that conditionally shows available fields such as:
    * **Artist**
    * **Genre**
    * **Age Restriction**
    * **Wheelchair Accessibility (Yes/No)**
    * **Event Type (Indoor/Outdoor)**
    * **Refund Policy**
* This improves transparency and reduces user confusion before purchase.


##  Google Maps and Places API [Yu Hong]
1. Interactive Google Maps: Embeds a fully functional map fragment that automatically locates and pins the event venue using Geocoding.
2. Explore Nearby: Integrated with Google Places API to help users find amenities within a 500m walking radius:
* Food & Dining: Displays top-rated restaurants and eateries nearby (Green Pins).
* Transport: Locates the nearest bus stops and transit points (Blue Pins).
3. Navigation Support: Includes a "Get Directions" button that seamlessly launches the external Google Maps app with the destination pre-filled.
4. Customizable Views: Users can toggle between Normal, Satellite, and Hybrid map layers.

## Enhanced Stage 1 Feature: Buy Ticket [Yu Hong]
1. Enhanced buy ticket UI

## Enhanced Stage 1 Feature: Explore Event [Yu Hong]
1. Search & Discovery Enhancements
* Smart Search Algorithm: Implemented a robust local filtering system for the Explore page that supports:
  * Initials Matching: Users can type "bp" for "Blackpink".
  * Fuzzy Search (Levenshtein): Integrated tolerance for typos (e.g., "jblakcpink") to ensure users find events even with spelling mistakes.

## Enhanced Stage 1 Feature: Biometric Login (Daphne)
I enhanced the login flow with biometric authentication to improve usability and security. Users will be able to return and access the app more quickly without having to enter email and password. If users still prefer the traditional way of logging in they are able to toggle enable and disable at the profile page.
1. Secure Biometric Authentication (Android BiometricPrompt)
* Integrated AndroidX BiometricPrompt to support fingerprint or device credentials in a native and secure manner.
* Biometric authentication is handled by the Android system; no biometric data is stored or accessed by the app.
* Ensures compliance with Android security best practices by delegating authentication to the OS.
2. Firebase Authentication Session Protection
* Biometric login is only enabled after a successful email and password login.
* The app verifies an existing Firebase Authentication session (currentUser) before allowing biometric authentication.
* Prevents unauthorized access by ensuring biometrics act as a secure gate to unlock an existing authenticated session, rather than replacing Firebase authentication.
3. User-Controlled Biometric Preference
* Introduced a toggle in the Profile page allowing users to enable or disable biometric login at any time.
* User preference is stored locally using SharedPreferences and respected on subsequent app launches.
4. Improved Login Experience
* Returning users with biometric login enabled are prompted for biometric authentication immediately upon app launch.
* First-time users or users who disable biometrics continue to use the standard email and password login flow.
* Clear error handling and fallback behavior are provided if biometric authentication is unavailable or fails
* Supports accessibility and user choice for devices that do not support biometrics or users who prefer traditional login methods.


## Stage 2 Feature: Speech Navigation (Daphne)
I have added on a speech navigation allowing users to navigate between key sections of the app using voice command. This feature improves accessibility and hands-free interaction.
1. Voice Command-Based Navigation Using Android Speech Recognizer
* Integrated Android’s built-in Speech Recognizer API to capture and convert user voice input into text.
* Users can trigger speech input via a dedicated microphone button and issue simple navigation commands such as “home”, “search”, “profile”, or “tickets”.
* Recognised speech is mapped to existing navigation routes, enabling seamless page transitions without manual interaction.
2. Accessibility and User Experience Enhancements
* Supports hands-free navigation, improving accessibility for users who may have difficulty with touch input.
* Provides clear feedback for unrecognised commands through on-screen messages, ensuring users understand how to interact with the feature.
* Designed to complement existing navigation controls without disrupting standard user flows.

