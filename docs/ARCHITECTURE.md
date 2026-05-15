# Architecture

Namma HomeStay is a single-module Android app, Kotlin + Jetpack Compose, **MVVM + a thin repository layer** for everything that touches Firestore.

> Plain-language goal: a host opens the app, fills three big cards, posts a photo, and is "live". A traveller opens the app, scrolls the list (or the map), taps a card, taps **I'm interested**, and the host's phone rings minutes later.

---

## High-level diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                              MainActivity                             │
│                                                                       │
│                                AppRoot                                │
│                    (auth-state + mode-state gate)                     │
│                                                                       │
│       ┌───────────────┬───────────────┬───────────────────────┐       │
│       │               │               │                       │       │
│       ▼               ▼               ▼                       ▼       │
│   LoginScreen   ModePickerScreen  MainScreen (host)    TravellerMain  │
│                 (first sign-in,    bottom-nav shell    (bottom-nav    │
│                  asks role+name)  + Pin Location +    shell + Detail) │
│                                    Today's Menu +     │               │
│                                    Inquiry Box        │               │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
                  │                                  │
                  └───────────────┬──────────────────┘
                                  ▼
                      ┌─────────────────────────┐
                      │   ViewModel layer       │
                      │   (StateFlow only)      │
                      └────────────┬────────────┘
                                   ▼
                      ┌─────────────────────────┐
                      │   Repository layer      │
                      │   (Flow<…> + suspend)   │
                      └────────────┬────────────┘
                                   ▼
                      ┌─────────────────────────┐
                      │  Firebase / DataStore   │
                      │  (no Cloud Storage —    │
                      │   photos are Blobs)     │
                      └─────────────────────────┘
```

---

## Packages

```
com.ifsvivek.nammahomestay/
├── NammaHomeStayApp.kt        # Application — Firebase + Firestore offline
│                                cache + osmdroid configuration
├── MainActivity.kt            # Auth gate → mode gate → host/traveller shell
│
├── data/
│   ├── model/Models.kt        # Host, Homestay, DailyMenu, Inquiry, Review,
│   │                            VerificationChecklist, AggregateRating
│   ├── FirestoreCollections.kt
│   ├── role/                  # UserMode + UserModeStore (DataStore preferences)
│   ├── seed/
│   │   └── SampleDataSeeder.kt   # one-tap "populate the marketplace" helper
│   └── repository/
│       ├── AuthRepository.kt        # phone-OTP sign-in, display-name updates
│       ├── HostRepository.kt        # write the host's shopfront + map pin
│       ├── HomestayBrowseRepository.kt   # observe LIVE homestays + today's menus
│       ├── MenuRepository.kt        # publish today's menu in a single set()
│       ├── InquiryRepository.kt     # send + observe inquiries (both sides)
│       └── ReviewRepository.kt      # 1–5 star reviews + aggregate ratings
│
├── ui/
│   ├── theme/                 # Earth-tone Color.kt, Theme.kt, Type.kt
│   ├── components/            # BigActionButton, SectionCard, NammaTopBar,
│   │                            PhotoImage, OsmMap, ModeSwitchButton, …
│   ├── navigation/Destinations.kt    # host bottom-nav routes
│   ├── MainScreen.kt          # host shell (bottom nav + map-pin route)
│   ├── HostShellViewModel.kt  # exposes the pending-inquiry count to the nav
│   ├── auth/                  # AuthViewModel + LoginScreen
│   ├── role/                  # RoleViewModel + ModePickerScreen
│   ├── home/                  # HomeViewModel + HomeProfileScreen + MapPin*
│   ├── menu/                  # MenuViewModel + DailyMenuScreen (the priority MVP screen)
│   ├── inquiry/               # InquiryViewModel + InquiryScreen
│   ├── guide/                 # GuideScreen (help cards + sign-out + seeder)
│   └── traveller/             # TravellerMainScreen + browse / detail / myinterests
│
└── util/                      # ImageCompressor, Dialer (ACTION_DIAL),
                                 ContextExt (findActivity), …
