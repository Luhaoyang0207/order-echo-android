# OrderEcho — Handoff

## Current goal

Create the first offline Android app for a single restaurant to review and retain its Huawei-generated call recordings.

## Confirmed environment

- Restaurant device: Huawei BAC-AL00 (Huawei nova 2 Plus).
- Android version: Android 8.0 (API 26).
- Root access: not used.
- Huawei built-in automatic call recording is already working.
- Recording directory: `/storage/emulated/0/Sounds/Callrecord/`.
- File format: `.amr`.
- Known file-name shape: `phoneNumber_datetime.amr`.

## First-release scope

- Scan the Huawei Callrecord directory for `.amr` files.
- Display recordings in a Chinese UI grouped virtually by **month, then date**.
- Show telephone number, recording date/time, duration, and file size where available.
- Play AMR files inside OrderEcho.
- Allow a user to delete an individual recording after confirmation.
- Allow a user to choose a retention period: **7, 30, 60, 90, or 180 days**. The default is **30 days**.
- Cleanup runs when the app starts, through a best-effort daily background job, and on an explicit user action.
- Show storage/cleanup status in Settings.

## Retention rule

The selected number means whole **natural calendar days**, including today. For a 30-day setting on 2026-07-26, retain 2026-06-27 through 2026-07-26 and delete files dated 2026-06-26 or earlier.

## Important decisions

- Do not move or rename original Huawei files. Grouping is visual only.
- Do not implement phone recording or interfere with phone calls.
- The app remains fully offline and must not request the `INTERNET` permission.
- A background job may be delayed by Android 8 battery management, so app-start cleanup is a required fallback.

## Files added in this task

- `AGENTS.md`
- `README.md`
- `docs/HANDOFF.md`
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- `docs/superpowers/specs/2026-07-26-orderecho-design.md`
- `docs/superpowers/plans/2026-07-26-orderecho-first-release.md`

## Blockers

None for design. The actual directory path and AMR playback must be revalidated on the physical restaurant device during implementation.

## Next recommended task

Execute `docs/superpowers/plans/2026-07-26-orderecho-first-release.md` task by task. Initialize the empty local repository and connect it to `Luhaoyang0207/order-echo-android` before making the first planned commit.
