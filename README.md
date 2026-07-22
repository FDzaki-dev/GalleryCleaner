# Gallery Cleaner (starter project)

A swipe-to-clean gallery app: swipe left to delete, right to keep. Organizes
your photos/videos by month or album, remembers where you left off in each
group, and lets you sort by date, size, or name.

## Why this is source code, not a ready .apk

This was built in a sandbox without the Android SDK / build tools installed
and without internet access to fetch them, so it can't be compiled into an
installable `.apk` here. The good news: turning this into an APK on your own
machine takes about 2 minutes.

## How to build the installable APK

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `GalleryCleaner` folder.
3. Let it sync (it will auto-download Gradle and dependencies the first time —
   this needs internet access once).
4. Click the green **Run ▶** button with a device/emulator connected, **or**
   go to `Build → Build Bundle(s) / APK(s) → Build APK(s)` to just get the
   `.apk` file (it'll show up under `app/build/outputs/apk/debug/`).
5. Copy that `.apk` to your phone and install it (enable "install from
   unknown sources" if prompted).

## What's implemented

- Media permission request (Android 13+ granular, older versions fallback)
- Loads all photos & videos from the device via `MediaStore`
- Group by **Month** or **Album**, sort by **Date / Size / Name**
- Swipe-card UI (drag left = delete, drag right = keep)
- Progress per group is saved (via Jetpack DataStore) so reopening a group
  picks up where you left off
- Batch delete using the proper Android 11+ `MediaStore.createDeleteRequest`
  system confirmation (with a direct-delete fallback for older Android)

## Building it entirely from your phone (via GitHub Actions)

This project already includes `.github/workflows/build.yml`, which builds
the APK in the cloud automatically every time you push. You never need
Android Studio or a computer — GitHub's servers do the compiling.

**Android phone (recommended: Termux app):**
1. Install **Termux** from F-Droid or Play Store.
2. In Termux:
   ```
   pkg install git unzip -y
   cd storage/downloads   # or wherever you saved the zip
   termux-setup-storage   # grant storage access if prompted
   unzip GalleryCleaner.zip
   cd GalleryCleaner
   git init
   git add .
   git commit -m "Initial commit"
   ```
3. On github.com (mobile browser or app), create a new **empty** repository,
   e.g. `GalleryCleaner`. Don't add a README/license there.
4. Create a Personal Access Token: github.com → Settings → Developer settings
   → Personal access tokens → Generate new token (classic), scope `repo`.
   Copy it somewhere safe — you'll use it as the password below.
5. Back in Termux:
   ```
   git remote add origin https://github.com/<your-username>/GalleryCleaner.git
   git branch -M main
   git push -u origin main
   ```
   When prompted for a password, paste the token from step 4 (not your
   GitHub password).
6. On github.com, open your repo → **Actions** tab. You'll see "Build APK"
   running (takes ~3-6 minutes).
7. When it finishes, tap into the run → scroll to **Artifacts** →
   download the artifact (named like `GalleryCleaner-v1.0.42-a1b2c3d.apk`,
   matching this build's version and commit). It's a zip containing one
   `.apk` with that same name.
8. Unzip it on your phone and tap the `.apk` to install (allow "install
   from unknown sources" if asked).

**iPhone:** use the **Working Copy** app instead of Termux — it can unzip,
commit, and push to GitHub directly from its own file browser, with the
same steps 3–4 above for creating the repo/token.

Every time you push a change, GitHub automatically rebuilds the APK for you.

## Ideas for next steps

- App name/icon: currently placeholder ("Gallery Cleaner" + simple icon) —
  change `app_name` in `res/values/strings.xml` and swap
  `res/drawable/ic_launcher.xml` for your own branding
- Random cleanup mode
- A stats/summary screen (photos cleaned, storage freed)
- Move-to-folder while swiping
