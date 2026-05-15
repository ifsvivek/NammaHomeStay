# Firebase setup

The app expects three Firebase services to be configured. **No paid plan / Blaze is required** — everything runs on the free Spark tier (Cloud Storage is intentionally not used; photos ride along inside Firestore documents).

The currently-wired project is `nammahomestay-dfe84230`. To point at your own project, replace [`app/google-services.json`](../app/google-services.json) and update [`.firebaserc`](../.firebaserc).

---

## 1 · Authentication — Phone provider

The whole sign-in flow is one phone number → one 6-digit SMS code.

1. Open https://console.firebase.google.com/project/_/authentication and click **Get started** (one-time).
2. **Sign-in method** tab → click **Phone** in the provider list → toggle **Enable** → **Save**.
3. *(For development on the Spark plan, real SMS is not delivered.)* Scroll to **Phone numbers for testing** and add the two demo numbers the README documents:

   | Phone number     | Verification code |
   | ---------------- | ----------------- |
   | `+91 9876543210` | `123456`          |
   | `+91 9988776655` | `123456`          |

4. *(For real numbers, later)* Add your debug **SHA-1** under ⚙ → Project settings → **Your apps** → the Android app → **Add fingerprint**. Get it with `./gradlew signingReport` and copy the `SHA1` under `Variant: debug`. Re-download `google-services.json` afterwards.

> Symptom if you skip step 1: `An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]` shown under "Send code".

---

## 2 · Cloud Firestore

1. Open Firestore Database → **Create database**.
2. Pick a location (e.g. `asia-south1` for India). This can't be changed later.
3. Either start in **test mode** (everything readable/writable until the rule expires) or, recommended, deploy our security rules — see the next section.

> The app's queries are deliberately composite-index-free (single `whereEqualTo` + client-side sort). You should never need to create a Firestore index by hand.

---

## 3 · Security rules

The repo ships a `firestore.rules` that locks the database down properly:
- **Hosts** own their own `hosts/{uid}`, `homestays/{uid}`, `daily_menus/{uid}`.
- **Travellers** can read every LIVE homestay + its today's menu, can create an inquiry only with their own `travellerId`, and can read inquiries they sent.
- **Reviews** are world-readable for any signed-in user; only the author can create/edit; rating must be an int 1..5.
- Sample data (doc ids starting with `sample-`) is writable by any signed-in user so the in-app **"Add demo data"** seeder works.

Deploy with the Firebase CLI:

```bash
npm i -g firebase-tools          # one-time
firebase login
firebase deploy --only firestore:rules
```

Or, if you don't want to use the CLI, paste the contents of [`firestore.rules`](../firestore.rules) into the Firebase console → **Firestore Database** → **Rules** tab → Publish.

---

## 4 · Cloud Storage — *not* used

Don't enable it. The free plan no longer includes Cloud Storage for new projects, and the app sidesteps that constraint by storing JPEG bytes as `Blob`s on the Firestore documents themselves (see [ARCHITECTURE.md](ARCHITECTURE.md#no-cloud-storage--photos-are-jpeg-blobs-in-the-document)).

If you upgrade to Blaze later and want Storage-backed photos, the only file that needs to change is the repository layer — see the `addPhoto` / `uploadMenuPhoto` shape we used pre-v1.0.

---

## 5 · Seeding sample data

Once Authentication and Firestore are live:

1. Build and install the app.
2. Sign in with a test number, pick **"I'm hosting"** mode.
3. Open **Help** (bottom-right tab).
4. Scroll to **Demo tools** → tap **"Add demo data"**.
5. Switch to **Traveller mode** (top-right pill). Five seeded homestays appear in **Find** with real-ish photos, reviews, and today's menus pinned to actual lat/lngs across Karnataka & Wayanad.

Re-running the seeder is safe — every sample uses a deterministic doc id so the second call overwrites instead of duplicating.

---

## Troubleshooting

| Symptom                                                               | Cause                                                                                       | Fix                                                                             |
| --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `CONFIGURATION_NOT_FOUND` on "Send code"                              | Authentication never initialised in the project                                             | Click **Get started** under Authentication (step 1 above).                      |
| Sign-in succeeds but `homestays` write fails with `PERMISSION_DENIED` | You skipped deploying `firestore.rules` and the database is in production-mode default-deny | `firebase deploy --only firestore:rules` *or* paste the rules into the console. |
| Browse map is empty even though you pinned your homestay              | Your homestay isn't LIVE yet (need name + ≥1 photo + all three promises)                    | Finish the **Setup your Home** progress card.                                   |
| Inquiries don't show up in **Sent**                                   | Old client (≤ v1.0.0) — was a composite-index bug                                           | Update to v1.1.0+.                                                              |
| Photo upload fails on a real number                                   | No SHA-1 registered → reCAPTCHA / Play Integrity fallback misbehaves                        | Add your debug SHA-1 to the Firebase app (step 1.4).                            |
