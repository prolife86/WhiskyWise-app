# Changelog

All notable changes to the WhiskyWise Android app are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The app connects to a self-hosted [WhiskyWise](https://github.com/prolife86/WhiskyWise) server.  
See that project's changelog for server-side changes.

---

## [0.3.3] — 2026-06-03 🔣 Right Separator, Right Currency

### Fixed

- **Decimal and thousands separators now respect the server-configured currency** —
  the app was unconditionally formatting all numbers with a decimal comma (e.g.
  `8,5` score, `€129,99`) regardless of which currency the server was set to.
  USD, GBP, AUD, CAD, and JPY now correctly use dot-decimal with comma thousands
  (e.g. `8.5`, `$1,000.00`), while EUR, CHF, SEK, NOK, DKK, and custom currencies
  keep comma-decimal with dot thousands (e.g. `8,5`, `€1.000,00`).

- **All display surfaces updated** — score badges in the collection list,
  detail page (score, ABV, price), wishlist detail (ABV, price), and share cards
  all use the correct separators.

- **Edit form pre-fill corrected** — ABV, score, and price fields in both the
  whisky edit form and the wishlist edit form are now pre-filled with the correct
  locale-aware separators. The `EditWishlistFragment` price field was also
  previously showing a raw dot-decimal float (`129.99`) regardless of locale —
  this is now fixed.

### Technical

- `TokenStore` now persists `currency_code` alongside `currency_symbol`.
  `saveCurrencyCode()` / `getCurrencyCode()` added; defaults to `"EUR"`.
- `MainActivity.refreshCurrencySymbol()` now also saves `currencyCode` from
  `GET /api/v1/stats` every time the app opens.
- `Extensions.kt`: replaced the single `DISPLAY_LOCALE` constant with
  `LOCALE_DOT` (`en-US`) and `LOCALE_COMMA` (`nl-NL`), plus a `localeFor(currencyCode)`
  helper that returns the correct one. All three format functions (`formatScore`,
  `formatAbv`, `formatPrice`) now accept a `currencyCode` parameter (defaults to
  `"EUR"` for backwards compatibility). New `formatForEdit(places, currencyCode)`
  extension added for form input pre-fill (no symbol, no thousands grouping).
- `WhiskyAdapter.setCredentials()` now accepts an optional `currencyCode` parameter
  and passes it to `formatScore()` in `bind()`.

### Notes

- Requires server v1.6.7 or later for correct `currency_code` in the API response.
  On older servers the app falls back to `"EUR"` (comma-decimal) — same behaviour
  as before this release.
- No API changes.

---

## [0.3.2] — 2026-05-20 💱 Your Currency, Your Rules

### Changed

- **Price display now uses the server-configured currency symbol** — previously
  hardcoded to `€` throughout the app. The symbol is read from the server's
  `GET /api/v1/stats` response (`currency_symbol` field, added in server v1.6.6)
  and stored locally in `EncryptedSharedPreferences`. Detail pages, wishlist
  detail, and share cards all reflect the server's choice automatically.
  Defaults to `€` if the server pre-dates v1.6.6.

### Fixed

- **Currency change on the server not reflected in the app** — the symbol was
  only fetched when the Statistics tab was visited. The currency is now fetched
  silently from `/api/v1/stats` every time `MainActivity` starts — on fresh
  launch, after returning from background, and after login. Failures are silent;
  the previously stored symbol remains.

- **Share card shows stale photo after rotation** — the share card loaded the
  front photo with no cache-busting, so Glide served the pre-rotation image from
  disk cache. The URL now appends `?t=<updatedAt>`, matching the strategy already
  used by `loadWhiskyPhoto()`.

- **"Max €" filter hint hardcoded in the collection filter bar** — the price
  filter input showed `Max €` regardless of the configured currency. The hint
  is now set in code from `TokenStore`.

### Technical

- `Models.kt`: added `currencySymbol` and `currencyCode` fields to `Stats`.
- `TokenStore`: added `saveCurrencySymbol()` and `getCurrencySymbol()`.
- `Extensions.kt`: `formatPrice()` accepts an optional `currencySymbol` parameter.
- `MainActivity`: added `refreshCurrencySymbol()` — fire-and-forget coroutine
  that fetches stats and persists the currency symbol on every app open.
- `StatisticsViewModel`: plain `ViewModel` — currency persistence moved to `MainActivity`.
- `DetailFragment`, `WishlistDetailFragment`, `WhiskyShareCard`: pass
  `TokenStore(context).getCurrencySymbol()` to `formatPrice()`.
- `WhiskyShareCard`: photo URL includes `?t=<updatedAt>` cache-buster.
- `CollectionFragment`: `etMaxPrice.hint` set dynamically from `TokenStore`.
- `fragment_collection.xml`: hardcoded `android:hint="Max €"` removed.

### Notes

- Requires server v1.6.6 for the currency to change. Against older servers
  the app defaults to `€` with no visible difference.
- No database changes.

---

## [0.3.1] — 2026-05-18 🔇 Silent Warning

### Fixed

- `Extensions.kt`: replaced deprecated two-argument `Locale("nl", "NL")` constructor
  with `Locale.forLanguageTag("nl-NL")`. Clears the compiler warning introduced in
  Java 19 / Kotlin 1.9+ with no behaviour change — decimal commas remain.

### Notes

- No UI changes. No server changes. No database changes.

---

## [0.3.0] — 2026-05-18 🏪 Store Ready

### Changed

- **targetSdk bumped to 36 (Android 16)** — meets Google Play's mandatory API level
  requirement for all new app submissions and updates from 2025 onward.
- **compileSdk bumped to 36** — consistent with targetSdk.

### Technical

- `app/build.gradle`: `compileSdk` and `targetSdk` updated from 35 → 36.
- `.github/workflows/android.yml`: AAB signing step added using `jarsigner`.
  The AAB is signed with the upload key before being attached to the GitHub
  Release. Google Play App Signing re-signs it with the distribution key for
  delivery to devices — no distribution key is stored in CI.
- AAB signature verification step added after signing.

### Notes

- **Google Play submission format**: submit the signed `.aab` file (not the APK)
  to the Google Play Console. The AAB is the required format for new app listings.
- **Google Play App Signing**: enrol via Play Console → Release → Setup →
  App signing. Google holds the distribution key; you only manage the upload key
  (stored as `KEYSTORE_BASE64` / `KEY_ALIAS` / `KEYSTORE_PASSWORD` / `KEY_PASSWORD`
  in GitHub Actions secrets).
- No code or UI changes in this release.
- No server changes. No database changes.

---

## [0.2.9] — 2026-05-17 📊 Tabs, Stats & Sharing

### Added

- **Statistics tab** — a dedicated fourth tab sits between Wishlist and Settings.
  Shows total bottles in your collection plus a 2×2 card grid of Open / Stashed /
  Finished / Wishlisted counts, and proportional breakdown bars. Numbers reload
  every time you visit the tab. Data comes from the existing `GET /api/v1/stats`
  endpoint — no server changes required.

- **Share card** — tap the share icon on any collection or wishlist detail page
  to generate a JPEG card for that bottle. The card includes the front photo
  (loaded from the server with your auth token), name, distillery, region, age,
  ABV, price, score, and tasting notes. Android's share sheet handles the rest —
  send via WhatsApp, Messages, or anything else. Wishlist cards are labelled
  "MY WISHLIST".

### Changed

- Bottom navigation now has four items: **Collection · Wishlist · Statistics · Settings**.
  Settings retains the gear icon and all its existing content (server URL, tokens,
  sessions, logout).

### Technical

- New `ui/statistics/StatisticsFragment` + `StatisticsViewModel` — mirrors the
  standalone app's statistics screen, wired to `WhiskyWiseRepository.getStats()`.
- New `fragment_statistics.xml` layout — hero total card, 2×2 status cards,
  proportional breakdown bars.
- New `util/WhiskyShareCard` — renders the share card off-screen, writes a JPEG to
  `cacheDir/shares/`, and serves it via `FileProvider`. Photos are loaded via Glide
  with a `Bearer` token header.
- New drawables: `ic_stats.xml` (bar-chart), `ic_share.xml` (share).
- New layouts: `card_share_whisky.xml`, `gradient_photo_fade.xml`, `bg_status_badge.xml`.
- `file_paths.xml` updated: added `<cache-path name="shares" path="shares/" />`.
- `proguard-rules.pro` updated: keep rules for `CardShareWhiskyBinding` and
  `WhiskyShareCard` so R8 does not strip the off-screen inflate path.
- `nav_graph.xml` / `bottom_nav_menu.xml` / `MainActivity` / `strings.xml` updated
  for the new four-tab layout.

### Notes

- No server changes required.
- No database changes.

---

## [0.2.8] — 2026-05-16 🏷 Status Where It Belongs

### Fixed

- **"Stashed" showing on wishlist cards** — the Wishlist screen reuses the same
  adapter as the Collection. Wishlist items default to `stashed` on the server,
  so the status pill was appearing on every wishlist card. Status is a collection
  concept and has no meaning on the wishlist — it is now hidden on wishlist items.

### Technical

- `WhiskyAdapter`: checks `whisky.wishlist` before rendering the status pill —
  `GONE` for wishlist items, `VISIBLE` with status text for collection items.

### Notes

- No server changes required.
- No database changes.

---

## [0.2.7] — 2026-05-15 📸 Wishlist Gets a Face

### Added

- **Cover photo on the Wishlist Edit screen** — tap the photo slot or the 📷 button to
  pick from your gallery or take a new shot. The photo is uploaded as `photo_front` and
  shows on both the Edit and Detail screens.

- **Cover photo on the Wishlist Detail screen** — if a cover photo has been set (from
  the app or the web), it is shown at the top of the detail view.

- **Photo becomes Front Label on promotion** — when a wishlist item is moved to the
  collection the photo is already stored as `photo_front`, so it appears immediately as
  the Front Label in the collection detail view. No re-upload required.

### Technical

- `fragment_edit_wishlist.xml`: added `ivPhotoCover` (ImageView) and `btnPickCover`
  (MaterialButton overlay) in a 120×120 dp FrameLayout before the Save button.
- `fragment_wishlist_detail.xml`: added full-width `ivPhotoCover` (200 dp height,
  hidden when null) at the top of the detail view.
- `EditWishlistFragment`: camera permission request, `TakePicture` launcher, gallery
  launcher, `handlePickedUri()`, `launchCamera()`. Photo uploaded via
  `WhiskyWiseRepository.uploadPhoto()` after item save completes.
- `WishlistDetailFragment`: loads `photoFront` via `loadWhiskyPhoto()` extension.

### Notes

- Requires server v1.6.4 for photo upload to work.
- No database changes.

---

## [0.2.6] — 2026-05-15 🥃 Wishlist Knows More Now

### Added

- **Age and ABV on the Wishlist Detail screen** — previously only visible after tapping
  Edit, Age and ABV are now displayed on the read-only detail view alongside Region,
  Price, Store, Barcode, and Notes.

- **Age and ABV on the Edit Wishlist screen** — the full edit form now includes Age and
  ABV fields, loaded from the server and saved back on submit.

### Technical

- `fragment_wishlist_detail.xml`: added Age and ABV side-by-side row between Region and
  Price/Store.
- `WishlistDetailFragment`: populates `tvAge` and `tvAbv`; ABV formatted with comma
  decimal to match the rest of the app.
- `fragment_edit_wishlist.xml`: added Age and ABV side-by-side row between Region and
  Price/Store.
- `EditWishlistFragment`: sets `etAge` and `etAbv` on load; reads and passes both to
  `WhiskyRequest` on save.

---

## [0.2.5] — 2026-05-15 📅 Sort by When You Last Poured

### Added

- **"Last Tasted" sort option on the Collection screen** — the sort spinner now includes
  Last Tasted ↑ and Last Tasted ↓ alongside the existing Name, Distillery, Price, Score,
  and Updated options. Bottles with no last tasted date sort last. Requires server v1.6.1+.

### Technical

- `sortKeys` in `CollectionFragment`: added `"last_tasted" to "asc"` and
  `"last_tasted" to "desc"` entries.
- `sort_labels` in `strings.xml`: added `Last Tasted ↑` and `Last Tasted ↓` items —
  indices kept in sync with `sortKeys`.

### Notes

- Requires server v1.6.1 or later. On older servers the sort parameter is silently
  ignored and the collection falls back to the default sort.
- No database changes.

---
 
## [0.2.4] — 2026-05-15 🛒 Scan, Know, Decide
 
### Added
 
- **Barcode scan "not found" prompt** — scanning a barcode on the Collection screen
  that isn't in your collection now offers two choices: navigate to Add Whisky with
  the barcode pre-filled, or fall back to a regular text search. Previously the scanner
  just submitted a text search silently with no feedback. Matches the web UI behaviour
  introduced in server v1.5.9.
- **Move to Collection from Wishlist Detail** — a new 🛒 toolbar icon on the Wishlist
  Detail screen opens a status picker (Stashed / Open / Finished). Confirming promotes
  the item to your collection, preserving all existing fields — name, distillery, price,
  notes, barcode, and everything else. Previously there was no way to promote a wishlist
  item from the app; you had to add the bottle manually as a new collection entry.
- **Age and ABV in the wishlist quick-add dialog** — the Add to Wishlist dialog now
  includes Age and ABV fields alongside the existing Name, Distillery, Region, Price,
  Store, Barcode, and Notes fields. Previously Age and ABV were only available through
  the full edit screen after creating the entry, matching the server's wishlist form.
### Fixed
 
- **Build error: `wishlist` field missing from `WhiskyRequest`** — the `promoteToCollection`
  function referenced `wishlist = false` on `WhiskyRequest`, which did not have that field.
  Added `val wishlist: Boolean? = null` to `WhiskyRequest`; existing callers are unaffected.
- **Build error: coroutine `launch` unresolved in `CollectionFragment`** — `kotlinx.coroutines.launch`
  was not imported alongside `lifecycleScope`. Import added; `viewLifecycleOwner.` prefix
  removed from the call site (redundant inside a Fragment).
- **Move to Collection wiped existing fields** — `serializeNulls()` is active on the Gson
  instance, so the promote request was sending every unset field as `null`, overwriting the
  barcode, name, and all other existing data on the server. The promote call now fetches the
  full item first and maps every existing field into the request body before flipping
  `wishlist=false`, preserving all data on promotion.
- **Move to Collection used wrong endpoint** — `promoteToCollection` was calling
  `PUT /api/v1/whisky/{id}` which filters `wishlist=False` server-side and returns 404 for
  wishlist items. Fixed to use `PUT /api/v1/wishlist/{id}` which accepts wishlist items and
  allows flipping the `wishlist` flag.
- **Barcode prefill not delivered to Add Whisky screen** — `prefillBarcode` was passed via
  `bundleOf` but not declared as an argument in `nav_graph.xml`. The Navigation Component
  silently drops undeclared bundle keys, so the barcode arrived as null. Argument declared;
  barcode now pre-fills correctly.
- **Move to Collection icon showed a ➕** — `@android:drawable/ic_menu_add` (a plus in a
  circle) was used for the wishlist promote action. Replaced with `ic_wishlist` (the
  bookmark/star icon already in the app's drawables) which correctly signals the item's
  origin rather than a generic add action.
- **Barcode lookup always fell back to text search** — `/api/barcode-lookup` on the server
  was protected by `@login_required` (session cookies only) instead of `@api_login_required`
  (Bearer token + session). The app sends a Bearer token; the server returned 401; the
  `onFailure` branch silently submitted a text search. Fixed server-side in v1.6.0.
### Technical
 
- `WhiskyWiseApi`: added `barcodeLookup(@Query code)` mapping to `GET api/barcode-lookup`.
- `Models.kt`: added `BarcodeLookupResponse(found, id?, name?)`; added `val wishlist: Boolean? = null`
  to `WhiskyRequest`.
- `WhiskyWiseRepository`: added `barcodeLookup()` and `promoteToCollection()`. The promote
  call fetches the full current item, maps all existing fields into the request body, then
  PUTs to `PUT /api/v1/wishlist/{id}` with `wishlist=false` and the chosen status — ensuring
  no existing data is lost and the correct endpoint is used.
- `WishlistViewModel`: added `promote(id, status, onDone)`.
- `CollectionFragment`: barcode result now calls `handleScannedBarcode()` which runs a
  lookup coroutine before deciding whether to navigate, prompt, or fall back to search.
  Fixed `lifecycleScope` import.
- `EditWhiskyFragment`: reads optional `prefillBarcode` argument and populates `etBarcode`
  on new-entry screens.
- `WishlistDetailFragment`: inflates new `menu_wishlist_detail` (Move to Collection + Edit
  + Delete); wires `showPromoteDialog()` with a single-choice status selector.
- `WishlistFragment`: `showAddDialog()` now reads `etAge` and `etAbv` and passes them to
  `WhiskyRequest`.
- `dialog_add_wishlist.xml`: added Age and ABV as a side-by-side row between Region and Price.
- New `menu_wishlist_detail.xml` menu resource — keeps the promote action off collection
  detail pages, which continue to use the shared `menu_detail.xml`.
### Notes
 
- Requires server v1.6.0 or later for the barcode lookup to work correctly in the app
  (the `/api/barcode-lookup` endpoint was fixed to accept Bearer tokens in v1.6.0).
  All other changes work with any server version.
- No database changes.
  
---

## [0.2.3] — 2026-05-15 🔍 Filter Everything (and Lock the Backdoor)

### Security

- **`android:allowBackup` disabled** — the manifest previously set `allowBackup="true"`,
  which permitted Android and Google Backup to copy the app's data partition (including
  `EncryptedSharedPreferences`) to a cloud or device backup. Although the preferences are
  encrypted, the Keystore key does not transfer — which is exactly the AEADBadTagException
  bug patched in v0.1.1. Disabling backup means the server URL and Bearer token are never
  included in any backup. Users switching phones will be asked to log in again, which is
  the correct behaviour for a credential store.

### Added

- **Infinite scroll on the Collection screen** — the list now loads in pages of 50 as you
  scroll down. When you reach within 8 items of the bottom, the next page is fetched
  automatically and appended. A thin amber progress bar appears at the bottom of the screen
  while a page is loading. Swipe-to-refresh always reloads from the first page. Any filter
  or sort change also resets to page 1. Previously the collection was capped at a hardcoded
  limit of 200 entries with no way to see the rest.

- **Flavour filter** — a second filter row on the Collection screen now includes a Dominant
  Flavour spinner (All / Floral / Smoky / Peaty / …). Previously wired in the API layer
  but absent from the UI.

- **Min Score and Max Price filters** — the same second filter row adds two text inputs:
  Min Score and Max €. Both were already supported by the API; this surfaces them in the
  app for the first time.

- **Last Tasted date field** — a date picker (calendar dialog) is now available on the
  Add / Edit Whisky form. The selected date is shown on the Whisky Detail page and on the
  Wishlist Detail page if previously set on the web. Requires server v1.5.9.

### Changed

- **"Inc. retired" checkbox is now checked by default** — all bottles including retired
  ones are shown when opening the app. Uncheck to hide retired bottles. Previously the
  checkbox opened unchecked, hiding retired bottles on first load. Now matches the web UI
  default.

- **Wishlist sort spinner now uses the correct label set** — the sort spinner on the
  Wishlist tab previously used the collection's 10-item sort list (which includes Updated
  ↑/↓). The wishlist API has no "updated" sort, so selecting those items would silently
  send an unrecognised parameter. The wishlist now has its own 8-item list.

