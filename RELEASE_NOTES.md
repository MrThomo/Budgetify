# Budgetify 2.0.0 — Final PoE release

Changes since the **1.0-prototype** (Part 2).

## ⭐ Innovative features added in this version
1. **Offline-first sync** — expenses are saved to RoomDB with no connection and synced automatically by WorkManager (and instantly when the phone reconnects). The sync endpoint is idempotent, so retries never create duplicates.
2. **Gamified budgeting** — points per expense (+bonus for receipts), day streaks and six badges, backed up to Cloud Firestore.
3. **Local-language support** — English, isiZulu and Sesotho across the whole UI, error messages, category names and push notifications.

## New
* **Single sign-on** with Google (Credential Manager → server-verified token → Firebase Auth).
* **Real-time push notifications** (Firebase Cloud Messaging): the server alerts you at 80 % and 100 % of a category budget; Settings has a "Send test notification" button.
* **Categories and budgets**: seeded defaults, add categories, edit monthly limits, progress bars.
* **Add-expense screen** with validated amount, category, note, date picker and **receipt photo** (stored in Firebase Storage — blob storage).
* **Dashboard**: monthly spend, points, streak, pending-sync counter, offline banner, "Sync now".
* **Rewards screen** reading badges back from Firestore (NoSQL read).
* New launcher icon, notification icon and Play Store graphics.

## Improved
* Settings now actually apply: dark theme switches live, language switches the whole app, currency drives every amount; changes made offline are queued and pushed later.
* Settings use dropdown / switches instead of free text, so invalid values cannot be entered.
* Expired login tokens (HTTP 401) now end the session cleanly and return the user to Login with a translated message instead of silently failing.
* Password rules strengthened (8+ characters, letter + digit); emails normalised.
* Release builds are HTTPS-only (cleartext only in debug); `allowBackup` disabled so encrypted token storage cannot be corrupted by restores.
* Unit tests now test the real production code (previously they tested copied logic); added REST-client tests with MockWebServer and a backend integration suite.
* GitHub Actions now builds and tests both the Android app and the API, and a release workflow produces signed APK/AAB.

## Fixed
* CI previously failed because the Gradle wrapper script was missing — CI now installs Gradle explicitly.
* Server rejected offline expenses whose category did not exist (database foreign key); categories are now seeded and validated, and rejected rows are reported back to the phone.
* Sync previously had no duplicate protection.

## Technical
* `targetSdk` / `compileSdk` 36 (Google Play requirement), AGP 8.11, Kotlin 2.0.21, Compose BOM 2025.04.
* Database: SQLite or Azure SQL (chosen by connection string).

## Publication evidence (add before submitting)
- [ ] Signed APK (from the GitHub Release or `assembleRelease`)
- [ ] Phone screenshots in `docs/images/`
- [ ] Screenshot of the Play Console upload / internal-testing release
