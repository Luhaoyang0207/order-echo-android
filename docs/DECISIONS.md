## 2026-09-22 — Keep a displayed first-call hint for the active call

The restaurant requires `第一次来电` to remain visible throughout ringing, rather than disappearing after an arbitrary 12-second display timer. Once the first-call check has completed and one presentation route has shown the fixed label, only answer, rejection, hang-up, another terminal call state, permission/feature disable, service destruction, or an explicit replacement may remove it.

Retain the 15-second safety limit only for a Call Log query that has not yet produced a display. On a successful display, cancel that lookup deadline while preserving the existing 500 ms read-only telephone-state check. This keeps long ringing calls visible without letting a stuck provider query retain a foreground service indefinitely.
## 2026-09-22 — Use a user-enabled accessibility overlay for locked first calls (D10 design)

Huawei BAC-AL00 evidence now rules out the three ordinary app presentation routes:
an Activity hides the native InCall UI, an application overlay stays below it, and a
high-importance silent notification is posted but suppressed on that screen. The user
approved an accessibility-based route after reviewing this evidence.

The implemented service is deliberately display-only. It will not retrieve window content,
listen to or persist accessibility events, perform gestures, filter keys, take screenshots,
show caller information, or control calls. The user must manually enable it in Android
Accessibility Settings. It owns a compact noninteractive TYPE_ACCESSIBILITY_OVERLAY when connected; unavailable or failed display falls back to D9 silent notification. Real Huawei visual validation remains required before display success is claimed.

## 2026-09-21 — Use an Activity-free heads-up notification on Huawei Keyguard (D9)

ADB evidence from Huawei BAC-AL00 / Android 8 shows that every attempt to start
`LockedFirstCallActivity` moves Huawei `InCallActivity` to the background and destroys
its display surface. Window focus and touch flags do not change that task transition.
The earlier D8 Activity route is therefore removed rather than adjusted again.

For a locked FIRST result, retain the already user-configured API-26 high-importance
`首次来电识别（锁屏提示）` channel, but post only a silent public notification titled
`第一次来电`. It has neither a full-screen nor content `PendingIntent`, so OrderEcho
starts no Activity and cannot replace the native call task. The existing 12-second,
answer, hang-up, and service-destruction cancellation paths remain. The unlocked
non-touchable overlay remains unchanged. If EMUI suppresses this silent heads-up
notification over its call screen, ordinary app permissions cannot satisfy both
visibility and preservation of the Huawei call UI; any Accessibility-based alternative
requires separate user approval.
## 2026-09-21 — Locked hint must not take input focus or touches (D8)

Huawei physical-device evidence showed that the D7 high-priority full-screen notification
successfully launches `LockedFirstCallActivity`, but its Activity task hides the native call
controls behind a “return to call” affordance. The display path is therefore proven, while the
focused/touchable configuration is unacceptable.

Keep the silent, number-free API-26 full-screen route only for Keyguard, but make the Activity
window translucent, non-focusable and non-touchable. The Activity remains a small top-centred
label while Android routes key input and touch input to the Huawei call window beneath it. This
preserves visible locked-screen identification without adding sound, vibration, caller data or
call control. The unlocked overlay remains unchanged.
## 2026-09-21 — Treat Huawei notification-channel priority as a full-screen prerequisite (D6 evidence)

The Huawei D6 report records `LOCKED_HINT_CHANNEL_NOT_HIGH` immediately before the
full-screen notification is posted, with no Activity create or start event. This is
not an overlay or layout failure: the system never dispatched the full-screen
`PendingIntent`.

On Android 8, full-screen intents require a high-importance notification channel;
channel behavior is ultimately controlled by the system and user settings. Keep the
implementation silent and number-free as required. Do not add a notification sound or
vibration merely to force an interruption. The remaining supported route is to inspect
and, if available, enable high-priority / banner presentation for the dedicated
`首次来电识别` notification channel on the Huawei device.
## 2026-09-21 — Launch a small top Activity from a full-screen notification (D5)

D4 reached Huawei's locked notification route but the customer's device still hid its
silent notification. The user then explicitly authorized a focused and touchable
window, provided it remains small and does not cover Huawei's answer or hang-up
controls.

For a locked FIRST result only, post a silent public API-26 high-importance
notification with a full-screen `PendingIntent` for `LockedFirstCallActivity`. The
Activity uses the legacy API-26 show-when-locked flags and is a small top-centred
dialog that contains only `第一次来电`. It has no caller number, no other customer data,
no sound and no vibration. It is cancelled and closed on answer, hang-up, service
shutdown or a 12-second timeout. The existing non-touchable overlay remains the sole
unlocked presentation. The focused Activity is intentionally restricted to the small
label area so native call controls remain outside it.
## 2026-09-21 — Use a silent system notification on the locked call screen (D4)

The user confirmed on Huawei BAC-AL00 that a FIRST result and overlay are visible
when unlocked, while the same fresh-number call is not visible when locked. The D3
diagnostic photos also show HISTORY_FIRST and accepted overlay creation for real
customer calls. This isolates the fault to the keyguard/call-screen window layer.

`TYPE_APPLICATION_OVERLAY` cannot safely take priority over that system layer. An
API-26 locked-emulator check also showed an accepted overlay hidden behind Keyguard;
a Toast was hidden as well. Keep the existing overlay for unlocked calls. For a
locked FIRST result, use a silent, high-priority, public Android notification titled
`第一次来电`; it contains no caller number, starts no Activity and never changes the
phone call. Cancel it on the existing terminal lifecycle events. No permission,
dependency, account, network, number storage or dialer integration is added.
# Decisions

## 2026-09-19 — Enable foreground runtime reception on Huawei (D3)