### Technical

- `CollectionViewModel`: replaced single `load()` with `load()` (reset + first page) and
  `loadMore()` (append next page). Added `_isLoadingMore` LiveData, `pageSize = 50`,
  `currentOffset`, and `allLoaded` guard. Added `currentFlavor`, `currentMinScore`,
  `currentMaxPrice`; `showRetired` default changed to `true`.
- `CollectionFragment`: added `RecyclerView.OnScrollListener` triggering `vm.loadMore()`
  when within 8 items of the bottom. Removed Snackbar truncation warning. Wired flavour
  spinner, min score and max price inputs.
- `WhiskyWiseRepository.getCollection()`: `minScore` and `maxPrice` now pass through from
  the ViewModel instead of being hardcoded to `null`.
- `Models.kt`: `last_tasted` added to `Whisky` and `WhiskyRequest`; `finished` field added
  to `Stats` (default 0 for backwards compatibility with servers older than v1.5.9).
- `strings.xml`: added `flavor_filter_labels` and `wishlist_sort_labels` arrays.
- `fragment_collection.xml`: added `loadingMoreBar` (`ProgressBar`, indeterminate, amber
  tint) anchored above the FAB.

### Notes

- Requires server v1.5.9 or later for Last Tasted to persist. All other changes work with
  any server version.
