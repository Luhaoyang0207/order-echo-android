# Task 1 Report: Today-first expansion state

## Status

Completed and verified.

## Changes

- Added UI-only `expandedDates` state in `RecordingListViewModel`, keyed by `LocalDate`.
- Added `VisibleMonthGroup` and `VisibleDateGroup` to expose the existing virtual month/date ordering plus each date's expansion state.
- `refresh()` and `clearQuery()` now reset expansion to today only.
- A non-empty search expands all dates represented in its filtered result.
- Added `toggleDate(date)` to independently expand or collapse one date.
- Preserved the existing adapter input at the activity boundary while the follow-up UI task changes it to render the new visible groups.
- Added ViewModel regression tests for default expansion, toggling yesterday, and search-result expansion using recordings for today, yesterday, and an earlier date.

## Verification

- RED verified: the focused ViewModel test task failed before implementation because `expanded` and `toggleDate` did not exist.
- PASS: `./gradlew.bat :app:testDebugUnitTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest` (with Android Studio JBR 17 configured as `JAVA_HOME`).
- PASS: `./gradlew.bat :app:testDebugUnitTest`.
- PASS: `git diff --check`.

## Review notes

- Expansion remains in memory only; it does not change the scan, repository, playback, cleanup, retention, or Huawei recording files.
- The activity's compatibility conversion intentionally does not render expansion yet. The next recording-list adapter/UI task must consume `VisibleMonthGroup` and wire date headers to `toggleDate`.
