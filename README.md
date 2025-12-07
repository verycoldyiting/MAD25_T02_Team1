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

# Planned task(s) and feature(s) allocation of each member in the team for Stage 2.
# Stage 2 Implementation:
Natalie -
1. Profile picture upload, password update and validation, and forget password
* Uploading of profile picture and editing of email and password which once saved will be updated to FireStore and FireBase Authentication
  
2. Add Event to Calendar
* Displays all details of an event, including title, artist, genre, venue, 
description, price, date and time and general sales period.

3. QR Code implementation
* Generating Qr Code that contains all the booking details and retrieving all booking details once scanned. 

Valerie -
1. Chatbot
* Allows message sending, voice input, and data retrieval with smart replies. It detects and translates languages, matches messages with artist info and events, responds to greetings, uses Levenshtein distance for keyword matching, provides smart replies, shows translated prompts, and cleans messages.

Yuhong -
1. Google Maps and Places API
* Displays Google Maps fragment and map pin of venue. Opens Google Maps application for navigation. Displays venue details and nearby places with Places API.

Daphne -
1. Biometric Login
* Account information is checked through Firebase Authentication. Includes forget password
  
2. Auto Fill payment page
* Enables users to instantly populate their card information using fingerprint authentication, removing the need to manually type payment details.
