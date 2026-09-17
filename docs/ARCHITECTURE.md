# Architecture

## Overview

OrderEcho is a single-module, offline Android application. The Huawei system phone app remains the source of recordings; OrderEcho is a local file manager and player for that one directory.

```text
Huawei Phone app
  └─ creates .amr recordings in Sounds/Callrecord
       └─ OrderEcho scans files and parses metadata
            ├─ Recording list: month → date → recording
            ├─ Local AMR player
            ├─ Manual deletion
            └─ Retention cleanup
```

## Planned components

| Component | Responsibility |
| --- | --- |
| Permission coordinator | Request and explain Android 8 storage access. |
| Recording repository | Safely list and parse AMR files inside the fixed directory. |
| Recording model | Hold display metadata and a validated local file reference. |
| Grouping service | Build month and date display sections without moving files. |
| Playback controller | Play one AMR recording at a time through Android `MediaPlayer`. |
| Retention service | Delete only validated AMR files older than the selected calendar-day cutoff. |
| Cleanup scheduler | Request a best-effort once-daily cleanup and restore it after reboot. |
| Settings store | Persist the selected retention option and last cleanup result locally. |
| UI | Chinese recording list and settings screens with large touch targets. |

## Storage safety boundary

Every operation that reads, plays, or deletes a recording must validate that its canonical path is a direct descendant of the canonical Callrecord directory and that it has the `.amr` extension. The app must never delete folders or files outside that boundary.

## Grouping and retention

The UI hierarchy is `month → date → recording`, for example:

```text
2026年7月
  7月26日 · 3 条
  7月25日 · 6 条

2026年6月
  6月30日 · 2 条
```

The retention setting offers 7, 30, 60, 90, and 180 days; it defaults to 30. Cleanup compares each recording's resolved recording date to the start-of-day cutoff in the device's local time zone.

## Failure handling

- Missing directory: show a clear empty/error state; do not create or alter Huawei directories.
- Permission denied: explain why storage permission is required and offer a route to system settings.
- Malformed name or metadata: show a safe fallback name/date where possible; skip unsafe files.
- Playback failure: show an error for that recording and release player resources.
- Deletion failure: retain the file, report the failure, and continue cleaning other eligible files.


## First incoming call hint (2026-09-17)

Independent `calls/` components observe protected manifest PHONE_STATE broadcasts.
`IncomingCallReceiver` records the first ringing receipt time and updates the
main-thread-only `IncomingCallSession`. One canonical number and generation token
live in memory until the call ends; no number is persisted.

The receiver starts non-exported `IncomingCallService` only for the first usable
number. The service immediately enters the foreground, then runs
`CallHistoryChecker` on a serialized executor with CancellationSignal. SQL filters
incoming/missed/rejected/blocked records older than the fixed cutoff, reads only
NUMBER/DATE/TYPE, and closes its lazy cursor on match, exhaustion or failure.
`AndroidCallNumber` combines PhoneNumberUtils with the Norwegian complete-number
rule in `CallNumber`; recording substring search is deliberately independent.

FIRST/PREVIOUS/UNKNOWN distinguishes absence of earlier inbound calls from query
failure. Before FIRST displays, the service rechecks session token, runtime/overlay
permissions and live RINGING state. OFFHOOK/IDLE synchronously invalidates results.
Conditional `stopSelfResult(startId)` preserves newer queued service starts.

`FirstCallOverlayManager` owns one application-context TYPE_APPLICATION_OVERLAY
window with no input focus or touch handling. Receiver state changes, 500ms state
checks, a 12-second overlay timeout, a 15-second total service deadline and service
destruction all remove it. Cancellation never changes Huawei call state.

SettingsFragment provides runtime permission requests and package-scoped overlay
settings/recovery. MainActivity routes only its own storage permission callback;
Settings can be entered even if storage was denied. The original boot receiver is
unchanged: it still only schedules recording cleanup. Manifest PHONE_STATE receipt
provides first-call background entry after reboot/unlock, subject to EMUI controls.