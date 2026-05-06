# Changelog

All notable changes to the WhiskyWise Android app are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The app connects to a self-hosted [WhiskyWise](https://github.com/prolife86/WhiskyWise) server.  
See that project's changelog for server-side changes.

---

## [Unreleased]

---

# WhiskyWise Android — v0.1.1 · "First Day Patch"

v0.1.0 shipped on a Tuesday.
By Wednesday it needed a patch.

The app crashed on first launch because Google helpfully restored encrypted
preferences from the previous install — encrypted with a key that no longer exists.
The app tried to decrypt them, failed cryptographically, and died before showing
a single pixel. Fixed: detect the corruption, wipe it, start fresh.

The rotate button also didn't rotate anything. The server was doing its job.
The app was replacing the rotated photo with a placeholder by passing a fake
filename to Glide. Fixed: pass the actual filename.

Two bugs. Both embarrassing in different ways. Both gone.

---

## [0.1.0] — 2026-05-06 🎉 First Public Release

### Fixed
- **App crashes when taking a photo** — `TakePicture` does not request `CAMERA`
  permission on its own. If the user hadn't previously opened the barcode scanner
  (which does ask), the app crashed silently on launch of the camera. Permission is
  now requested explicitly before the camera is opened, with a graceful Snackbar if
  denied.
- **Photos not loading for existing whiskies** — the server's `GET /api/photo/<filename>`
  endpoint was using `@login_required` (browser session only) instead of
  `@api_login_required` (Bearer token + session). Every photo request from the app
  received a redirect to `/login`. Fixed server-side in v1.5.2; no app code changed.

### Added
- **Camera capture** — tapping a photo button now offers a choice: **Take photo**
  (camera) or **Choose from gallery**. Previously only the gallery was available.
  Uses `TakePicture` with a `FileProvider` URI so no external storage permission is
  required on Android 10+.
- **Rotate photo** — a **↻** button appears next to each saved photo in the edit
  screen. Tapping it rotates the image 90° clockwise on the server and reloads the
  thumbnail. Only available for photos already saved to the server; not shown for
  locally queued uploads.
- `FileProvider` declaration and `res/xml/file_paths.xml` — required for camera
  capture URI on Android 10+.
- `POST /api/photo/{id}/{slot}/rotate` wired in `WhiskyWiseApi` and
  `WhiskyWiseRepository`.

### Notes
- Requires WhiskyWise server ≥ 1.5.2.
- Minimum Android version: API 26 (Android 8.0 Oreo).

---

## [0.0.6] — 2026-05-06 🔍 Visible Progress

Turns out "fully implemented" and "actually works" are two different things.

### Fixed
- **Edit button didn't exist** — `MenuProvider` inflates menu items into a `Toolbar`.
  There was no `Toolbar`. The edit button was therefore nowhere. Added a `Toolbar` to
  `activity_main.xml` and wired it up as the support action bar — now the Edit and
  Delete actions appear in the detail screen as intended, and the back arrow shows up
  automatically when navigating into any sub-screen.
- **Radar chart showed only a grid** — three issues conspiring. First, radar fields in
  `WhiskyRequest` were `Int?` defaulting to `null`, which Gson silently omits, so the
  server never received the slider values and stored 0s. Second, `RadarView` drew
  all-zero data as a single invisible point at the centre. Third — and the one that
  persisted after the first two were fixed — the Android `Whisky` model expected flat
  `radar_woody` keys but the server returns a nested object: `"radar": {"woody": 3, ...}`.
  Gson silently ignored it. All three fixed: radar fields are non-nullable `Int`, the
  chart uses a minimum render ratio, and the model now maps the `"radar"` object via a
  dedicated `RadarData` class with convenience accessors so nothing else changed.
- **Edit form shows all sliders at zero** — same root cause as above. The server's
  stored values were being discarded on deserialisation because the response shape
  didn't match the model. The edit form now pre-fills from the actual stored values.
- **Photos not loading** — `loadWhiskyPhoto()` assumed the server always returns a bare
  filename. The URL builder now normalises whatever the server returns before
  constructing the final URL.
- **Photo buttons missing when adding a new whisky** — fragments rendered flush against
  the status bar due to the missing `Toolbar`, offsetting the scroll view and hiding
  the lower half of the form.
- **App crashed when tapping "Choose" on a photo slot** — `Intent(ACTION_PICK)` with
  `image/*` throws `ActivityNotFoundException` on Android 13+ with targetSdk 35.
  Replaced with `ActivityResultContracts.GetContent`.

---

## [0.0.5] — 2026-05-05 👁️ It Was There All Along

### Fixed
- **Radar chart showing no data** — all seven flavour axes defaulted to 0 because the
  edit form had no sliders. Added 0–5 `SeekBar` sliders with live value labels.
- **Cannot edit a saved whisky** — the edit form had no barcode, radar, or photo fields,
  silently discarding everything not in the original form. All fields are now present
  and pre-filled when editing.
- **Photos not visible on whisky detail** — the edit form had no photo UI so no photos
  were ever uploaded. The detail view now displays the front photo as a hero image.
- **Cannot add photos** — the edit form now includes Front, Back, and Cask photo rows
  with a **Choose** and **Remove** button each.
- **Barcode not shown in detail view** — added a BARCODE row (hidden when blank).
- **No barcode scanning** — added a Barcode field and a **Scan** button. Opens
  `BarcodeScanActivity` (CameraX + MLKit) that scans and returns the value.
- **API tokens cannot be revoked** — replaced the token count field with a
  `RecyclerView` listing each token by name, creation date, and last-used date, each
  with a **Revoke** button and confirmation dialog.
- **Stale collection after editing** — `onResume()` now reloads the list automatically.
- **Wishlist items were decorative** — tapping a wishlist entry now opens the detail screen.
- **Double photo upload on rotation** — the saved ID is now consumed immediately after
  use; events are not a subscription service.
- **Error Snackbar haunting subsequent screens** — errors are cleared after display.
- **`flavor_profile` silently dropped on edit** — the server-computed classification is
  now round-tripped correctly.
- **Bearer token logged in release builds** — `HttpLoggingInterceptor` now uses `NONE`
  in release builds.
- **`@OptIn(ExperimentalGetImage)` on the wrong scope** — moved to `startCamera()`.
  The CI build no longer fails.

### Added
- `BarcodeScanActivity` — full-screen CameraX barcode scanner.
- `TokenAdapter` + `item_token.xml` — per-token rows in the Settings screen.
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` permissions for gallery picker.

### Changed
- `EditWhiskyViewModel` — replaced `_saved: LiveData<Boolean>` with
  `_savedId: LiveData<Int?>` so photo uploads run before navigating back.

---

## [0.0.4] — 2026-05-05 🔧 It Actually Works Now

### Fixed
- **Empty collection screen** — `RecyclerView` was missing a `LayoutManager`.
- **"RetrofitClient not initialised" crash** — added `WhiskyWiseApp` to initialise
  the API client at process start regardless of which screen Android restores first.
- **Camera cutout / notch overlap** — status bar is now transparent and layouts
  respect system window insets.
- **White circle launcher icon** — added proper adaptive icon definitions.

---

## [0.0.3] — 2026-05-04 🔧 Build & Code Quality

### Fixed
- **Deprecated toolbar menu API** — replaced with the modern `MenuProvider` API.
- **Gradle 10 compatibility warning** — added `android.suppressUnsupportedCompileSdk=35`.
- **MLKit native library strip warning** — added `packaging { jniLibs { keepDebugSymbols } }`.

### Changed
- **App icon** — replaced the placeholder with a custom WhiskyWise bottle icon.

---

## [0.0.2] — 2026-05-04 🐛 Hotfix

### Fixed
- **App crash on launch** — `SingleResponse<T>` generic wrapper caused a Gson type
  erasure error at runtime. Replaced with concrete response classes.

---

## [0.0.1] — 2026-05-03 🚧 Initial Pre-release

### Added

**App**
- **Bearer token login** — server URL, username and password. Token stored in
  `EncryptedSharedPreferences` (AES256-GCM); password never saved on device.
- **Auto-login** — stored token skips the login screen on subsequent launches.
- **Collection screen** — browse with free-text search, status filter chips, and
  pull-to-refresh.
- **Whisky detail screen** — hero photo, tasting notes, metadata, dominant flavour,
  and a native radar chart for the seven flavour axes.
- **Add / Edit form** — create or edit collection entries; all API fields supported.
- **Wishlist screen** — browse with pull-to-refresh; add via quick-add dialog.
- **Settings screen** — server URL, token count, app version, Log Out.

**Build & CI**
- GitHub Actions pipeline — debug APK on every push; signed APK + AAB on release.
- Automated versioning from git release tag.
- Issue management workflows and templates.

**Dependencies**
- Gradle 9.5.0 / AGP 9.2.0 / Kotlin 2.3.21
- compileSdk / targetSdk 35 (Android 15)
- Retrofit 2.11.0 + OkHttp 4.12.0 · Navigation 2.9.0 · Lifecycle 2.9.0
- Glide 4.16.0 · AndroidX Security Crypto 1.1.0-alpha06
- CameraX 1.4.2 · MLKit Barcode Scanning 17.3.0

### Notes
- Minimum Android version: API 26 (Android 8.0 Oreo).
- The `gradlew` and `gradle-wrapper.jar` are not committed; generated by CI at build time.
