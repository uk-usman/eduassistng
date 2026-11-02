# Build the EduAssist NG APK in the cloud (no Android Studio)

You need a free GitHub account. Everything below is clicking, no coding.

## One-time setup
1. Go to github.com → **New repository** → name it `eduassist-android` → **Private** → Create.
2. Unzip `eduassist-android.zip` on your computer. In the new repository click **Add file → Upload files**, and drag in EVERYTHING inside the unzipped folder (including the hidden `.github` folder — if your computer hides it, drag the whole folder contents with "show hidden files" on). Click **Commit changes**.
3. Click the **Actions** tab. If asked, click "I understand my workflows, enable them". The job **Build EduAssist APK** starts by itself (takes about 5–8 minutes).
4. When it shows a green tick, open it and scroll to **Artifacts**. Download **EduAssist-NG-APK** (a zip containing `EduAssist-NG.apk`). Send that APK on WhatsApp or put it on your website.

## IMPORTANT – save your signing key (first build only)
The first build also makes **SAVE-THIS-signing-key** under Artifacts. Download it and keep it somewhere safe (cloud drive + phone). Then:
Repository → **Settings → Secrets and variables → Actions → New repository secret**, add:
- `KEYSTORE_BASE64` = the text inside `KEYSTORE_BASE64.txt`
- `KEYSTORE_PASSWORD` = the text inside `KEYSTORE_PASSWORD.txt`

Why: later builds will use the same key, so people can install updates over the old app. Without the secrets, every build makes a new key and phones will refuse the update.

## Phone alerts (Firebase) – optional but you said yes
1. console.firebase.google.com → **Add project** (name: EduAssist NG; turn Analytics off).
2. In the project: **Add app → Android**. Package name: `com.circleworld.eduassist`. Register, then **download google-services.json**.
3. GitHub secret `GOOGLE_SERVICES_JSON` = open that file in Notepad, copy ALL the text, paste.
4. Firebase → ⚙ **Project settings → Service accounts → Generate new private key**. Download the JSON.
5. Website admin → **Mobile app (APK)** → paste that whole JSON into "Phone alerts" → Save → **Test Firebase connection**.
6. In GitHub → **Actions → Build EduAssist APK → Run workflow** to build again with alerts included. Install the new APK.

## Updating the app later
Change files in GitHub (or upload new ones) → the build runs again → download the new APK. Raise `versionCode` in `app/build.gradle.kts` by 1 each time you release so phones accept the update.

## Sharing
- WhatsApp: send the `.apk`. The person taps it and allows "Install unknown apps" for WhatsApp/Files.
- Website: upload the APK to your site (cPanel File Manager → public_html/downloads/) and link to it with a "Download Android app" button.
