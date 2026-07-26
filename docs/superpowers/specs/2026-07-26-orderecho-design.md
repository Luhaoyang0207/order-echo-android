# OrderEcho First Release — Design

**Status:** Approved for review  
**Date:** 2026-07-26

## Purpose

OrderEcho helps one restaurant review its existing Huawei automatic call recordings on a Huawei BAC-AL00 running Android 8.0. It is not a call recorder. It reads only locally generated AMR recordings and provides safe on-device playback, visual organization, and retention cleanup.

## Scope

### Included

- Android 8.0+ app, with API 26 as the minimum supported version.
- Scan `/storage/emulated/0/Sounds/Callrecord/` for `.amr` files.
- Chinese-language UI with a recording screen and a settings screen.
- Virtual hierarchy: month, then date, then individual recording.
- Display a parsed telephone number when available, plus date/time, duration, and file size.
- In-app AMR playback using Android `MediaPlayer`: play, pause, stop, progress, current time, total time, and one active playback at a time.
- Refresh list and search by telephone number.
- Confirmed deletion of a selected individual recording.
- Automatic retention cleanup at app start, from a best-effort daily background job, and from a user-triggered action.
- Retention choices: 7, 30, 60, 90, or 180 natural calendar days; 30 days is the default.
- Status in Settings: selected period, automatic-cleanup state, recording count, occupied space, oldest recording date, and most recent cleanup result.

### Excluded

- Capturing call audio, accessing call content, or changing the Huawei phone app.
- Root, privileged/system installation, or a custom dialer.
- Moving, renaming, uploading, synchronizing, or backing up recordings.
- Network permissions, user accounts, servers, databases, ads, or analytics.
- Support guarantees for phones other than the restaurant's BAC-AL00.

## User experience

The app opens directly to the recording list. Newer recordings appear first. A month header contains date sections, and each recording row offers a large play control plus its number and basic metadata. The list remains a virtual view of Huawei files; the physical folder is never reorganized.

The Settings screen lets an employee choose one of the five retention periods and run cleanup immediately. Individual deletion always requires confirmation. A clear permission screen is shown instead of an empty list when storage permission is absent.

## Data flow

1. Huawei finishes a call and creates an AMR file in `Sounds/Callrecord`.
2. OrderEcho scans only direct AMR files in that directory after storage permission is granted.
3. The repository validates file paths, extracts available metadata from the file name, and falls back to safe file metadata when parsing fails.
4. The grouping service places recording models under month and date sections in memory.
5. `MediaPlayer` plays the selected validated local file.
6. Cleanup computes the local start-of-day cutoff from the selected retention value, validates each candidate again, and deletes only eligible AMR files.

## Safety and privacy

- No `INTERNET` permission.
- Do not log recording contents or complete telephone numbers.
- Check canonical paths before playback and deletion to prevent path traversal or accidental deletion outside the Callrecord directory.
- Restrict cleanup and individual deletion to `.amr` files in that directory.
- Continue scanning or cleanup after a malformed, locked, or damaged individual file; surface a non-sensitive error.

## Android constraints

Android 8 grants the required shared-storage access through runtime `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` permissions. A boot receiver can re-register scheduled cleanup after a restart. The background scheduler is best effort: Android battery saving can delay it, so startup cleanup and the manual cleanup control ensure expired recordings are eventually removed.

## Verification plan

Before release, test on the physical BAC-AL00:

1. Permission grant, refusal, and recovery from system settings.
2. Scanning actual Huawei-generated AMR files.
3. AMR playback and switching between two recordings.
4. Month/date grouping across a month boundary.
5. Each retention option using dated test files, including the cutoff boundary.
6. Individual deletion confirmation and a failed deletion.
7. Cleanup after app relaunch and after a phone reboot.
8. Confirm the manifest contains no network permission.

