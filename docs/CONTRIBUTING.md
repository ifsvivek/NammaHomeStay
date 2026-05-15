# Contributing

Thanks for taking a look! Whether you're filing a bug report, suggesting a feature, or sending a pull request — here's how to get up and running.

---

## Set up your dev environment

| Tool           | Tested with                                    |
| -------------- | ---------------------------------------------- |
| Android Studio | Latest stable (Koala or newer)                 |
| JDK            | 17 (bundled with Android Studio)               |
| Android SDK    | API 36, build-tools 37                         |
| `gh` CLI       | optional, only if you want to cut releases     |
| `firebase` CLI | optional, only for deploying `firestore.rules` |

```bash
git clone https://github.com/ifsvivek/NammaHomeStay.git
cd NammaHomeStay
./gradlew :app:assembleDebug         # first build pulls deps; ~2-3 min
./gradlew :app:installDebug          # install on connected device/emulator
```

The Gradle version catalog is the single source of truth for dependency versions — see [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). Android Studio will pick it up automatically.

A working `app/google-services.json` for the project `nammahomestay-dfe84230` is committed so the app runs out of the box. If you'd rather use your own Firebase project, follow [`docs/FIREBASE_SETUP.md`](FIREBASE_SETUP.md).

---

## Run tests

```bash
./gradlew :app:testDebugUnitTest
```

There are five JUnit unit tests on the LIVE-eligibility / aggregate-rating domain logic. Compose-UI tests and Firestore-emulator integration tests are on the roadmap — see [README's Future improvements](../README.md#future-improvements).

CI runs the same tests + a debug + an unsigned release build on every push to `main` and every PR — see [`.github/workflows/build.yml`](../.github/workflows/build.yml).

---

## Project structure at a glance

The full map lives in [`docs/ARCHITECTURE.md`](ARCHITECTURE.md). The 30-second version:

```
data/        # models + repositories + DataStore mode store + seed/
ui/          # Compose screens + ViewModels per feature, plus shared components/
util/        # ImageCompressor, Dialer, ContextExt
```

Every screen is **stateless** Compose with a corresponding ViewModel exposing `StateFlow<UiState>`. Repositories return `Flow<DomainModel>` (snapshot listeners under the hood) or `suspend` mutators. Screens never touch Firestore directly.

---

## Coding conventions

- **Kotlin**, idiomatic; data classes with all `val` parameters where possible.
- Compose composables are stateless; state lives in `ViewModel`s.
- Comments only explain *why*, not *what* — the code already says *what*. If you find yourself documenting a non-obvious constraint, a hidden invariant, or a workaround for a specific bug, that's the right comment to write.
- Match the project's existing visual treatment: earth-tone palette (see [`Color.kt`](../app/src/main/java/com/ifsvivek/nammahomestay/ui/theme/Color.kt)), large type (`headlineMedium` for primary titles, ≥18 sp body), ≥48 dp touch targets, 60 dp primary buttons. Every action gets a `Snackbar` or success animation.

---

## Adding a new feature

A typical change touches three layers:

1. **Domain model** in [`data/model/Models.kt`](../app/src/main/java/com/ifsvivek/nammahomestay/data/model/Models.kt) — a `data class` with all-default fields (so Firestore can deserialise it via no-arg ctor).
2. **Repository** in [`data/repository/`](../app/src/main/java/com/ifsvivek/nammahomestay/data/repository/) — `observe…(): Flow<…>` for reads + `suspend fun` for writes.
3. **ViewModel** in [`ui/<feature>/`](../app/src/main/java/com/ifsvivek/nammahomestay/ui/) — one `MutableStateFlow<UiState>`, methods called by the Composable.
4. **Composable** screen — stateless, takes the `UiState` + callbacks. Use `collectAsStateWithLifecycle()` to subscribe.
5. **Rules** in [`firestore.rules`](../firestore.rules) — most new collections need an explicit allow/deny. The existing functions `signedIn()`, `isOwner(uid)`, `isSample(anyId)` are usually enough.

If you're adding strings the user will see, please add them to [`res/values/strings.xml`](../app/src/main/res/strings.xml) AND the Hindi + Kannada overlays — see [`res/values-hi/`](../app/src/main/res/values-hi/) and [`res/values-kn/`](../app/src/main/res/values-kn/). If you can't translate, leave the value as the English text and we'll backfill.

---

## Commit messages

We follow a loose [Conventional Commits](https://www.conventionalcommits.org/) style. The first line is `<type>: <imperative summary>`, where `<type>` is one of:

| Type       | Use for                                         |
| ---------- | ----------------------------------------------- |
| `feat`     | A new user-visible feature or screen            |
| `fix`      | A bug fix                                       |
| `docs`     | README, docs/, KDoc-only changes                |
| `chore`    | Build files, dependency bumps, release prep     |
| `refactor` | Internal restructuring without behaviour change |
| `test`     | Adding or updating tests                        |

The body explains *why*, not *what*. The diff already shows *what*. A good commit message tells the next reader what trade-off you weighed.

Example:
```
fix(map): stop the MapView painting over the toolbar during pan/zoom

Compose Modifier.clip on a Box wrapping an AndroidView wasn't honoured
during osmdroid's gesture-time invalidations. Switched to
graphicsLayer { clip = true } + clipToOutline = true on the MapView so
the View canvas can't escape its bounds.
```

---

## Pull request checklist

Before opening a PR:

- [ ] `./gradlew :app:testDebugUnitTest` passes.
- [ ] `./gradlew :app:assembleDebug` passes.
- [ ] Any new user-visible strings live in `strings.xml` (with `values-hi` + `values-kn` if you can translate; otherwise note in the PR).
- [ ] Any new Firestore collection / write path has a rule in `firestore.rules`.
- [ ] You haven't accidentally committed `keystore.properties`, `*.jks`, or a `.docx` / `.pdf` report (the `.gitignore` should catch them — please verify).

---

## Cutting a release

For maintainers, after merging a release-worthy set of PRs to `main`:

```bash
# 1. Make sure keystore.properties + app/release-keystore.jks exist locally.
# 2. Bump versionCode + versionName in app/build.gradle.kts.
# 3. Build the signed release APK:
./gradlew :app:assembleRelease

# 4. Verify the signature:
$ANDROID_HOME/build-tools/*/apksigner verify --verbose \
    app/build/outputs/apk/release/app-release.apk

# 5. Cut the GitHub release (gh CLI must be authenticated):
gh release create vX.Y.Z \
    "app/build/outputs/apk/release/app-release.apk#NammaHomeStay-vX.Y.Z.apk" \
    --title "Namma HomeStay vX.Y.Z — short headline" \
    --notes-file path/to/release-notes.md \
    --latest
```

The release keystore is committed-locally-only (gitignored). **Don't lose it** — losing the keystore means you can never publish an update to the Play Store with the same package name.
