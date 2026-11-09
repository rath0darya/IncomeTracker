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

## Secure release signing
The release workflow requires these private GitHub Actions repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Never commit the keystore or passwords. The same private keystore must be reused for every update.

## Detection limitations
Notification access is user-authorized. Matching is heuristic and depends on supported UPI app notification formats. A notification is not proof of settlement; detected entries are PENDING until verified against the bank or UPI app. SMS and email are not accessed.
