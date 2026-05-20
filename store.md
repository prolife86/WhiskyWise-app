# WhiskyWise — App Store Listing

---

## Short Description
*(max 80 characters)*

Your personal whisky collection, always in your pocket.

---

## Full Description
*(max 4 000 characters — plain text, no markdown)*

WhiskyWise puts your entire whisky collection in your pocket.

Connect to your self-hosted WhiskyWise server and browse, manage, and share every bottle you own — from the rare single cask stashed at the back of the shelf to the everyday dram that's always open on the counter.

**YOUR COLLECTION, ALWAYS WITH YOU**
Browse your full collection from anywhere. Filter by status, dominant flavour, minimum score, or maximum price. Sort by name, distillery, price, score, last updated, or last tasted. Search instantly across all your bottles. Scroll through everything with smooth infinite loading.

**WISHLIST**
Keep track of bottles you want to try. Add notes, price targets, store hints, and a cover photo. When you finally pick one up, move it to your collection in one tap and choose its opening status on the spot.

**STATISTICS**
See your collection at a glance — total bottles, a breakdown of Open, Stashed, and Finished, and how many are waiting on your wishlist. Numbers update every time you visit.

**DETAIL PAGES**
Every bottle gets a full detail view: tasting notes (nose, palate, finish), flavour profile, radar chart, ABV, age, region, score, price, store, barcode, and a swipeable photo gallery with front, back, cask, and barcode shots. Photos always load at the correct orientation, even after rotation.

**SHARE**
Tap the share button on any collection or wishlist bottle to generate a beautifully formatted card — photo included — and send it via WhatsApp, Messages, or anywhere else Android lets you share.

**BARCODE SCANNER**
Scan a barcode to instantly check whether it's in your collection. Not found? The app offers to open a new entry with the barcode pre-filled, or fall back to a text search.

**CURRENCY**
Prices display in whatever currency your server admin has configured — EUR, GBP, USD, and more. The symbol is fetched automatically every time the app opens, so a change on the server is reflected immediately.

**SECURE BY DEFAULT**
Your credentials are stored in Android EncryptedSharedPreferences (AES256-GCM). All communication with your server uses Bearer token authentication. View and revoke individual sessions and tokens directly from the app's Settings screen.

**SELF-HOSTED**
WhiskyWise is a companion app for the open-source WhiskyWise server. You run the server; you own the data. No cloud subscription, no third-party data sharing, no ads.

Requires a running WhiskyWise server instance. See github.com/prolife86/WhiskyWise for setup instructions.

---

## Keywords / Tags

whisky, whiskey, bourbon, scotch, collection, tracker, tasting notes, bottle log, spirits, distillery, wishlist, cellar tracker, self-hosted

---

## Category

Food & Drink

## Content Rating

Everyone (no age-restricted content; the app does not sell or promote alcohol, it is a personal collection tracker)

---

## App Details

| Field | Value |
|---|---|
| Package name | `com.whiskywise.app` |
| Current version | 0.3.4 |
| Minimum Android | 8.0 (API 26) |
| Target Android | 16 (API 36) |
| Build format | Android App Bundle (AAB) |
| App signing | Google Play App Signing |
| Permissions | Camera (barcode scanning), Internet |
| In-app purchases | None |
| Ads | None |
| Data collected | None — all data stays on your own server |

---

## Privacy Policy

WhiskyWise does not collect, store, or transmit any personal data to the app developer or any third party. All data entered by the user is stored exclusively on the user's own self-hosted server. The app communicates only with the server address configured by the user. No analytics, crash reporting SDKs, or advertising frameworks are included.

A privacy policy URL is required by Google Play. Host the text above (or a version of it) at a URL of your choice and enter it in the Play Console.

---

## What's New (v0.3.4)

Prices now display in whatever currency your server admin has configured — EUR, GBP, USD, and more. The symbol is fetched automatically every time the app opens, so a server-side change is picked up immediately. The price filter hint in the collection view updates to match. Share cards also no longer show a stale pre-rotation photo from cache.
