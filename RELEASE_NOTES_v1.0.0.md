# Namma HomeStay v1.0.0 — first release 🏡

The first cut of the **Simplified Host Portal**: an Android app that lets rural farmers and homemakers run a homestay listing as easily as making a phone call — phone-OTP login, a card-based "digital shopfront", a 60-second "Today's Menu" post, and a one-tap "Call Guest" inquiry box. Designed for low digital literacy: big buttons, high-contrast earth tones, large type, and zero jargon.

## What's in this build

### 📱 Frictionless onboarding
- One-screen sign-in: phone number → 6-digit SMS code. No passwords, no email.
- SMS auto-retrieval / instant verification when the device supports it.
- First sign-in lands you on a **"Setup your Home"** progress bar (X of 5 steps done).

### 🏠 My Home — the digital shopfront
- `LazyColumn` of large cards: setup progress, a clear **LIVE / NOT LIVE YET** pill, home name & village, a photo strip, and the "promises to your guests" toggles.
- **Verification checklist**: three big toggles — clean bedding · working washroom · drinking water. The home goes **LIVE** only when all three are on, there's at least one photo, and the home has a name.
- **Photo upload**: "Tap to add photo" → pick from gallery/camera. Images are aggressively compressed for rural data; up to 6 photos.
- A floating **"Today's Menu"** button, always one tap away.

### 🍲 The "60-Second Menu" (the priority MVP feature)
- One photo + one dish name + one price + one big button — built to feel like posting a WhatsApp status.
- Publishing is a single Firestore `set()`, with a full-screen ✓ to confirm. Editing pre-fills the last menu.
- "Take today's menu down" removes it.

### 📞 Inquiry Box & direct connect
- "Incoming Interests" lists travellers (name + relative time + status).
- Tap a card → expand → big **green "Call Guest"** button opens the phone dialer with the number pre-filled (`ACTION_DIAL` — no call permission needed) and marks the inquiry as called.
- "Add a sample interest" button to seed test data until the traveller-facing app exists.

### 📖 Guide & Help
- Plain-language help cards for every feature, a "Call support" button, and sign out.

### Design & feedback
- **Earth-tone** Material 3 theme (leaf green / clay brown / harvest gold / cream); dynamic wallpaper colour is intentionally off.
- Large typography (`headlineMedium` titles, ≥18 sp body), ≥48 dp touch targets, 60 dp primary buttons.
- Every action shows a `Snackbar` or a success animation; errors surface the real reason (handy while wiring up Firebase).
- Slim, low-chrome top headers and a 4-tab bottom bar (Home · Menu · Interests · Help).

## Tech notes

- Kotlin · Jetpack Compose (Material 3) · MVVM + a thin repository layer · Navigation-Compose · Coroutines/Flow.
- **Firebase Auth (Phone) + Cloud Firestore only — no Cloud Storage.** The free Spark plan doesn't include Storage for new projects, so photos are stored as compressed JPEG `Blob`s inside Firestore documents (`ImageCompressor` keeps them well under the ~1 MB per-doc limit). `Homestay` therefore caps photos at 6.
- Firestore offline cache is enabled, so the host still sees their last menu / inquiries when the connection drops.
- minSdk 24 · targetSdk 36 · compileSdk 36 · AGP 9.2.1 · Kotlin 2.2.10 · Firebase BoM 34.13.0.

## Install

Download **`NammaHomeStay-v1.0.0.apk`** below and install it (you may need to allow "install from unknown sources"). It's a signed release APK (APK Signature Scheme v2).

To run against your own Firebase project, see the **Getting started** section in the [README](README.md): enable the Phone sign-in provider (+ a test phone number for dev), create a Firestore database, and deploy `firestore.rules`.

## Known limitations

- No traveller-facing app yet — "Interests" is seeded via the test button.
- 6-photo cap on the homestay and one dish photo, both heavily compressed (Firestore document size limit). Cloud Storage on a paid plan removes this.
- Support phone number in *Guide & Help* is a placeholder (`+911800000000`).
- R8/minification is off in the release build (Firestore POJO models need `-keep` rules first) — production hardening is a follow-up.
- No automated tests beyond the template stubs.

---

**Full setup & architecture:** see the [README](README.md).