D2 physical-device photos show default/SIM1 PhoneStateListener RINGING with number
presence and runtime PHONE_STATE RINGING/IDLE. There are no original manifest-receiver
events during the same foreground probe. This supports repairing event reception
rather than changing caller identity or history rules; the vendor-level reason the
manifest receiver is not invoked remains unknown.

Use one production, private foreground CallMonitoringService with a runtime
IncomingCallReceiver path. Reuse existing session deduplication, worker lookup and
short-lived overlay service. Do not feed multiple listener APIs into the session.
Keep the protected manifest receiver as fallback; both paths respect a new explicit
user-enabled preference, false by default (including upgrades). No phone state or
number is persisted. No permission or dependency is added.

The monitor has a persistent generic notification with Stop and a Settings toggle.
Disable invalidates pending sessions, cancels lookup/display and stops monitoring.
Settings observes changes so notification Stop cannot leave a stale Close button
that accidentally enables the feature. START_STICKY, ordinary boot after unlock,
package replacement and app resume attempt to restore only the enabled choice with
ready permissions; they cannot bypass force-stop, denied permissions or EMUI controls.

This supersedes the September 17 decision to rely solely on manifest reception and
avoid a long-lived service. Foreground runtime registration is necessary for the
path that actually delivered events on the target phone. The recording cleanup
receiver remains unchanged; a separate recovery receiver handles call monitoring.


## 2026-09-19 — Explicit 60-second reception probe (D2)

The user confirmed D1's test overlay is visible and a freshly reopened report still
contains only CLEARED after a phone call. Collect independent reception evidence
before selecting a permanent receiver/listener change.

A private service declared and implemented only in src/debug runs after an explicit
Settings tap, enters the foreground with a generic stop action, and unregisters
its runtime PHONE_STATE receiver and PhoneStateListeners after about 60 seconds or
manual stop. It is START_NOT_STICKY, has no boot entry, never feeds IncomingCalls,
never queries Call Log, and does not control calls. Repeated starts do not extend
the current deadline. It observes the default subscription and up to two current
active subscriptions; subscription IDs remain transient.

Record only fixed enum events, number-present/absent flags and timestamps within
the existing 32-event limit. A listener API return proves only a registration
request, not delivery; an actual callback is separate evidence. AppOps status is
another clue, not proof of successful event delivery. The probe's foreground
process can itself improve the original manifest receiver's delivery: if both
paths work during the probe, do not attribute that solely to runtime registration.

The report observes preference changes while displayed and unregisters on dismiss
or view destruction, avoiding D1's stale-open-dialog ambiguity. No production
identification behavior or permission was changed.

References: Android [broadcast registration](https://developer.android.com/develop/background-work/background-tasks/broadcasts),
[implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions),
and [Android 8 TelephonyManager implementation](https://android.googlesource.com/platform/frameworks/base/+/android-8.0.0_r1/telephony/java/android/telephony/TelephonyManager.java).


## 2026-09-18 — Local debug diagnostics for Huawei acceptance

The physical phone shows no hint even after the user confirmed all three permissions,
EMUI launch management, removal of the test number's system history and a foreground
app test. No USB connection is available. The root cause remains unverified.

Keep identification rules unchanged while gathering evidence. Debug builds retain
at most 32 timestamped, fixed-enum processing events in a separate private preferences
file. Never store caller numbers, Intent contents, Call Log rows or exception text.
Writes are asynchronous and best effort; an abrupt kill can lose the latest events.
Release builds do not collect events or expose diagnostic controls.

Settings offers a distinctly labelled overlay test, removed on timeout or leaving
the screen, plus a scrollable report and diagnostic-only clear action. A successful
WindowManager.addView is reported as system acceptance, not proof that EMUI displayed
it over the dialer. The ordinary-screen test does not establish locked-dialer support.


## 2026-09-17 — Read-only first incoming caller hint

The user explicitly expanded scope to observe PHONE_STATE and read system Call Log
for a first-incoming-call overlay on Huawei BAC-AL00 / Android 8. This supersedes
older no-call-state wording. No call recording/control, dialer replacement,
contacts, database, number persistence or network is introduced.

Use a manifest receiver (Android 8 broadcast exception), in-memory deduplicated
session and short foreground service with its required generic notification.
Avoid permanent services, Application changes and additional boot work. The
service uses conditional stopSelfResult so an old call cannot stop a newer queued
start. It owns cancellable worker queries and an application-context non-touchable
overlay; Activity lifetime cannot leak the window.

Count only incoming/missed/rejected/blocked rows before first-ring receipt minus
5000ms. Treat provider failure as UNKNOWN, not FIRST. Exact canonical Norway/E164
identity is separate from recording search substrings. Android provides no shared
call ID/exact ring timestamp, so unbounded vendor delay cannot be excluded with a
mathematical guarantee; document the margin, rapid-redial and deleted-history limits.

Keep minSdk 26, targetSdk 28, compileSdk 34 and existing dependencies. Suppress only
lint's ExpiredTargetSdkVersion because this is internal APK distribution, not a
Google Play release; keep all other lint checks active.

## 2026-07-27 — Replace direct views before showing a Fragment screen

The recording list is inflated directly into the activity's content container, while Settings is a Fragment. Before adding or replacing the Settings Fragment, the activity must remove direct child views from that container; otherwise the Fragment is drawn over the recording interface instead of replacing it.

## 2026-07-27 — Keep directory scans metadata-free

Recording discovery, retention cleanup, statistics, and playback-file validation must not read AMR media metadata. These paths can process many files, so duration extraction is limited to an explicitly requested, single validated recording and any future caller must run it off the UI thread. This keeps large recording archives responsive without weakening the storage boundary.

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
