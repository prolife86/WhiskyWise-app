<img src="Icons/Icon 1.png" alt="WhiskyWise" width="80" align="left" style="margin-right:16px"/>

# WhiskyWise Mobile App

A native Android companion app for the [WhiskyWise](https://github.com/prolife86/WhiskyWise) self-hosted spirits tracker.
> This app is in active development. See [CHANGELOG.md](CHANGELOG.md) for the full history.

---

## Features

| Feature | Details |
|---|---|
| 🔐 Secure login | Bearer token stored in EncryptedSharedPreferences (AES256-GCM) |
| 🥃 Collection | Browse, search, filter by status / dominant flavour / min score / max price, sort by name / distillery / price / score / updated, retired filter, infinite scroll |
| 📝 Tasting notes | Full nose / palate / finish fields, last tasted date |
| 📡 Radar chart | Native canvas widget matching the web flavour chart |
| 📸 Photos | Add front / back / cask photos via camera or gallery; rotate saved photos |
| 📋 Wishlist | Browse, add, view and edit wishlist items |
| ⚙️ Settings | Server info, browser sessions + API tokens with per-entry revoke, app version, logout |
| 🌐 Self-hosted | Works with HTTP (local network) and HTTPS |
| 🔖 Barcode | Scan barcodes via camera or enter manually |

---

## Requirements

For running the app (APK install or release build)
- Android 8.0 (API 26) or later
- A running [WhiskyWise](https://github.com/prolife86/WhiskyWise) server ≥ v1.5.9

For development (Android Studio)
- Android Studio Meerkat (2024.3) or later

---

## Installing the APK (without Android Studio)

You can install the app directly using a prebuilt APK without Android Studio.

### 1. Download the APK
Download the latest APK from: [GitHub Releases](https://github.com/prolife86/WhiskyWise-app/releases)

### 2. Allow installation from unknown sources

Android blocks APK installs from outside the Play Store by default. You must enable permission for the app used to open the APK (usually your file manager or browser).

Option A — File Manager (recommended)
1. Open your Files / File Manager app
2. Tap the downloaded .apk file
3. When prompted, enable “Install unknown apps” for your file manager
4. Return and confirm installation

Option B — System Settings
1. Go to Settings → Security / Privacy → Install unknown apps
2. Select your browser or file manager
3. Enable Allow from this source

### 3. Install the APK
1. Open the downloaded .apk file
2.  Tap Install
3.  Wait for installation to complete
4.  Open the app from your launcher

### ⚠️ Notes
- Android may show a warning about installing unknown apps—this is expected behavior.
- You may need to re-enable permissions after updates on some devices (e.g. Samsung, Xiaomi).
- No Android Studio is required to install or use the app.

---

## Setup for development

### 1. Open in Android Studio

1. **File → Open** and select this folder.
2. Let Gradle sync finish — all dependencies are downloaded automatically.
3. The Gradle wrapper is **not committed** to this repo. Android Studio will generate it on first sync, or the CI pipeline generates it at build time via `gradle wrapper --gradle-version 9.5.0`.
4. Connect a device or start an emulator.
5. **Run → Run 'app'**.

### 2. First launch

1. Enter your WhiskyWise server URL — e.g. `http://192.168.1.100:5000` for a local network instance or `https://whiskywise.yourdomain.com` for an internet-facing one.
2. Enter your username and password.
3. The app exchanges your credentials for a Bearer token. Your password is never stored on the device.

---

## Architecture

```
app/
└── src/main/java/com/whiskywise/app/
    ├── WhiskyWiseApp.kt              # Application class — initialises RetrofitClient on startup
    ├── api/
    │   ├── WhiskyWiseApi.kt          # Retrofit interface — all REST endpoints
    │   ├── RetrofitClient.kt         # OkHttp + Bearer token interceptor
    │   └── WhiskyWiseRepository.kt   # suspend fun wrappers → Result<T>
    ├── model/
    │   └── Models.kt                 # Data classes mirroring the API schema
    ├── util/
    │   ├── TokenStore.kt             # EncryptedSharedPreferences wrapper
    │   └── Extensions.kt            # Glide photo loader, number formatters
    └── ui/
        ├── login/LoginActivity
        ├── MainActivity              # Bottom nav host
        ├── collection/               # List, search, filter
        ├── detail/                   # View, edit, RadarView, BarcodeScanActivity, photo upload
        ├── wishlist/                 # Browse, add, wishlist-specific detail + edit
        └── settings/                 # Server info, token list with revoke, logout
```

**Notable resource folders:**
- `res/values/` — base theme, colours, strings
- `res/values-v27/` — theme override for API 27+ (display cutout / notch support)
- `res/xml/` — network security config and FileProvider path declarations (camera capture)
- `res/mipmap-anydpi-v26/` — adaptive icon definitions (eliminates white circle on modern launchers)

---

## API endpoints used

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/auth/token` | Exchange credentials for Bearer token |
| `GET` | `/api/auth/tokens` | List API tokens (with IP + version metadata) |
| `DELETE` | `/api/auth/token/{id}` | Revoke an API token |
| `GET` | `/api/auth/sessions` | List active browser sessions *(requires server ≥ v1.5.5)* |
| `DELETE` | `/api/auth/session/{id}` | Revoke a browser session *(requires server ≥ v1.5.5)* |
| `GET` | `/api/v1/stats` | Dashboard summary |
| `GET` | `/api/v1/collection` | Paginated collection with filters |
| `GET/POST/PUT/DELETE` | `/api/v1/whisky/{id}` | CRUD for collection entries |
| `GET/POST/PUT` | `/api/v1/wishlist` | Wishlist management |
| `POST/DELETE` | `/api/v1/whisky/{id}/photo/{slot}` | Photo upload / delete |
| `POST` | `/api/photo/{id}/{slot}/rotate` | Rotate a saved photo 90° clockwise |
| `GET` | `/api/photo/{filename}` | Authenticated photo serving |

---

## Security

- The Bearer token is stored in Android `EncryptedSharedPreferences` (AES256-GCM).
- Your password is sent only once at login and is never persisted.
- `android:allowBackup` is disabled — the token and server URL are never included in Android or Google backups.
- HTTP is permitted to support local network / Home Assistant setups. Use HTTPS in production.
- The app sends `X-Client-Version` on every request so the server can track which app version is behind each token.
- Tokens and browser sessions can be revoked individually from Settings, or all at once via Log out.

---

## Versioning & CI

Version numbers are never edited by hand. The git release tag is the single source of truth.

### How it works

```
GitHub Release published (e.g. v0.1.0)
        │
        ▼
extract-version job
  strips 'v' prefix → "0.1.0"
  converts to versionCode → 100  (major×10000 + minor×100 + patch)
        │
        ▼
build job
  generates gradlew via: gradle wrapper --gradle-version 9.5.0
  patches app/build.gradle with versionCode + versionName
  runs lint
  builds debug APK        → artifact (always)
  builds release APK + AAB (unsigned) → artifact
        │
        ▼
sign-and-publish job  (release events only)
  decodes keystore from KEYSTORE_BASE64 secret
  signs APK with apksigner
  attaches signed APK + AAB to the GitHub Release
```

### Build outputs per trigger

| Trigger | Debug APK | Signed APK | AAB |
|---|---|---|---|
| Manual (`workflow_dispatch`) | ✅ artifact | — | — |
| GitHub Release | ✅ artifact | ✅ attached to release | ✅ attached to release |

### Creating a release

1. Go to **Releases → Draft a new release**
2. Create a new tag: `v0.1.0`
3. Paste the release notes and click **Publish release**
4. The workflow builds, signs, and attaches the APK and AAB automatically

### One-time setup — signing secrets

The easiest way is to use the included **Generate Keystore** workflow, which runs entirely in GitHub Actions — no local tools needed:

1. Go to **Actions → Generate Keystore (run once) → Run workflow**
2. Enter a key alias, keystore password and key password
3. Copy the base64 string from the **Print encoded keystore** step
4. Add four secrets under **GitHub → Settings → Secrets → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | base64 string from step 3 |
| `KEYSTORE_PASSWORD` | password you entered |
| `KEY_ALIAS` | alias you entered (e.g. `whiskywise`) |
| `KEY_PASSWORD` | key password you entered |

5. Delete the `generate-keystore.yml` workflow from the repo once done
6. Download and keep the `whiskywise-keystore-BACKUP` artifact safe — losing the keystore means you cannot publish updates to the Play Store

> Prefer CLI? `keytool -genkeypair -keystore whiskywise.jks -alias whiskywise -keyalg RSA -keysize 2048 -validity 10000 -storetype JKS`, then `base64 -w 0 whiskywise.jks` (Linux) or `base64 -i whiskywise.jks` (macOS).

### GitHub Actions workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| `android.yml` | Release / manual | Lint, build, sign, publish |
| `close-blank-issues.yml` | Issue opened | Closes issues not using a template |
| `close-issues-on-release.yml` | Release published | Closes issues labelled `awaiting release` |

All actions run on **Node.js 24** and are up to date as of May 2026.

---

## Tech stack

| Library | Version |
|---|---|
| Kotlin | 2.3.21 (built into AGP 9.x — no separate plugin needed) |
| Android Gradle Plugin | 9.2.0 |
| Gradle | 9.5.0 |
| compileSdk / targetSdk | 35 (Android 15) |
| Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Navigation Component | 2.9.0 |
| Lifecycle ViewModel/LiveData | 2.9.0 |
| Glide | 4.16.0 |
| AndroidX Security Crypto | 1.1.0-alpha06 |
| CameraX | 1.4.2 |
| MLKit Barcode Scanning | 17.3.0 |

---

## License

MIT — same as WhiskyWise itself.
