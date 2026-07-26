# Decisions

## 2026-07-26 — Manage Huawei recordings rather than record calls

Huawei's built-in automatic call recording is already proven to create AMR files on the restaurant phone. OrderEcho will only manage those existing files. This avoids root access, protected telephony audio APIs, and fragile call-recording behavior.

## 2026-07-26 — Use virtual month/date grouping

Recordings will remain in Huawei's original `Sounds/Callrecord` directory. The interface will group them by month and date without moving or renaming files, avoiding broken links in the Huawei phone application.

## 2026-07-26 — Offer fixed retention choices, defaulting to 30 days

The settings screen will offer 7, 30, 60, 90, and 180 natural-day retention options. Thirty days is the default. This provides understandable choices while keeping deletion behavior testable and safe.

## 2026-07-26 — Stay offline

No recording data leaves the phone. OrderEcho must not request the `INTERNET` permission and will not include accounts, cloud sync, tracking, or analytics.

## 2026-07-26 — Kotlin/XML app with a single offline module

The first implementation will use Kotlin, XML layouts, and AndroidX in one Android application module. This keeps the app appropriate for the Android 8 target and avoids unnecessary infrastructure or dependencies.
