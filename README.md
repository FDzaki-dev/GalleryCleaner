# Gallery Cleaner

A swipe-to-clean gallery app: swipe left to delete, right to keep. Organizes
your photos/videos by month or album, remembers where you left off in each
group, and lets you sort by date, size, or name.

## 🔗 Download the latest APK

**[github.com/FDzaki-dev/GalleryCleaner/releases/latest](https://github.com/FDzaki-dev/GalleryCleaner/releases/latest)**
 — signed, installable APK,
rebuilt and published automatically every time changes are pushed to
`main`. No Android Studio needed, just download and tap to install (enable
"install from unknown sources" if prompted).

## ✨ What's implemented

- Media permission request (Android 13+ granular, older versions fallback)
- Loads all photos & videos from the device via `MediaStore`
- Group by **Month** or **Album**, sort by **Date / Size / Name**
- Swipe-card UI (drag left = delete, drag right = keep) with a 3rd action,
  **Organize**, to move photos to a folder while swiping
- Random clean mode, cleanup goal with progress bar, per-group resume
- Smart detection: near-duplicate photos & blurry photos (on-device, no
  upload), optional backup-before-delete
- App lock: biometric prompt (fingerprint/face) with automatic fallback to
  your device's PIN/pattern/password, 3 full theme styles, built-in crash
  logger
- Progress per group is saved (Jetpack DataStore) so reopening a group
  picks up where you left off
- Batch delete using the proper Android 11+ `MediaStore.createDeleteRequest`
  system confirmation (with a direct-delete fallback for older Android)

See `CHANGELOG.md` for the full, newest-first history of every change, and
`PROJECT_STATE.md` for current status + what's next.

## 🛠 Building it yourself

This project includes `.github/workflows/build.yml`, which builds the APK
in the cloud automatically on every push — GitHub's servers do the
compiling, you never need Android Studio or a computer.

**Android phone (Termux, recommended):**
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
6. On github.com, open your repo → **Actions** tab — "Build APK" runs
   automatically (~3-6 minutes). When it finishes, check the repo's
   sidebar → **Releases** for the signed `.apk`, ready to download and
   install directly (see the shortcut at the top of this README).

**iPhone:** use the **Working Copy** app instead of Termux — it can unzip,
commit, and push to GitHub directly from its own file browser, with the
same steps 3–4 above for creating the repo/token.

**On a computer, with Android Studio:**
1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `GalleryCleaner` folder.
3. Let it sync (auto-downloads Gradle and dependencies the first time —
   needs internet access once).
4. Click **Run ▶** with a device/emulator connected, or
   `Build → Build Bundle(s) / APK(s) → Build APK(s)` to just get the
   `.apk` (shows up under `app/build/outputs/apk/debug/`).

Every time you push a change, GitHub automatically rebuilds and republishes
the APK for you.

## 💡 Ideas for next steps

- Multi-language support (Spanish, Portuguese-BR)
- One-time-purchase premium tier
- Play Store readiness (privacy policy, Data Safety form, screenshots)

See `PROJECT_STATE.md` → "Belum Dikerjakan" for the up-to-date pending list.