- No breaking changes.

---

## [0.2.2] — 2026-05-13 🚩 Retired and Proud

### Changed

- **Retired badge moved to the top of the detail page** — the "Retired" label
  now appears as a red pill between the whisky name and the score, making it
  immediately visible without having to scroll. Previously it was a small amber
  text below the Status field. The Store field is no longer displaced.

### Notes

- No server changes required.

---

## [0.2.1] — 2026-05-13 📅 Date Detectives

### Added

- **Added and Updated dates on the detail page** — the bottom of every whisky
  detail screen now shows two date fields side by side: *Added* (when the entry
  was first created) and *Updated* (when it was last changed). Dates are
  formatted as `07 May 2026`. Both values were already returned by the API;
  they just weren't displayed.

### Fixed

- **ABV, Price, and Score input fields now accept decimal commas** — fields were
  using `inputType="numberDecimal"` which on many devices only allows a dot.
  All three are now `inputType="text"` restricted to digits and `.,` so both
  `8,5` and `8.5` are accepted. Pre-filled values when editing now show with a
  comma, consistent with the detail and collection views.

### Notes

- No server changes required. Works with any server version that returns
  `created_at` and `updated_at` in the whisky detail response (all versions
  since v1.5.0).

---

## [0.2.0] — 2026-05-13 🔢 Comma Sense

