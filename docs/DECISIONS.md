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

## 2026-07-26 — Revalidate the recording boundary immediately before deletion

Recording discovery and deletion use the canonical Callrecord directory and accept only direct regular-file children with an AMR extension. The same checks are repeated immediately before each `File.delete()` call, so a model object cannot authorize a later deletion outside the Huawei recording directory.

## 2026-07-26 — Use repository validation before AMR playback

The playback controller asks `RecordingRepository` for currently validated recordings before opening a file with `MediaPlayer`. This keeps playback within the same canonical direct-child AMR boundary as discovery and deletion, while leaving the Huawei-generated files unchanged.

## 2026-07-26 — Isolate recording-file failures and report partial cleanup

Repository scans treat each direct child as an independent operation: canonical-path or metadata failure skips and counts only that entry. Deletion exceptions become failed results, cleanup continues with later eligible recordings, and the latest deleted/failed counts are persisted for Settings and background-worker reporting.

## 2026-07-26 — Read duration without weakening the storage boundary

The Android duration reader is injected into `RecordingRepository` and runs only after the repository validates a direct AMR child. Duration extraction failure leaves the recording visible with `时长未知`; it never authorizes another path or prevents other recordings from loading.
