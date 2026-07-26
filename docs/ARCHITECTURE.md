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