### Added

- **Retired filter** — a checkbox in the filter bar lets you include or exclude
  retired bottles. Unchecked by default (retired bottles hidden); tick it to
  show the full collection including retired entries. Passes `retired=no` or
  omits the parameter to the server accordingly. Requires server ≥ v1.5.8.

- **"Updated" sort option** — two new entries at the bottom of the sort spinner:
  *Updated ↑* (oldest change first) and *Updated ↓* (most recently changed
  first). Requires server ≥ v1.5.8.

### Changed

- **Sort order: Name now listed before Distillery** in the sort spinner. Name
  is the more common sort anchor; Distillery is immediately below it.

- **Filter bar redesigned** — the status chips, sort spinner, and new retired
  checkbox now share a single compact horizontal row instead of stacking
  vertically. The chips scroll horizontally if needed; the spinner and checkbox
  are fixed on the right.

- **Decimal commas guaranteed on all devices** — ABV, Price, and Score are now
  formatted with an explicit Dutch locale (`nl_NL`) instead of relying on the
  device's system locale. Users on a phone set to English no longer see `8.5`
  while users on Dutch see `8,5` — it's always `8,5`. The database and API
  continue to use dot-decimal; this is a display-only change.

### Notes

- No model or API changes beyond the new `retired` query parameter.
- Requires server ≥ v1.5.8 for the retired filter and updated sort to work;
  the app degrades gracefully against older servers (retired checkbox has no
  effect; updated sort falls back to server default).

