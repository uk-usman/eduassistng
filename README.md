# EduAssist NG — Android app (Kotlin + Jetpack Compose)

Native Android client for the EduAssist NG customer API (`/api/v1/customer`).
Package: `com.circleworld.eduassist` · min SDK 24 · target SDK 35.

> No Android Studio? Read **ANDROID_GUIDE.md** to build the APK in the cloud with GitHub (free).

## Open it
1. Install **Android Studio** (current stable release).
2. **File → Open** → choose this folder (`eduassist-android`). Wait for *Gradle sync* (first sync downloads Gradle 8.9 and libraries — needs internet).
3. If Studio offers to upgrade the Android Gradle Plugin, you can accept; the code uses only stable APIs.

## Point it at your server
Edit `app/build.gradle.kts`:
- `debug` → `BASE_URL` (default `http://10.0.2.2:8080` = your computer's localhost from the Android emulator).
- `release` → `BASE_URL` is already `https://eduassist.site.je`.

## Run
Pick an emulator/phone → ▶ Run. Sign in or create an account (the email code is sent by your server).

## Build the APK for sharing
**Build → Generate Signed App Bundle / APK → APK → create a new keystore → release.**
Keep the keystore and its passwords safe: you need the same key for every update.
The file ends up in `app/release/app-release.apk`. For Google Play choose *Android App Bundle (.aab)*.

## What is inside
- `data/Api.kt` — HTTP client (OkHttp), token storage, errors
- `data/Models.kt` — JSON → Kotlin models
- `ui/` — Compose screens: Auth, Home, Requests, Request detail (+ payment), New request, Payments, Notifications, Profile, Support
- Payment opens the gateway checkout in a Chrome Custom Tab; when the user returns, the app asks the server to verify the payment.

## Adding features
Each endpoint is documented in your admin dashboard → **Documentation**. Add a call with `Api.data("GET", "/path")`, map it in `Models.kt`, and build a screen in `ui/`.

## Not included yet
Biometric/PIN lock. (Document upload and Firebase push alerts are included.) This code was written without access to the Android SDK, so the first sync/build may need small fixes.
