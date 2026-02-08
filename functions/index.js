// Firebase Admin SDK (used to initialize the Firebase app context)
const admin = require("firebase-admin");
admin.initializeApp();

// v2 callable functions + structured errors
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");

setGlobalOptions({ region: "asia-southeast1" });

function getStripe() {
  const key = process.env.STRIPE_SECRET_KEY;
  if (!key) throw new Error("Missing STRIPE_SECRET_KEY (secret env var).");
  return require("stripe")(key);
}

exports.createPaymentIntent = onCall(
  { secrets: ["STRIPE_SECRET_KEY"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Login required.");
    }
    // Initialize Stripe SDK using secret key
    const stripe = getStripe();

    const amount = request.data.amount;
    const currency = request.data.currency || "sgd";

    const intent = await stripe.paymentIntents.create({
      amount,
      currency,
      automatic_payment_methods: { enabled: true },
      metadata: {
        uid: request.auth.uid, // which user initiated payment
        eventId: request.data.eventId || "",  // which event the payment is for
        bookingId: request.data.bookingId || ""  // your internal booking reference
      }
    });

    // Return clientSecret ONLY (safe to send to client).
    return { clientSecret: intent.client_secret };
  }
);
