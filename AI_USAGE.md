# AI Tool Usage

**Tool:** Claude (Anthropic), claude.ai, used in the Part 2 and Part 3 phases of this project.

> **Student: edit this so it matches what you actually did.** The brief requires honest disclosure, and your lecturer may ask you about any line of code.

## How AI was used
1. **Scaffolding (Part 2).** Generated the initial Android/Compose project structure and the ASP.NET Core API skeleton from my Part 1 design (MVVM, Retrofit, Room, JWT, BCrypt).
2. **Feature code (Part 3).** Drafted the offline-sync repository, WorkManager job, FCM service, Google SSO flow, Firestore/Storage repository, gamification engine, the English/isiZulu/Sesotho string resources, and the backend sync, categories and notification controllers.
3. **Testing.** Drafted the unit tests (validator, budget maths, gamification, MockWebServer API tests) and the backend xUnit integration tests.
4. **CI/CD.** Adapted the GitHub Actions workflow from the brief's reference links into `ci.yml` and `release.yml`.
5. **Debugging.** Identified why the Part 2 prototype would fail in CI (missing Gradle wrapper) and at runtime (no categories, un-hosted API).
6. **Images.** The app icon vector, Play Store icon and feature graphic were generated with a script.

## What I did myself
* Product idea, Part 1 research and design decisions.
* Creating the Firebase project, hosting the API, creating the signing keystore, running and debugging the app on my phone, recording the video, and the Play Console steps.
* Reviewing the generated code and translations. The isiZulu and Sesotho text was AI-drafted and **should be checked by a first-language speaker** (replace this line with who checked it).

## Citation
Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai [Accessed 04 November 2026].

*(Word count: about 270)*
