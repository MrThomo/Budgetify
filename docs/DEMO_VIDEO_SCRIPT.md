# Demo video script (screen-record your phone, talk over it, ~7–9 min)

Say what you are doing and **why** — the brief marks the voice-over.

1. **Intro (20 s)** — app name, purpose, tech stack.
2. **Cloud side first (60 s)** — show Azure/host URL + Swagger; Firebase console: Authentication (users), Firestore (empty), Storage. Say "I'll come back to these after the demo."
3. **Register** with a weak password → show the validation message. Register properly. Show the **database row: `PasswordHash` is a BCrypt hash**, not the password.
4. **Log out / log in** (wrong password first → error, no crash).
5. **SSO** — Continue with Google. Show the new user in Firebase Authentication.
6. **Settings** — change currency, toggle dark theme (live), switch language to **isiZulu** then **Sesotho** (whole app changes), save. Show the settings row on the API.
7. **Budgets** — edit a limit, add a category.
8. **Add expense** with receipt photo; try invalid amount ("abc") → validation, no crash.
9. **Offline mode** — airplane mode, add 2 expenses ("waiting to sync" badge, offline banner). Turn data back on → pending count drops to zero. Show the rows now in the API/database (Swagger `GET /api/transactions`).
10. **Notifications** — push an expense over a limit → alert appears. Then Settings → **Send test notification** (FCM). Show it arriving in isiZulu.
11. **Rewards** — points, streak, badges. Show **Firestore** documents `users/<id>/achievements` and **Storage** `receipts/<id>/…` jpg (NoSQL + blob).
12. **GitHub** (60 s) — repo, README, commit history, Actions run green, `final-PoE` tag, release with signed APK.
13. **Wrap-up (15 s)** — what was added since Part 2.