---

## [0.1.10] — 2026-05-11 🗂 Sort Yourself Out

### Added

- **Sort spinner on Collection and Wishlist** — a dropdown at the top of both
  views lets you order bottles by Distillery, Name, Price, or Score, each
  available ascending (↑) or descending (↓). Collection defaults to Score ↓;
  Wishlist defaults to Distillery ↑. Selection survives swipe-to-refresh and
  resume. Requires server ≥ v1.5.7.

---

## [0.1.9] — 2026-05-10 👁 The Watchful Pour

### Added

- **`X-Client-Version` header on every request** — the app now sends its version
  string (e.g. `0.1.9`) as `X-Client-Version` with every API call. The server
  (≥ v1.5.5) reads this header and stores it against the token, so admins and
  users can see exactly which app version is behind each active API token without
  any extra calls.

- **Browser session list in Settings** — a new *Browser Sessions* section
  appears above the existing API Tokens panel. It shows all active web-browser
  logins for your account: IP address, server version, User-Agent, login time,
  and last-seen time. Any session you don't recognise can be revoked with one
  tap.

- **Richer token metadata** — the API Tokens list now shows the origin IP and
  client version alongside the existing created / last-used timestamps.

### Changed

- `SettingsFragment` now loads both tokens and sessions on open (`loadAll()`).
- `SettingsViewModel` exposes a `sessions` `LiveData` and matching
  `loadSessions()` / `revokeSession()` methods.