```

---

## Patterns we follow

### MVVM + a thin repository layer
- **ViewModel** owns a single `StateFlow<UiState>`. The `UiState` is a plain data class.
- ViewModels never see Firestore types directly — they call **Repository** methods that return `Flow<DomainModel>` or `suspend` fun.
- Repositories wrap Firebase SDK calls. Every snapshot listener is exposed as a `Flow` via `callbackFlow { ... awaitClose { reg.remove() } }`.
- Compose screens are **stateless** — they receive state + callbacks, never call repos directly.

### One snapshot listener per screen
Every list / detail view subscribes to a `Flow` keyed on the resource it needs. Firestore's offline cache means the listener is essentially free, and the UI updates the instant another device writes.

### No Cloud Storage — photos are JPEG `Blob`s in the document
The free Firebase Spark plan no longer includes Cloud Storage for new projects. So:
- `util/ImageCompressor` scales an image to ≤ 1080 px and keeps dropping JPEG quality until the bytes fit a target budget (≈ 140 KB for home photos, ≈ 350 KB for the dish photo).
- Those bytes are wrapped in `com.google.firebase.firestore.Blob` and stored on the `Homestay` / `DailyMenu` doc directly.
- `Homestay.MAX_PHOTOS` caps homestay photos at 6 so the doc never approaches Firestore's ~1 MB cap.
- `ui/components/PhotoImage` decodes a `Blob` off the main thread (`produceState { withContext(Dispatchers.Default) { decode } }`) so cards stay scrolly.

### Uber-style mode switch
- `data/role/UserModeStore` persists `UserMode = HOST | TRAVELLER` via DataStore Preferences (per device).
- `RoleViewModel` exposes a `ModeState` sealed type: `Loading | NotChosen | Chosen(mode)`.
- `AppRoot` (in `MainActivity`) gates on this: not-signed-in → `LoginScreen`, signed-in + NotChosen → `ModePickerScreen`, signed-in + Chosen(HOST) → `MainScreen`, signed-in + Chosen(TRAVELLER) → `TravellerMainScreen`.
- Sign-out clears the persisted mode so a different user on the same device sees the picker.
- A `ModeSwitchButton` lives in every top bar; tapping it flips the mode.

### Composite-index-free Firestore queries
Every snapshot listener uses a **single** `whereEqualTo` (no `orderBy`) so Firestore doesn't demand a composite index before it'll work. Sorting is done client-side in Kotlin — fine for the few-dozen-items volumes we'll see. Snapshot errors are `Log.e`-ed so future regressions are debuggable from Logcat.

### Aggregated reviews are computed on the client
With no Cloud Functions on the free plan, the "★ X.X (Y)" aggregate that decorates browse cards is computed in `ReviewRepository.observeAggregatesByHostId()` by reading every review and grouping by `homestayId` in memory. Fine until the project has thousands of reviews; at that scale you'd want a precomputed aggregate updated by a Cloud Function.

### OSM (not Google Maps)
- `osmdroid:osmdroid-android:6.1.20` — Apache-2.0, OpenStreetMap tiles, no API key.
- `ui/components/OsmMap` is a thin `AndroidView` wrapper:
  - Forwards Compose lifecycle to osmdroid (`onResume` / `onPause` / `onDetach`).
  - Adds osmdroid's `CopyrightOverlay` ("© OpenStreetMap contributors" — TOS requirement).
  - Rasterises our `ic_map_pin.xml` vector to a `BitmapDrawable` once (osmdroid's `Marker.icon` is unreliable with raw `VectorDrawable`s).
  - Forces a `RenderNode`-level clip via `Modifier.graphicsLayer { clip = true }.clipToBounds()` + `clipToOutline = true` on the `MapView` so gesture-time invalidations stay inside the slot.
  - Auto-zooms to fit all markers when the list changes.
- `NammaHomeStayApp.onCreate` sets osmdroid's `userAgentValue` and tile-cache directory.

### Localization
Resource-only — no runtime translation library:
- `res/values/strings.xml` is the default (English).
- `res/values-hi/strings.xml` (Hindi) and `res/values-kn/strings.xml` (Kannada) override per-string.
- Android picks the right locale at runtime from the device's language list. Test it via Settings → System → Languages.

---

## Firestore data model

See [README's "Firestore data model" section](../README.md#firestore-data-model) for the full schema. In one paragraph:

- `hosts/{uid}` — minor profile mirror.
- `homestays/{uid}` — the host's shopfront. Photos are `Blob[]` (max 6), checklist is a nested object, `live` is the boolean travellers filter on, optional `latitude`/`longitude` for the map view. Demo data lives at `homestays/sample-<region>-<n>` (write rule allows any signed-in user to touch the `sample-*` path).
- `daily_menus/{uid}` — overwritten daily in one `set()`. Image is a `Blob?`.
- `inquiries/{autoId}` — `hostId` + `travellerId` + status. Both sides read their own half via a single `whereEqualTo`.
- `reviews/{autoId}` — 1–5 stars + optional comment, keyed by `homestayId` + `travellerId`. Sample reviews use ids like `sample-coorg-02-r3`.

[`firestore.rules`](../firestore.rules) is the authoritative version.

---

## Release builds

The release build uses **R8 + resource shrinking**. Keep-rules for the Firestore POJOs and osmdroid live in [`app/proguard-rules.pro`](../app/proguard-rules.pro). The release APK is signed by `app/release-keystore.jks` *if* `keystore.properties` exists at the repo root (it's gitignored). On a CI / clean clone the release build is left **unsigned** rather than failing.

---

## Continuous integration

[`.github/workflows/build.yml`](../.github/workflows/build.yml) runs on every push to `main` and on every PR:
- Sets up JDK 17 + Gradle cache
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:assembleRelease` (unsigned, since CI has no keystore)
- Uploads the debug APK as an artifact (14-day retention)
- Uploads the test reports on failure

There's no secret leakage — `google-services.json` is intentionally committed (its keys are restricted by package + SHA-1; Google considers this safe).
