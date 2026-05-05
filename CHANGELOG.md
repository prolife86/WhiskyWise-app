# Changelog

All notable changes to the WhiskyWise Android app are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The app connects to a self-hosted [WhiskyWise](https://github.com/prolife86/WhiskyWise) server.  
See that project's changelog for server-side changes.

---

## [Unreleased]

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
