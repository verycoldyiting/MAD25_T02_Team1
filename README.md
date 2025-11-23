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
## Login/Register Page [Daphne Cheng & Valerie Kho]
1. Login/Register Fragment:
* Provides a fragment that allows users to choose to login or register.
* Offers a seamless transition between login and registration forms.

2. Firebase Integration:
* Account information is checked through Firebase.
* Newly created accounts are updated in Firebase for secure management using Firebase Authentication.
* Ensures user data is managed safely and efficiently, leveraging Firebase's robust authentication mechanisms.

3. Forget Password Option:
* Available in the login fragment.
* Prompts a dialogue box asking users for their email.
* Users can press the cross to exit the dialogue.

## Profile Page [Daphne Cheng]
1. View Account Information:

* Displays the user's account information.
* Data is dynamically populated from Firebase, ensuring it is always up-to-date.
2. Edit Profile:

* Users can edit their profile information by pressing the pencil icon next to the fields.
* Provides an intuitive and user-friendly way to make changes to personal details.
3. Password Visibility Toggle:

* Users can check the checkbox to show their password.
* Allows users to verify their input for accuracy, enhancing security and reducing login erros.
4. Logout Button:

* Allows users to log out of their account.
* Provides a secure way to end the session.
5. Save Button:

* Allows users to save updated account information.
* Ensures changes are stored and reflected in their profile and stored in Firebase.

## Home Page [Valerie Kho]
1. Consistent Header:

* The header remains visible and consistent across all pages.
2. Side Scroll View for "Upcoming Events":

* Displays a horizontal scrolling list of upcoming events.
* The top 3 events are displayed based on their upcoming dates.
* Event information is dynamically extracted from Firebase and populated on the interface.
3. "Recommended for You" Section:

* Shows a recycler view of events recommended for the user.
* Recommendations allows user to view the available list of events that are happening in singapore.
4. Dynamic Event Picture:

* The featured event picture displayed below the header changes randomly.
* Each time the user refreshes the page, a new image is shown.
5. User-Friendly Footer:

* Contains navigation icons for easier movement through the app.
* Designed to be intuitive and enhance the user experience.

## Footer Navigation [Valerie Kho]
* The footer navigation includes icons that links to the Homepage, Explore Events, Booking History, and Profile Page for easy access and seamless user experience.

## Explore Event Page [Natalie Wong]
1. Event Search:

* Allows users to search events by title or artist.
* Provides a user-friendly interface for quick event discovery.
2. Filter Options:
* Users can filter events by price, event type, or date.
* Price and event type options are dynamic and depend on the database.
* Users can clear the selected filters by using the "Clear Filters" button.
* Ensures users can find events that meet their preferences.
* Made scrollable to ensure responsiveness in landscape orientation.
3. RecyclerView of Events:

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


# Planned task(s) and feature(s) allocation of each member in the team for Stage 2.
# Stage 2 Implementation:
Natalie -

Valerie -

Yuhong -

Daphne -
