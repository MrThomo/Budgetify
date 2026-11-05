# Setup checklist — the parts only YOU can do

Code is finished; these steps connect it to your own accounts. Do them in order (about 2–3 hours the first time).

## 0. Put the project on GitHub (no zip files!)
```bash
cd Budgetify
git init && git add . && git commit -m "Final PoE: Part 3 features"
git branch -M main
git remote add origin https://github.com/<you>/Budgetify.git
git push -u origin main
```
If you already have a repo from Part 2, copy these files over it instead and commit in several small commits (markers look at commit history).

**Gradle wrapper:** if your repo has `gradlew` and `gradle/wrapper/gradle-wrapper.jar`, keep them. If not, run `gradle wrapper --gradle-version 8.13` inside `android-app/` once and commit the result. CI works either way.

## 1. Firebase project (free Spark plan)
1. <https://console.firebase.google.com> → **Add project** → "Budgetify".
2. **Add Android app**: package name `com.budgetify.app`. Add your debug **SHA-1** (`gradlew signingReport`, or Android Studio → Gradle → `signingReport`) **and later your release keystore SHA-1**.
3. Download **`google-services.json`** → put in `android-app/app/` and commit it.
4. **Authentication** → Sign-in method → enable **Google** and **Anonymous**. In the Google provider's *Web SDK configuration* copy the **Web client ID**.
5. Paste that ID into `android-app/app/src/main/res/values/config.xml` (`google_web_client_id`).
6. **Firestore Database** → create (production mode) → Rules → paste `firebase/firestore.rules`.
7. **Storage** → get started → Rules → paste `firebase/storage.rules`.
8. **Project settings → Service accounts → Generate new private key** (JSON). This is for the server only — **never commit it**.

## 2. Host the REST API (required: the video must show a hosted API)

**Fastest: Render (free tier, no Azure setup)**
1. Push the repo to GitHub (step 0). Make a free account at <https://render.com> and connect GitHub.
2. **New → Blueprint** → choose your repo. Render reads `render.yaml` in the repo root and builds the Docker image (about 5–10 minutes the first time).
3. When asked for `Google__WebClientId` and `Firebase__CredentialsJson`, paste the Web client ID (step 1.4) and the whole service-account JSON (step 1.8). You can leave them blank for now and add them later under **Environment**.
4. Open the URL Render gives you (like `https://budgetify-api-xxxx.onrender.com`). You should see `{"status":"Budgetify API running"}`; `/swagger` shows the API docs.
5. Free instances sleep when idle and the first request takes ~50 s. **The database also resets when it sleeps**, so record your video in one sitting. For data that survives, use Azure below.

**Alternative: Azure App Service** (Azure for Students)
1. Create a Web App: Runtime **.NET 8**, Linux. Deploy `backend/Budgetify.Api` (VS → Publish, or `az webapp up`).
2. Configuration → Application settings: `Jwt__Key` (32+ random characters), `ConnectionStrings__DefaultConnection` = `Data Source=/home/budgetify.db`, `Google__WebClientId`, `Firebase__CredentialsJson`, `Swagger__Enabled` = `true`.

## 3. Point the app at your API (no rebuild needed)
Open the app → on the **Login** screen tap **Server settings** (small text at the bottom) → paste your API address (for example `https://budgetify-api-xxxx.onrender.com`) → **Save and test**. It must say *Connected*. The first test after a long idle can take about a minute.

To bake the address into future builds, also set `BUDGETIFY_API_URL` in `android-app/gradle.properties`.

## 4. Run on your phone
Android Studio → enable USB debugging on the phone → run the **debug** build. Test: register → add expense → airplane mode → add another → reconnect (it syncs) → Settings → Send test notification.
First request after idle can take ~30 s on free hosting — wake the API before recording.

## 5. Signing key + GitHub secrets
```bash
keytool -genkeypair -v -keystore budgetify-release.jks -alias budgetify -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 budgetify-release.jks      # macOS: base64 -i budgetify-release.jks
```
GitHub repo → Settings → Secrets and variables → Actions → add `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. **Back the .jks up somewhere safe; never commit it.**
For local signed builds create `android-app/keystore.properties` (git-ignored) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

## 6. Final submission steps
1. Check the **Actions** tab is green (fix anything red).
2. `git tag final-PoE && git push origin final-PoE` — this tag also triggers the release workflow, which attaches the **signed APK/AAB** to a GitHub Release.
3. Take phone screenshots → `docs/images/` (names used in the README).
4. Record the video (see `docs/DEMO_VIDEO_SCRIPT.md`), upload to YouTube as **Unlisted**, paste the link at the top of `README.md`.
5. Play Console (one-off US$25 developer fee): create app → upload the AAB to **Internal testing** → screenshot the upload page for evidence. Use `store-listing/`. You also need a privacy-policy URL.
6. Edit `AI_USAGE.md` so it is true, and have an isiZulu / Sesotho speaker check the translations.
