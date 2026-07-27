# OrderEcho Recording List Redesign

**Status:** Approved for written review  
**Date:** 2026-07-27

## Goal

Make the recording page calm and efficient for a restaurant receiving 10–30 calls each day. The default view must make today's recordings immediately accessible while keeping older days compact.

## Selected direction

Use the approved **Today-first** layout. Today is expanded by default. Yesterday and all earlier dates are collapsed by default, with each date header showing its date and recording count. The existing virtual month/date organization remains; no Huawei file is moved or renamed.

## Layout

1. A compact page header shows `通话录音` and today's recording count.
2. Search and refresh remain at the top.
3. Each month contains date sections. The current date starts expanded; all other dates start collapsed.
4. A date header is a large touch target and displays `今天 · N 条`, `昨天 · N 条`, or `M月D日 · N 条`.
5. A recording row displays only a bold phone number, `time · duration`, and one prominent play/pause control.
6. Playback progress and an explicit Stop control appear only for the active recording.
7. Deletion moves to a low-emphasis overflow action so it does not compete with playback.

## Interaction rules

- Expanding one date does not automatically collapse another date.
- Search results expand every date containing a matching number, so a match is never hidden.
- Clearing the search restores the normal Today-first expansion state.
- The empty, missing-directory, permission, playback-error, and cleanup states retain their current behavior.
- Month/date grouping, retention choices, playback safety validation, and deletion confirmation are unchanged.

## Data and performance

Expansion state is UI-only and keyed by the recording date. It is rebuilt on refresh, starts with today expanded, and does not affect files, storage scans, playback, or cleanup. The redesign must not reintroduce full-directory media metadata parsing.

## Verification

- Unit tests cover default expansion, collapsing/expanding a date, and search-result expansion.
- Instrumentation test covers tapping a date header and confirms that its recording rows appear/disappear.
- Build the debug APK and test on the Huawei BAC-AL00 with at least three dates of recordings.

