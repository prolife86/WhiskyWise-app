# Changelog

All notable changes to the WhiskyWise Android app are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The app connects to a self-hosted [WhiskyWise](https://github.com/prolife86/WhiskyWise) server.  
See that project's changelog for server-side changes.

---

## [0.0.1] — 2026-05-03 🚧 Initial Pre-release

### Added
- **Bearer token login** — enter your WhiskyWise server URL, username and password to obtain a personal API token. The token is stored in `EncryptedSharedPreferences` (AES256-GCM); your password is never saved on the device.
- **Auto-login** — a stored token skips the login screen on subsequent launches.
- **Collection screen** — browse your whisky collection with free-text search, status filter chips (All / Open / Stashed / Retired), pull-to-refresh, and per-row thumbnail, score, distillery and region display.
- **Whisky detail screen** — full view including hero photo, tasting notes (nose, palate, finish, general notes), key metadata (region, age, ABV, status, price, store), dominant flavour, and a native radar chart plotting the seven flavour axes (Woody, Smoky, Cereal, Floral, Fruity, Medicinal, Fiery).
- **Add / Edit form** — create new collection entries or edit existing ones. Supports all API fields; sends only changed fields on update.
- **Wishlist screen** — browse your wishlist with pull-to-refresh. Add items via a quick-add dialog (name + notes).
- **Settings screen** — shows the connected server URL, active token count, app version, and a Log Out button.
- **GitHub Actions build pipeline** — automated versioning, build and release workflow (`android.yml`) triggered by a GitHub Release tag. Produces a signed APK and AAB attached directly to the release.

### Notes
- Requires WhiskyWise server ≥ 1.5.0 (introduces the Bearer token API).
- Minimum Android version: API 26 (Android 8.0).
- Pre-production — expect breaking changes between releases.