### Technical

- New `ApiSession` model + `SessionListResponse` wrapper in `Models.kt`.
- `WhiskyWiseApi` gains `listSessions()` (`GET /api/auth/sessions`) and
  `revokeSession()` (`DELETE /api/auth/session/{id}`).
- `WhiskyWiseRepository` exposes the two new calls via the same `safeCall`
  wrapper used by the rest of the API.
- New `SessionAdapter` RecyclerView adapter; current session row is labelled
  *"This session"* and its Revoke button is disabled so you can't boot yourself.
- `item_session.xml` row layout added; `fragment_settings.xml` gains the
  Browser Sessions header and `rvSessions` RecyclerView.
- `TokenListItem` extended with `originIp` and `clientVersion` fields.

> **Server requirement:** session listing and the `X-Client-Version` header
> require WhiskyWise server ≥ v1.5.5.

---

## [0.1.8] — 2026-05-09 🗂️ The Wishlist Detail Update

### Fixed

- **Wishlist items now open a dedicated detail screen** — tapping a wishlist entry
  previously opened the collection detail screen, which displays tasting notes
  (Nose, Palate, Finish), Score, Status, Radar chart, and Photos — none of which
  exist for a bottle you haven't bought yet. Wishlist items now navigate to a
  purpose-built `WishlistDetailFragment` showing only the seven fields that mirror
  the server's `wishlist_form.html`: Name, Distillery, Region, Price, Store,
  Barcode, and Wishlist Notes. The edit (pencil) and delete (trash) toolbar
  actions work as before.

---

## [0.1.7] — 2026-05-09 🍷 The Flavour Update

### Added

- **Barcode search on the collection screen** — a barcode icon next to the
  search bar opens the scanner. Scan a bottle and the barcode drops straight
  into the search field. Same camera activity, same permission flow as
  everywhere else in the app.

### Fixed

- **Dominant flavour is now editable** — the field existed on the server and was
  displayed on the detail screen, but the edit form silently passed back whatever
  value was already stored. There was no way to set or change it from the app.
  A spinner now appears below the Status field with all 16 options from the
  server's `DOMINANT_FLAVOURS` list (Floral, Fresh, Fruity, Malty, Medicinal,
  Oily, Peaty, Smoky, Spicy, Sweet, Vanilla, Vegetative, Woody, Mixed,
  Undefinable, Complicated). Selecting "— Select dominant flavour —" clears the
  field on the server.

- **Rotated photos now show correctly in the app** — the server rotates photos
  in-place, so the filename and URL never change after rotation. Glide's cache
  key is the URL, meaning it served the stale pre-rotation image indefinitely.
  Photo URLs now include the whisky's `updated_at` timestamp as a `?t=` query
  parameter. The server already bumps `updated_at` on every rotation, so the
  cache key changes automatically — no manual cache-skip needed.

