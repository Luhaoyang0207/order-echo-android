# OrderEcho — AI Agent Instructions

## Project purpose

OrderEcho is a fully offline Android app for one restaurant's Huawei BAC-AL00 (Android 8.0) phone. Huawei's built-in phone app records incoming calls. OrderEcho manages the resulting AMR files; it does not record calls itself.

## Read first

Before changing code, read:

- `README.md`
- `docs/HANDOFF.md`
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- the latest file under `docs/superpowers/specs/`

## Non-negotiable scope

- Support Android 8.0 and later; the target device is Android 8.0 / API 26.
- Read recordings only from `/storage/emulated/0/Sounds/Callrecord/`.
- Do not implement call recording, call control/interception, cloud sync, accounts, analytics, ads, or networking.
- Read-only PHONE_STATE observation and system Call Log lookup are allowed solely for the first-incoming-call overlay requested on 2026-09-17. Do not persist phone numbers or replace the Huawei dialer.
- Do not request the `INTERNET` permission.
- Do not move or rename Huawei-generated recording files.
- Any deletion must be limited to AMR files inside the configured Callrecord directory.
- Keep the code simple, offline, and maintainable for a restaurant employee.

## Shared-memory maintenance

After every meaningful task, update `docs/HANDOFF.md`. Record durable product or technical choices in `docs/DECISIONS.md`.

