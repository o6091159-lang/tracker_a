# Tracker Professional - GitHub APK Builder

This repository packages the Tracker Professional HTML app inside a small Android WebView application.

## Build without Android Studio
1. Upload these files to a GitHub repository.
2. Open the repository's **Actions** tab.
3. Open **Build Android APK** and run it with **Run workflow**, or push to `main`.
4. When the workflow finishes, open the run and download the artifact named **Tracker-Professional-APK**.
5. Extract the artifact and install `app-debug.apk` on Android.

The HTML app is stored at `app/src/main/assets/index.html`.

## App icon
The launcher icon lives in `app/src/main/res/mipmap-*` (adaptive icon for Android 8+, plus legacy PNGs for older versions) and is referenced from `AndroidManifest.xml`.

## Notifications
The app shows real Android notifications (permission requested when notifications are switched on).
- While the app is open, alerts are shown immediately.
- A background check runs about every 30 minutes (WorkManager) for the meal reminder and the daily shortfall alerts, so they also work when the app is closed. Exact timing depends on the phone's battery saver.
- Code: `NotificationHelper`, `TrackerBridge` (JS bridge), `ReminderWorker`, `ReminderScheduler`.
