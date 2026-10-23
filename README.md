# Steady

A minimal, private Android habit streak tracker built with native Kotlin and Android XML layouts. The app works offline, does not request network access, and stores its data in app-private SharedPreferences.

## Features

- Minimal dark navy and electric-blue interface.
- One daily check-in from the app or Android home-screen widget.
- Current streak and best streak; the best streak survives a reset or recovery check-in.
- Selectable strict, one-grace-day, or two-grace-day rules.
- Recovery flow with an optional private note. It records a setback, resets the active streak, and preserves the best streak.
- Recovery history shows the latest five entries; up to 20 entries are stored locally.
- No account, analytics, ads, or network permission.
- Android backup and device-transfer backup are disabled for app data.

## Streak semantics

The counter advances on a successful daily check-in, not merely because a calendar day passed. Grace days define the maximum gap between two check-ins. If that gap expires, the current streak displays as zero; the best streak remains saved. The recovery action is optional and should be used only if you want to record a setback. A normal missed day does not create a recovery entry automatically.

## Build an APK with GitHub Actions

1. Push this project to a GitHub repository (branch `main`), including `.github/workflows/build.yml`.
2. The **Build Steady APK** workflow runs automatically (about 3-5 minutes). You can also run it manually from the **Actions** tab.
3. On your phone, open the repository → **Releases** → **Steady (latest build)** → tap `Steady.apk` to download it.
   (Alternative: the Actions run also keeps a `steady-apk` artifact, but that is a zip and needs login.)
4. Open the downloaded file and allow "Install unknown apps" for your browser when Android asks.

The workflow builds a debug-signed APK for personal use, not a Play Store release. Unit tests run after the APK is built and never block it.

## Add the widget

Long-press an empty area of the Android home screen → **Widgets** → **Steady** → add the widget. Tap the check-in button to mark the day complete; tap elsewhere on the widget to open the app.

## Manual test checklist

- First launch shows current streak `0` and best streak `0`.
- Tapping check-in once changes current streak to `1`; tapping again the same day does not increment it.
- After checking in on consecutive eligible days, current streak increments and best streak follows it.
- Resetting the active streak does not erase best streak or recovery history.
- Saving a recovery check-in clears the active streak, increments the recovery count, saves the optional note, and preserves best streak.
- Recovery history displays the latest entries and remains available after closing and reopening the app.
- App and widget display the same current streak and check-in state.
- Widget check-in disables itself for the rest of the day and refreshes the app state.
- With strict mode, an expired gap shows current streak `0`; the next check-in starts at `1`.
- App has no internet permission, and backup is disabled in the manifest.

## Technical notes

- Android application: Kotlin, minimum SDK 26, Java 17 target.
- Local persistence: Android `SharedPreferences`; no external libraries or cloud service.
- Widget: `AppWidgetProvider` + `RemoteViews`.
- The widget uses an inexact alarm for midnight refresh; Android may deliver it a little late due to power management.