- **Full-screen photo no longer uses deprecated API** — `FLAG_FULLSCREEN` was
  deprecated in API 30. Replaced with `WindowInsetsControllerCompat` +
  `WindowCompat.setDecorFitsSystemWindows()`, which works correctly from our
  minSdk (26) upward with no version branching.

- **Radar chart labels no longer clip on tablets** — label text size and
  placement offset were fixed pixel values (`28px` and `42px`), which looked
  fine on phones but caused "Medicinal" to render as "dicinal" and "Cereal" as
  "Cere" on an 11" tablet. Both values are now derived from the chart radius
  (`r × 0.18` and `r × 0.28`), scaling correctly at any screen size. The chart
  itself is slightly smaller (`0.55r` vs `0.65r`) to give labels room to breathe.  

- **Launcher icon no longer clips the red cap** — the wax cap was sitting
  outside the adaptive icon safe zone (centre 66% of the 108dp canvas), causing
  every launcher with a circular or squircle mask to cut it off. The icons are
  now rendered directly from the vector SVG source at each density, with the
  artwork scaled to 62% of the canvas and centred with a small downward nudge.
  Sharp at every size, full bottle visible on all launchers.

- **CI: `softprops/action-gh-release` bumped to v3** — v2 ran on Node 20 which
  is being removed from GitHub Actions runners on September 16th 2026. v3 targets
  Node 24 and silences the deprecation warning.

---

## [0.1.6] — 2026-05-07 📸 The Photo & Wishlist Update

### Added

- **Swipeable photo gallery on the detail screen** — the single front-photo hero
  image is replaced with a full-width `ViewPager2` pager. Front, back, and cask
  photos (whichever exist) can be swiped through left/right. Amber dot indicators
  overlay the bottom of the pager and track the current page; dots are hidden
  when a whisky has only one photo. The entire photo area collapses when no
  photos exist, keeping the layout clean for photo-free entries.

- **Delete button in the toolbar** — the delete action was buried in the 3-dot
  overflow menu, requiring two taps to reach. It is now a trash icon in the
  toolbar alongside the edit button (`showAsAction="always"`). The confirmation
  dialog still fires before anything is deleted. The 3-dot menu is gone entirely.

- **Barcode scanner on wishlist add and edit** — the barcode field in both the
  add dialog and the edit screen now has a Scan button, launching the same
  `BarcodeScanActivity` (CameraX + MLKit) used by the collection form. Camera
  permission is requested by the activity itself on launch — identical behaviour
  to the collection scanner.

### Fixed

- **Wishlist add form now matches the server** — the add dialog previously showed
  only Name and Notes. It now exposes all fields from the server's
  `wishlist_form.html`: Name, Distillery, Region, Expected Price, Store / Where
  to Buy, Barcode, and Wishlist Notes.

- **Editing a wishlist item no longer shows tasting notes** — tapping Edit on a
  wishlist item previously opened the full collection edit form, complete with
  Nose, Palate, Finish, score sliders, radar chart, status and photos — none of
  which make sense for a bottle you haven't opened. Wishlist items now open a
  dedicated `EditWishlistFragment` with only the seven wishlist-appropriate
  fields, matching the server form exactly.

- **Wishlist edit screen has a delete button** — the only way to delete a
  wishlist item was to navigate back to the detail screen. A trash icon now
  appears in the toolbar of the wishlist edit screen, with the same confirmation
  dialog as everywhere else.

---

## [0.1.5] — 2026-05-07 🌐 The Network Update

### Fixed

- **Clearing a field now actually saves** — Gson omits `null` fields from the
  request body by default, so wiping distillery, region, notes, score, or any
  other optional field had no effect — the old value quietly survived on the
  server. `RetrofitClient` now builds Gson with `serializeNulls()`, so `null`
  fields are included in the body and the server clears them correctly. Requires
  server ≥ 1.5.4.

- **App no longer hangs when switching from WiFi to 5G** — OkHttp's default
  connection pool keeps sockets alive for 5 minutes. When the network changed,
  those sockets became dead but were still handed out for new requests, which
  then blocked until the full 30-second read timeout expired. The connection pool
  is now configured with a 30-second keepalive, so stale sockets are evicted
  promptly after a network switch. `retryOnConnectionFailure(true)` is also
  enabled so the rare mid-flight request during a switch retries transparently.

- **Delete (Remove) button text on the edit screen is now readable** — the
  Remove buttons on photo slots used `OutlinedButton` style, which inherits a
  dark text colour from the theme. Against the button's background tint the text
  was near-invisible. Text and stroke colour are now explicitly white.

