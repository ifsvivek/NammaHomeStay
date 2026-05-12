<div align="center">

# 🏡 Namma HomeStay

**A "Simplified Host Portal" that makes digital marketing as easy as making a phone call.**

Built for rural farmers and homemakers with low digital literacy — big buttons, high contrast, warm earth tones, and zero technical jargon.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-2026.02-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-555)
![Backend](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)

</div>

---

## Why this app exists

A rural host has a spare room, home-cooked food, and a phone — but no idea how to "put it online". Namma HomeStay is the bridge: a host opens the app, taps a few large cards, takes a photo of today's lunch, and travellers can find them. The whole product is built around one rule:

> **Less is More.** If a step needs an explanation, the step is wrong.

### Design rules (non-negotiable)

| Rule | What it means in the app |
|---|---|
| **Large touch targets** | Every action is a ≥ 48 dp control; the primary buttons are 60 dp tall. |
| **Earth tones** | Greens / browns / creams (eco-tourism). Dynamic wallpaper colour is **off** so the brand stays consistent. |
| **Big type** | Headlines use `headlineMedium`; body text is ≥ 18 sp. |
| **Always confirm** | Every save shows a `Snackbar` or a big success ✓ animation — "the internet did its job." |
| **No jargon** | "Promises to your guests", not "Verification checklist". "Interests", not "Leads". |

---

## Features

### 📱 Frictionless onboarding
One screen. Phone number → 6-digit SMS code → you're in. No passwords, no email, with SMS auto-retrieval when the device supports it. First sign-in drops you straight onto a **"Setup your Home"** progress bar.

### 🏠 My Home — the digital shopfront
A `LazyColumn` of large cards:
- **Setup progress** — "X of 5 done", with a live progress bar and a clear **LIVE / NOT LIVE YET** pill.
- **Home name & village** — two fields, one Save button.
- **Photos** — a "Tap to add photo" tile + a strip of thumbnails (up to 6), each removable. Photos are aggressively compressed for rural data.
- **Promises to your guests** — three big toggles (clean bedding · working washroom · drinking water). The home only goes **LIVE** when all three are on, there's at least one photo, and the home has a name.
- A floating **"Today's Menu"** button, always one tap away.

### 🍲 The "60-Second Menu" — the core feature
Built to feel like posting a WhatsApp status: **one photo + one dish name + one price + one big button.** Publishing is a single Firestore `set()` so it's genuinely fast, and a full-screen ✓ pops in to confirm. Editing today's menu pre-fills the last one.

