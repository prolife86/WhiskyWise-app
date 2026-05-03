# WhiskyWise Mobile App

A native Android companion app for the [WhiskyWise](https://github.com/prolife86/WhiskyWise) self-hosted spirits tracker.

### Android (beta)

Get the Android app on the [Google Play Store](https://play.google.com/store/apps/details?id=com.WhiskyWise.app)

## Features

| Feature | Details |
|---|---|
| 🔐 Secure login | Bearer token stored in EncryptedSharedPreferences |
| 🥃 Collection | Browse, search, filter by status, sort by score |
| 📝 Tasting notes | Full nose / palate / finish fields |
| 📡 Radar chart | Native canvas widget matching the web flavour chart |
| ➕ Add / Edit | Full whisky form with all API fields |
| 📋 Wishlist | Browse and add wishlist items |
| 🌐 Self-hosted | Works with HTTP (local network) and HTTPS |

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+ (Android 8.0)
- A running WhiskyWise instance (Home Assistant add-on or standalone Docker)

## Setup

### 1. Get the Gradle wrapper JAR

The `gradle-wrapper.jar` is not included in this source tree (it is a binary).
Run this once from the project root to bootstrap it:

```bash
gradle wrapper --gradle-version 8.4
```

Or simply open the project in Android Studio — it will download everything automatically.

### 2. Open in Android Studio

1. **File → Open** and select this folder.
2. Let Gradle sync finish (it will download all dependencies).
3. Connect a device or start an emulator.
4. **Run → Run 'app'**.

### 3. First launch

1. Enter your WhiskyWise server URL, e.g. `http://192.168.1.100:5000`  
   (or `https://whiskywise.yourdomain.com` for HTTPS setups)
2. Enter your username and password.
3. The app exchanges your credentials for a Bearer token — your password is never stored.

## Architecture

```
app/
└── src/main/java/com/whiskywise/app/
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
        ├── collection/              # List, search, filter
        ├── detail/                  # View, edit, RadarView canvas widget
        ├── wishlist/
        └── settings/                # Server info, token count, logout
```

## API endpoints used

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/auth/token` | Exchange credentials for Bearer token |
| `GET` | `/api/v1/stats` | Dashboard summary |
| `GET` | `/api/v1/collection` | Paginated collection with filters |
| `GET/POST/PUT/DELETE` | `/api/v1/whisky/{id}` | CRUD for collection entries |
| `GET/POST/PUT` | `/api/v1/wishlist` | Wishlist management |
| `POST/DELETE` | `/api/v1/whisky/{id}/photo/{slot}` | Photo upload/delete |
| `GET` | `/api/photo/{filename}` | Authenticated photo serving |

## Security notes

- The Bearer token is stored using Android's `EncryptedSharedPreferences` (AES256-GCM).  
- Your password is sent only once (at login) and is never persisted.  
- HTTP is permitted to support local network setups. Use HTTPS in production.  
- Tokens can be revoked from **Settings → Log out** or from the WhiskyWise web UI.

## Versioning

The Android app uses the **same git tag as the source of truth** as the WhiskyWise Docker workflow — no version numbers are ever edited by hand.

### How it works

```
GitHub Release published (e.g. v1.2.3)
        │
        ▼
extract-version job
  strips 'v' prefix → "1.2.3"
  converts to versionCode → 10203  (major*10000 + minor*100 + patch)
        │
        ▼
build job
  patches app/build.gradle with versionCode + versionName
  runs lint
  builds debug APK  → artifact (always)
  builds release APK + AAB (unsigned) → artifact
        │
        ▼
sign-and-publish job  (release events only)
  decodes keystore from KEYSTORE_BASE64 secret
  signs APK with apksigner
  attaches signed APK + AAB to the GitHub Release
```

### Creating a release

Identical flow to the Docker workflow — just publish a GitHub Release:

1. Go to **Releases → Draft a new release**
2. Create a new tag: `v1.2.3` (same tag triggers both Docker and Android builds)
3. Write release notes and click **Publish release**
4. The workflow builds, signs, and attaches the APK and AAB automatically

### One-time setup: signing secrets

You need a keystore to sign release builds. Create it once and store it as GitHub secrets:

```bash
# 1. Generate keystore (keep whiskywise.jks safe — losing it means you can't update the Play Store app)
keytool -genkey -v \
  -keystore whiskywise.jks \
  -alias whiskywise \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Base64-encode it
base64 -i whiskywise.jks | pbcopy          # macOS (copies to clipboard)
base64 whiskywise.jks                      # Linux (print to terminal)
```

Then add these four secrets in **GitHub → Settings → Secrets → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | base64 output from step 2 |
| `KEYSTORE_PASSWORD` | store password you chose |
| `KEY_ALIAS` | `whiskywise` (or whatever alias you used) |
| `KEY_PASSWORD` | key password you chose |

### Build outputs per trigger

| Trigger | Debug APK | Signed APK | AAB |
|---|---|---|---|
| Pull request | ✅ artifact | — | — |
| Push to main | ✅ artifact | — | — |
| GitHub Release | ✅ artifact | ✅ attached to release | ✅ attached to release |



## License

MIT — same as WhiskyWise itself.
