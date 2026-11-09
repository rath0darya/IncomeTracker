# Anon Tracker

Personal UPI income ledger. Customer payments go directly to the user's existing UPI-linked bank account. The app does not collect, route, hold or initiate payments.

## Features
- QR generated from configured UPI ID
- QR-session-aware UPI notification matching
- Detected alerts are pending until manually verified
- Manual income records and transaction ledger
- Hourly, daily, weekly and monthly insights
- Dark/light theme and app-wide AnonVox footer
- Room database persistence

## Build
JDK 17 and Gradle 8.9: `gradle :app:assembleDebug`

## GitHub release
The single-job GitHub Actions workflow builds a release APK signed with the Android Gradle plugin's debug signing identity generated on the runner. No repository signing secrets or manual secret setup are required. The workflow verifies the APK signature and publishes the APK plus source ZIP and TAR.GZ archives to GitHub Releases.

**Update limitation:** GitHub-hosted runners do not retain the generated debug signing key between runs. A later APK can therefore have a different signing identity. Android may require uninstalling the previous app before installing a later release. App data stored on the device may be lost when uninstalling.

## Detection limitations
Notification access is user-authorized. Matching is heuristic and depends on supported UPI app notification formats. A notification is not proof of settlement; detected entries are PENDING until verified against the bank or UPI app. SMS and email are not accessed.