### 📞 Inquiry Box & direct connect
"Incoming Interests" lists travellers (name + relative time + status). Tap a card to expand it and hit the unmistakable **green "Call Guest"** button — it opens the phone dialer with the number pre-filled (`ACTION_DIAL`, so no call permission is ever requested) and marks the inquiry as called. (There's no traveller-facing app yet, so the screen has an **"Add a sample interest"** button to seed test data.)

### 📖 Guide & Help
Plain-language cards explaining every feature, a "Call support" button, and sign out.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + a thin repository layer |
| Auth | Firebase Phone Auth (OTP) |
| Database | Cloud Firestore (real-time listeners, on-device offline cache) |
| Images | **Stored as compressed JPEG `Blob`s inside Firestore documents** — see note below. Coil renders the local pick-preview; a custom `PhotoImage` decodes stored blobs off the main thread. |
| Image picking | `ActivityResultContracts.GetContent` |
| Navigation | Navigation-Compose (4-tab bottom bar) |

### A note on photos & the free plan

The free Firebase **Spark** plan no longer includes Cloud Storage for new projects, so this app **does not use Cloud Storage at all**. Instead:

- `ImageCompressor` scales an image down (≤ ~1080 px) and then keeps lowering JPEG quality until the bytes fit a budget (≈ 140 KB for home photos, ≈ 350 KB for the dish photo).
- Those bytes are stored as a Firestore `Blob` directly on the document — well under Firestore's ~1 MB per-document limit.
- `Homestay` therefore caps photos at **6**. (Want full-resolution or unlimited photos later? Upgrade to the Blaze plan and move blobs into Cloud Storage — only the repository layer changes.)

---

## Project structure

```
app/src/main/java/com/ifsvivek/nammahomestay/
├─ NammaHomeStayApp.kt         # Application — Firebase init + Firestore offline cache
├─ MainActivity.kt             # Auth gate: LoginScreen ↔ MainScreen
├─ data/
│  ├─ model/Models.kt          # Host, Homestay, DailyMenu, Inquiry, VerificationChecklist
│  ├─ FirestoreCollections.kt
│  └─ repository/              # AuthRepository, HostRepository, MenuRepository, InquiryRepository
├─ ui/
│  ├─ theme/                   # Earth-tone colours, large typography
│  ├─ components/Components.kt  # BigActionButton, SectionCard, SetupProgressCard, PhotoImage, NammaTopBar, …
│  ├─ navigation/Destinations.kt
│  ├─ MainScreen.kt            # bottom-nav shell + NavHost
│  ├─ auth/                    # AuthViewModel, LoginScreen
│  ├─ home/                    # HomeViewModel, HomeProfileScreen
│  ├─ menu/                    # MenuViewModel, DailyMenuScreen   ← the priority MVP screen
│  ├─ inquiry/                 # InquiryViewModel, InquiryScreen
│  └─ guide/GuideScreen.kt
└─ util/                       # ImageCompressor, Dialer (ACTION_DIAL), ContextExt
```

---

## Firestore data model

```
hosts/{uid}
  uid: string
  name: string
  phone: string
  verifiedStatus: "new" | "verified"     # flips to "verified" once the home is LIVE

homestays/{uid}                            # one shopfront per host; doc id == host uid
  hostId: string
  name: string
  location: string
  images: Blob[]                           # compressed JPEGs, max 6
  checklist: { cleanBedding, functionalWashroom, drinkingWater: bool }
  live: bool

daily_menus/{uid}                          # one doc per host; overwritten daily in a single set()
  hostId: string
  dishName: string
  price: number
  image: Blob | null
  dateTimestamp: timestamp

inquiries/{autoId}
  hostId: string
  guestName: string
  guestPhone: string
  status: "pending" | "called" | "closed"
  timestamp: timestamp
```

Security rules are in [`firestore.rules`](firestore.rules) — a host can only read/write their own `hosts` / `homestays` / `daily_menus`, and any signed-in user can create an `inquiry`.

---

## Getting started

### 1. Open the project
Clone it and open in Android Studio (latest stable). It uses the Gradle version catalog ([`gradle/libs.versions.toml`](gradle/libs.versions.toml)) — just let it sync.

### 2. Connect a Firebase project
- Create a Firebase project and an Android app with package name **`com.ifsvivek.nammahomestay`**.
- Download `google-services.json` into [`app/`](app/). (A working one for the project `nammahomestay-dfe84230` is already committed for development.)

### 3. Turn on the backend (in the Firebase console)
1. **Authentication → Sign-in method → Phone → Enable.** For development, also add a *test phone number* under "Phone numbers for testing" (e.g. `+91 9876543210` with code `123456`) so you don't need real SMS or a SHA-1 on an emulator.
2. **Firestore Database → Create database** (pick a region, e.g. `asia-south1`).
3. Deploy the security rules — `firebase deploy --only firestore:rules` (the repo has [`firebase.json`](firebase.json) + [`.firebaserc`](.firebaserc)) — or just start the database in test mode while developing.
4. *(For real phone numbers, later)* add your debug **SHA-1** under Project settings → Your apps, and re-download `google-services.json`.

> No Cloud Storage setup is needed — see the note above.

### 4. Build & run
```bash
./gradlew :app:assembleDebug      # build the debug APK
./gradlew installDebug            # install on a connected device/emulator
```
> If a build ever fails with a stale `mergeDebugResources` / `merged.dir/values.xml` error, run `./gradlew clean` and rebuild — that's an incremental-cache glitch, not the code.

---

## Roadmap / known limitations

- **No traveller-facing app yet** — the "Interests" screen is seeded via a test button. A browse-and-enquire app would write to `inquiries`.
- **6-photo cap** on the homestay and one dish photo, both heavily compressed (Firestore document size limit). Cloud Storage on a paid plan removes this.
- Support phone number in *Guide & Help* is a placeholder (`+911800000000`) — swap it before shipping.
- No automated tests yet beyond the template stubs.

---

<div align="center">
Made with care for hosts who'd rather make a phone call than fill out a form.
</div>
