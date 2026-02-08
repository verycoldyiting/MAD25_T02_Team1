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
Natalie -
1. Profile picture upload, password update and validation, and forget password
* Uploading of profile picture and editing of email and password which once saved will be updated to FireStore and FireBase Authentication
  
2. Add Event to Calendar
* Displays title, artist and venue on the day of the concert/show synced on phone calendar.
  
3. QR Code implementation
* Generating Qr Code with timer for each ticket in the booking history for ticket scanning. (This code creates a dynamic QR code display for each concert ticket with an automatic refresh timer. When the ticket card loads, it generates a unique QR code containing the booking details (ticket ID, event name, seat section, and category) plus a timestamp. A background timer counts down from 60 seconds, displayed as "Next refresh in X seconds" below the QR code. When the countdown reaches zero, the system automatically generates a completely new QR code by updating the timestamp, making each QR code unique and different from the previous one. This 60-second cycle repeats infinitely as long as the ticket is displayed, providing a constantly changing security QR code that never shows the same pattern twice. The entire process happens automatically without any user interaction or stored image files - everything is generated in real-time using the ZXing library.)

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

## Enhanced Stage 1 Feature: Stripe Third-Party Payment Integration [Valerie Kho]
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

## Enhanced Stage 1 Feature: Event Details Page with Dynamic Event Information + Past Event Handling [Valerie Kho]
The Event Details page was enhanced to provide users with richer event context and safer interactions, by dynamically loading event data from Firestore, displaying structured event information, and preventing ticket purchase for ended events.
1. Past Event Detection + Ticket Sales Disabled
* Automatically detects whether an event is already over using event date vs current time.
* If event has ended:
    * **Shows a visible status label “Event Ended”**
    * **Replaces “Buy Tickets” with a disabled message: “Ticket sales are closed.”**
2. Structured “Event Information” Section (More Details for Users)
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

## Echanced Stage 1 Feature: Explore Event [Yu Hong]
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