- **CI: `close-issues-on-release` workflow updated to Node 24** — the
  `gcampbell-msft/fixed-pending-release` action was pinned to a Node 20 commit
  with no Node 24 update available. Replaced with a native `actions/github-script@v7`
  implementation that does the same job and needs no runtime updates.

---

## [0.1.4] — 2026-05-06 🔧 The Plumbing Update

### Fixed

- **FAB actually floats now** — in both the Collection and Wishlist screens the
  Floating Action Button was a child of a vertical `LinearLayout`, where
  `layout_gravity="bottom|end"` has no effect. It rendered below the list
  instead of floating over it. Both layouts now wrap the `SwipeRefreshLayout`
  and FAB in a `FrameLayout` so gravity works as intended.

- **Filter chips reflect the active selection** — tapping a status chip (All /
  Open / Stashed / Finished) applied the filter but never visually checked the
  chip. The selected chip now shows as checked; the others are cleared.

- **`TokenStore` no longer created on every list-item bind** — constructing
  `EncryptedSharedPreferences` involves Keystore I/O and is expensive enough to
  cause scroll jank. `WhiskyAdapter` now exposes `setCredentials()` and stores
  the server URL and token once at setup time; `bind()` uses the cached values.

- **Photo rotate error no longer leaks a stuck spinner** — `rotatePhoto` was
  called directly in `EditWhiskyFragment` against its own `Repository` instance.
  If the screen rotated mid-request, the fragment was destroyed and the progress
  bar visibility was never reset. The call is now inside `EditWhiskyViewModel`
  alongside all other loading state, so rotation is safe.

- **Photo upload / delete errors are now visible before navigating away** —
  `processPhotos` previously called `onDone()` (which pops the back stack)
  unconditionally, meaning any upload error was posted to a LiveData that the
  about-to-be-destroyed fragment would never observe. Errors now show in a
  Snackbar before the screen pops; on full success navigation happens immediately.

- **`SettingsViewModel` error re-shown on screen rotation** — the error
  `LiveData` was never cleared after display, so rotating the screen re-delivered
  the same Snackbar. `SettingsViewModel` now has a `clearError()` method, called
  by `SettingsFragment` immediately after showing the message.

### Changed

- **`CollectionViewModel.delete()` removed** — the method was dead code; deletion
  is handled exclusively through `DetailViewModel`. Removing it avoids confusion
  about which ViewModel owns the delete operation.

---

## [0.1.3] — 2026-05-06 📦 Status: Corrected

### Fixed
- **Wrong status values** — the app used `stashed / open / retired` for the status
  spinner, but the server uses `stashed / open / finished`. Any whisky saved with
  status "Retired" was being stored as an unrecognised string. Status values now
  match the server exactly.
- **Retired flag missing from edit form** — `retired` is a separate boolean field on
  the server (distinct from `status`), used to mark a whisky that is no longer in
  production or available in shops. It was never exposed in the app. A **Retired**
  checkbox is now present in the edit form, with a description label, and pre-fills
  correctly when editing an existing entry.
- **Retired badge missing from detail screen** — a small amber RETIRED badge now
  appears below the status field on the detail screen when `retired = true`.
- **"Finished" filter chip missing from collection** — the filter chip previously
  labelled "Retired" now correctly filters by `finished` status.

---

## [0.1.2] — 2026-05-06 🔄 The Rotation Was There All Along

### Fixed
- **Rotate button updates the server but not the screen** — Glide caches images by
  URL. The server rotate endpoint changes the image content but not the filename, so
  the URL stays the same. Glide found the pre-rotation image in its disk cache,
  served it immediately, and never asked the server for the updated version. Fixed:
  `loadWhiskyPhoto()` gains a `skipCache` flag; when a rotate completes, the affected
  slot is reloaded with `DiskCacheStrategy.NONE` so Glide fetches the rotated image
  fresh from the server. All other loads continue to use the cache normally.

  ---

## [0.1.1] — 2026-05-06 🔄 First Day Patch

### Fixed
- **App crashes on first launch** — Google Backup automatically restores encrypted
  preferences from a previous install during reinstallation. The Keystore key from
  the old install no longer exists, so `EncryptedSharedPreferences` throws
  `AEADBadTagException` before the app can even show a screen. `TokenStore` now
  catches this, wipes the corrupted preferences file and stale Keystore entry, and
  recreates clean storage. The user is asked to log in again — which is correct
  behaviour after a reinstall.
- **Rotate button does nothing** — the server was rotating the photo correctly, but
  the reload logic immediately overwrote the ImageView with a placeholder by passing
  a fake cache-busting string to Glide instead of a real photo path. Fixed: Glide's
  memory cache is cleared for the slot first, then `vm.load()` is called — the
  existing observer reloads the image with the real path, and Glide fetches the
  rotated version fresh from the server.

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
