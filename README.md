# Snaply

A swipe-to-clean gallery app: swipe left to delete, right to keep. Organizes
your photos/videos by month or album, remembers where you left off in each
group, and lets you sort by date, size, or name.

## 🔗 Download the latest APK

**[github.com/FDzaki-dev/GalleryCleaner/releases/latest](https://github.com/FDzaki-dev/GalleryCleaner/releases/latest)**
 — signed APK, auto-rebuilt on every push to `main`. Download and
install (enable "install from unknown sources" if prompted).

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
  your device's PIN/pattern/password, 4 full theme styles, built-in crash
  logger
- Progress per group is saved (Jetpack DataStore) so reopening a group
  picks up where you left off
- Batch delete using the proper Android 11+ `MediaStore.createDeleteRequest`
  system confirmation (with a direct-delete fallback for older Android)

See `CHANGELOG.md` for the full, newest-first history of every change, and
`PROJECT_STATE.md` for current status + what's next.

## 🛠 Building it yourself

Builds via `.github/workflows/build.yml` on every push — no local
compiling needed.

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
6. On github.com → your repo → **Actions** tab — "Build APK" runs
   (~3-6 min). When done, **Releases** has the signed `.apk`.

**iPhone:** use **Working Copy** instead of Termux — unzip, commit, push
to GitHub from its file browser, same steps 3–4 above.

**On a computer, with Android Studio:**
1. Install **Android Studio**: https://developer.android.com/studio
2. **Open** → select this `GalleryCleaner` folder.
3. Let it sync (downloads Gradle/dependencies once, needs internet).
4. **Run ▶** with a device/emulator, or `Build → Build Bundle(s) /
   APK(s) → Build APK(s)` → `.apk` under `app/build/outputs/apk/debug/`.

Every push to GitHub auto-rebuilds and republishes the APK.

## 💡 Ideas for next steps

- App language is hardcoded to casual Indonesian (Batch141) — multi-language (Spanish, Portuguese-BR) only if that lock is lifted
- One-time-purchase premium tier
- Play Store readiness (privacy policy, Data Safety form, screenshots)

See `PROJECT_STATE.md` → "Belum Dikerjakan" for the up-to-date pending list.
