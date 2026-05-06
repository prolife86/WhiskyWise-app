# Changelog

All notable changes to the WhiskyWise Android app are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The app connects to a self-hosted [WhiskyWise](https://github.com/prolife86/WhiskyWise) server.  
See that project's changelog for server-side changes.

---

## [Unreleased]

---

## [0.0.7] — 2026-05-06 📸 The Camera Was There All Along

### Fixed
- **Photos not loading for existing whiskies** — the server's `GET /api/photo/<filename>`
  endpoint was decorated with `@login_required` (browser session cookies only). The app
  sends `Authorization: Bearer <token>`, which Flask-Login silently ignored — returning
  a redirect to `/login` instead of the image. Glide received an HTML page, showed the
  placeholder, and said nothing. Fixed server-side in v1.5.2 by switching to
  `@api_login_required`. No app code changed for this fix; requires server ≥ 1.5.2.
- **No camera option when adding photos** — tapping a photo button launched the system
  document picker, which only shows existing files. There was no way to take a new photo.
  Tapping a photo button now shows a dialog: **Take photo** or **Choose from gallery**.
  Camera capture uses `TakePicture` with a `FileProvider` URI so no external storage
  permission is required on Android 10+.

### Added
- `FileProvider` declaration in `AndroidManifest.xml` — required to pass a
  `content://` URI to the camera app for full-resolution capture.
- `res/xml/file_paths.xml` — declares `cacheDir` as the shared path for camera
  temp files.

### Notes
- Photo loading requires WhiskyWise server ≥ 1.5.2.

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
  filename. It doesn't — depending on server version the path may include `photos/` or
  `api/photo/` as a prefix, producing a double-pathed URL that 404s every time. The URL
  builder now normalises whatever the server returns before constructing the final URL.
- **Photo buttons missing when adding a new whisky** — the layout had them. The code
  wired them. They still weren't visible. Root cause: same missing `Toolbar` as above —
  without it claiming `?attr/actionBarSize` at the top of the layout, fragments rendered
  flush against the status bar and the scroll view was offset, making the lower half of
  the form appear to be missing fields that were actually just out of frame.
- **App crashed when tapping "Choose" on a photo slot** — `Intent(ACTION_PICK)` with
  `image/*` throws `ActivityNotFoundException` on Android 13+ devices with targetSdk 35.
  Replaced with `ActivityResultContracts.GetContent`, which routes through the system
  document picker, is always present, and handles storage permissions internally.

---

## [0.0.5] — 2026-05-05 (patch) · "While We Were Here"
 
Turns out shipping a working UI is easier when the code is also correct.
 
### Fixed
- **Stale collection after editing** — the list no longer requires a manual swipe-to-refresh
  after saving a new or edited whisky. `onResume()` now does the obvious thing.
- **Wishlist items were decorative** — tapping a wishlist entry did nothing. It now opens
  the detail screen, like a normal list item in a normal app.
- **Double photo upload on rotation** — rotating the screen after saving triggered the
  photo queue a second time. The saved ID is now consumed immediately after use; events
  are not a subscription service.
- **Error Snackbar haunting subsequent screens** — a failed save would re-show its error
  message after every screen rotation until the heat death of the ViewModel. Errors are
  now cleared after display.
- **`flavor_profile` silently dropped on edit** — the server-computed flavour classification
  was never included in the update payload, so every save quietly erased it. The value is
  now round-tripped correctly and the server can recompute it from the new radar values.
- **Bearer token logged in release builds** — `HttpLoggingInterceptor` was set to `BODY`
  unconditionally, writing auth tokens and whisky data to logcat in production. Logging is
  now `NONE` in release builds.
- **`@OptIn(ExperimentalGetImage)` on the wrong scope** — the annotation was on the class,
  but Lint (correctly) requires it on the function containing the actual `proxy.image` call.
  Moved to `startCamera()`. The CI build no longer fails.
- **Radar chart showing no data** — all seven flavour axes (Woody, Smoky, Cereal, Floral,
  Fruity, Medicinal, Fiery) defaulted to 0 because the edit form had no sliders to set
  them. The data polygon collapsed to an invisible dot at the centre of the grid.
  Added 0–5 `SeekBar` sliders with live value labels; values are now saved to the server
  and the chart renders correctly.
- **Cannot edit a saved whisky** — the edit form had no barcode, radar, or photo fields,
  so editing an existing entry silently discarded everything not in the original form.
  All fields are now present and pre-filled when editing.
- **Photos not visible on whisky detail** — `loadWhiskyPhoto()` existed and worked, but
  the edit form had no photo UI so no photos were ever uploaded. The detail view now
  displays the front photo as a hero image when one is present.
- **Cannot add photos** — the edit form now includes Front, Back, and Cask photo rows,
  each with a **Choose** button (system image gallery) and a **Remove** button. Selected
  images are uploaded to the server after the whisky record is saved; removals trigger
  a server-side delete.
- **Barcode not shown in detail view** — the `barcode` field existed in the model and API
  but had no corresponding view in `fragment_detail.xml`. Added a `BARCODE` row that
  appears only when a value is present.
- **No barcode scanning when adding/editing a whisky** — the edit form had no barcode
  field at all. Added a `Barcode` text input and a **Scan** button. Tapping Scan opens
  a new full-screen `BarcodeScanActivity` (CameraX + MLKit Barcode Scanning, already in
  the dependency tree) that auto-detects and returns the barcode without manual typing.
- **API tokens cannot be revoked** — the Settings screen showed only a token count
  `TextView`. `SettingsViewModel.revokeToken()` existed but had no UI to call it.
  Replaced the count field with a `RecyclerView` listing each token by name, creation
  date, and last-used date, each row with a **Revoke** button and confirmation dialog.
  - **Radar chart blank** — flavour profile axes (Woody, Smoky, Cereal, Floral, Fruity,
  Medicinal, Fiery) were never editable, so all seven values were always saved as 0 and
  the data polygon collapsed to an invisible dot at the centre. Added 0–5 sliders to the
  edit form; values are now saved and rendered correctly.
- **Edit form discards data** — opening an existing whisky in the edit screen no longer
  silently drops radar values, barcode and status; all fields are pre-filled from the
  server response.
- **Photos not visible** — `loadWhiskyPhoto()` was fully implemented but never called for
  new or edited entries because the edit form had no photo UI. Photos now load correctly
  in the detail view.
- **Photos cannot be added** — the edit form now includes Front / Back / Cask photo rows
  with a gallery picker and a Remove button. Selected images are uploaded to the server
  after the whisky record is saved; removals trigger a server-side delete.
- **Barcode not shown in detail view** — added a BARCODE row to the detail screen
  (hidden when blank).
- **No barcode scanning** — added a Barcode field and a **Scan** button to the edit form.
  Tapping Scan opens a new `BarcodeScanActivity` (CameraX + MLKit, already in the
  dependency tree) that scans and returns the value without any manual typing.
- **API tokens cannot be revoked** — the Settings screen previously showed only a token
  count. Replaced it with a `RecyclerView` listing each token's name, creation date and
  last-used date, each with a **Revoke** button backed by a confirmation dialog.

### Added
- `BarcodeScanActivity` — full-screen CameraX preview with MLKit barcode analysis;
  auto-closes and returns the raw value to the edit form on first successful scan.
- `TokenAdapter` + `item_token.xml` — `ListAdapter`-backed token rows for the Settings
  screen; supports live list diffing when tokens are revoked.
- `activity_barcode_scan.xml` — barcode scanner layout with `PreviewView` and
  instructional overlay label.
- `READ_MEDIA_IMAGES` permission (API 33+) and `READ_EXTERNAL_STORAGE` (≤ API 32)
  for the system image gallery picker.
- `androidx.recyclerview:recyclerview:1.3.2` as an explicit dependency (was previously
  only transitive).
- `BarcodeScanActivity` — full-screen CameraX barcode scanner using MLKit Barcode
  Scanning; returns the raw barcode value to the calling fragment.
- `TokenAdapter` + `item_token.xml` — per-token list rows in the Settings screen.
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STOR  

### Changed
- `EditWhiskyViewModel` — replaced boolean `_saved: LiveData<Boolean>` with
  `_savedId: LiveData<Int?>` so the fragment receives the whisky ID on success and can
  run photo uploads/deletes before navigating back.
- `EditWhiskyFragment` — now waits for `savedId` before processing the photo queue,
  then calls `popBackStack()`. Previously it navigated away immediately, making photo
  operations unreachable.

---

## [0.0.4] — 2026-05-05 🔧 It Actually Works Now

### Fixed
- **Empty collection screen** — `RecyclerView` was missing a `LayoutManager`. Android was quietly showing nothing. It now shows your whiskies.
- **"RetrofitClient not initialised" crash** — when Android killed and restored the app process, it bypassed the login screen entirely, leaving the API client uninitialised. Added `WhiskyWiseApp` to initialise it at process start regardless of which screen Android opens first.
- **Camera cutout / notch overlap** — content was rendering behind the front camera on punch-hole and notch devices. Status bar is now transparent and the layout respects system window insets.
- **White circle launcher icon** — the app icon appeared as a bottle inside a white circle on Android 8+ adaptive icon launchers. Added proper adaptive icon definitions with the correct dark background.

---

## [0.0.3] — 2026-05-04 🔧 Build & Code Quality

### Fixed
- **Deprecated toolbar menu API** — replaced `setHasOptionsMenu` / `onCreateOptionsMenu` / `onOptionsItemSelected` in `DetailFragment` with the modern `MenuProvider` API, eliminating the Kotlin deprecation warning.
- **Gradle 10 compatibility warning** — added `android.suppressUnsupportedCompileSdk=35` to `gradle.properties` to silence the "deprecated Gradle features" warning ahead of the Gradle 10 migration.
- **MLKit native library strip warning** — added `packaging { jniLibs { keepDebugSymbols } }` to `app/build.gradle` for the MLKit barcode `.so` files, which are pre-stripped by Google and cannot be stripped again by the Android build tools.

### Changed
- **App icon** — replaced the placeholder bottle icon with a custom WhiskyWise icon: a classic single malt bottle with a cream label, red wax cap, and WW serif monogram. Generated at all five mipmap densities (mdpi → xxxhdpi).

---

## [0.0.2] — 2026-05-04 🐛 Hotfix

### Fixed
- **App crash on launch** — `SingleResponse<T>` generic wrapper caused a `Class cannot be cast to ParameterizedType` error at runtime due to Kotlin/Java type erasure. Replaced with concrete response classes (`WhiskyResponse`, `WhiskyListResponse`, `TokenDataResponse`, etc.) that Gson can deserialize without reflection issues.

---

## [0.0.1] — 2026-05-03 🚧 Initial Pre-release

### Added

**App**
- **Bearer token login** — enter your WhiskyWise server URL, username and password to obtain a personal API token. The token is stored in `EncryptedSharedPreferences` (AES256-GCM); your password is never saved on the device.
- **Auto-login** — a stored token skips the login screen on subsequent launches.
- **Collection screen** — browse your whisky collection with free-text search, status filter chips (All / Open / Stashed / Retired), pull-to-refresh, and per-row thumbnail, score, distillery and region display.
- **Whisky detail screen** — full view including hero photo, tasting notes (nose, palate, finish, general notes), key metadata (region, age, ABV, status, price, store), dominant flavour, and a native radar chart plotting the seven flavour axes (Woody, Smoky, Cereal, Floral, Fruity, Medicinal, Fiery).
- **Add / Edit form** — create new collection entries or edit existing ones. Supports all API fields; sends only changed fields on update.
- **Wishlist screen** — browse your wishlist with pull-to-refresh. Add items via a quick-add dialog (name + notes).
- **Settings screen** — shows the connected server URL, active token count, app version, and a Log Out button.

**Build & CI**
- **GitHub Actions pipeline** (`android.yml`) — automated build triggered on push, pull request and GitHub Release. Produces a debug APK on every push and a signed APK + AAB on release.
- **Automated versioning** — `versionCode` and `versionName` are derived from the git release tag at build time; no manual version editing required.
- **Issue management workflows** — blank issues are automatically closed with a comment directing users to the issue templates; issues labelled `awaiting release` are automatically closed when a release is published.
- **Issue templates** — bug report and feature request templates to ensure consistent issue quality.

**Dependencies (initial versions)**
- Gradle 9.5.0 / AGP 9.2.0 / Kotlin 2.3.21
- compileSdk / targetSdk 35 (Android 15)
- Retrofit 2.11.0 + OkHttp 4.12.0
- Navigation Component 2.9.0
- Lifecycle ViewModel/LiveData 2.9.0
- Glide 4.16.0
- AndroidX Security Crypto 1.1.0-alpha06
- CameraX 1.4.2 + MLKit Barcode Scanning 17.3.0

### Fixed
- `gradle.properties` missing from project — caused `android.useAndroidX` error on first CI run.
- `fragment_detail.xml` used invalid `app:label` attribute inside `<include>` tags — replaced with inline layout pairs.
- `DetailFragment.kt` used early `return` inside a Kotlin expression body (`= when`) — converted to block body.
- `gradlew` hand-written script had incorrect JVM opts quoting — replaced with `gradle wrapper` generated at CI build time.
- `android.yml` `versionName` sed patch hardcoded `"0.0.1"` — replaced with semver regex so it works for all future versions.

### Notes
- Requires WhiskyWise server ≥ 1.5.0 (introduces the Bearer token API).
- Minimum Android version: API 26 (Android 8.0 Oreo).
- Pre-production — expect breaking changes between releases.
- The `gradlew` and `gradle-wrapper.jar` are not committed; they are generated by the CI pipeline at build time.
- Post-job Gradle cache cleanup warnings (`gradlew exit code 1`) are cosmetic and do not affect build output.
