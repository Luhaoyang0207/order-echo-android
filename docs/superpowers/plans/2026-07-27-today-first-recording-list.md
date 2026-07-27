# Today-First Recording List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the recording list so today is immediately usable while older call days remain compact and expandable.

**Architecture:** Keep the existing repository and `RecordingGrouper` unchanged. Add an in-memory date-expansion state in `RecordingListViewModel`, pass it to `RecordingListAdapter`, and flatten only expanded date groups into recording rows. XML layouts provide the compact header, date buttons, and low-emphasis delete action.

**Tech Stack:** Kotlin, AndroidX RecyclerView, XML layouts, JUnit 4, Espresso/UI Automator instrumentation tests; Android 8.0 / API 26 minimum.

## Global Constraints

- Read recordings only from `/storage/emulated/0/Sounds/Callrecord/`; do not move or rename Huawei files.
- Do not request `INTERNET` or add networking, accounts, analytics, ads, or a database.
- Preserve all existing storage-boundary checks, playback behavior, retention options, cleanup behavior, and deletion confirmation.
- Today starts expanded; every other date starts collapsed.
- Search results expand every matching date; clearing search restores the normal Today-first expansion state.
- Recordings may be 10–30 per day, so avoid full-directory media metadata parsing and avoid one large action bar per row.
- All user-facing copy remains Simplified Chinese.

---

## File map

| Path | Responsibility |
| --- | --- |
| `ui/RecordingListViewModel.kt` | Own date-expansion state and produce visible month/date sections. |
| `ui/RecordingListAdapter.kt` | Render expandable date headers and compact rows. |
| `ui/MainActivity.kt` | Forward date-header taps and preserve existing search/playback/delete behavior. |
| `res/layout/fragment_recording_list.xml` | Add a compact title/count header. |
| `res/layout/item_date_header.xml` | Make each date header a large clickable touch target. |
| `res/layout/item_recording.xml` | Compact static row; playback controls appear only when active. |
| `res/values/strings.xml` | Today/yesterday, counts, and accessibility labels. |
| `ui/RecordingListViewModelTest.kt` | Expansion rules and search behavior. |
| `ui/MainActivityNavigationTest.kt` | Date-header show/hide instrumentation coverage. |

### Task 1: Add Today-first expansion state

**Files:**
- Modify: `app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListViewModel.kt`
- Modify: `app/src/test/java/com/luhaoyang/orderecho/ui/RecordingListViewModelTest.kt`

**Interfaces:**

```kotlin
data class VisibleDateGroup(
    val date: LocalDate,
    val recordings: List<RecordingFile>,
    val expanded: Boolean
)

data class VisibleMonthGroup(
    val month: YearMonth,
    val dates: List<VisibleDateGroup>
)

fun toggleDate(date: LocalDate): RecordingListState
fun visibleGroups(state: RecordingListState.Content): List<VisibleMonthGroup>
```

- [ ] **Step 1: Write failing ViewModel tests**

Create recordings for today, yesterday, and an older date. Add these tests:

```kotlin
@Test fun refreshExpandsOnlyTodayByDefault() {
    val state = viewModel.refresh() as RecordingListState.Content
    val dates = state.groups.flatMap { it.dates }
    assertTrue(dates.single { it.date == LocalDate.now() }.expanded)
    assertFalse(dates.single { it.date == LocalDate.now().minusDays(1) }.expanded)
}

@Test fun togglingAnOlderDateShowsOnlyThatDatesRows() {
    viewModel.refresh()
    val state = viewModel.toggleDate(LocalDate.now().minusDays(1)) as RecordingListState.Content
    assertTrue(state.groups.flatMap { it.dates }.single { it.date == LocalDate.now().minusDays(1) }.expanded)
}

@Test fun searchExpandsEveryDateThatContainsAMatch() {
    viewModel.refresh()
    val state = viewModel.setQuery("471") as RecordingListState.Content
    assertTrue(state.groups.flatMap { it.dates }.all { it.expanded })
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest`

Expected: FAIL because date expansion state and `toggleDate` do not exist.

- [ ] **Step 3: Implement minimal expansion state**

Store `expandedDates: MutableSet<LocalDate>` in the ViewModel. On `refresh()` and `clearQuery()`, set it to `setOf(LocalDate.now())`. In `setQuery()`, use the dates present in the filtered result when the query is non-empty. `toggleDate(date)` toggles only that date and returns the current list state. Update `RecordingListState.Content` to hold `List<VisibleMonthGroup>`, mapping each existing `MonthGroup` to its visible date groups while retaining month/date ordering from `RecordingGrouper`.

- [ ] **Step 4: Run focused and full unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListViewModel.kt app/src/test/java/com/luhaoyang/orderecho/ui/RecordingListViewModelTest.kt
git commit -m "feat: add today-first recording expansion"
```

### Task 2: Render compact expandable recording sections

**Files:**
- Modify: `app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListAdapter.kt`
- Modify: `app/src/main/java/com/luhaoyang/orderecho/ui/MainActivity.kt`
- Modify: `app/src/main/res/layout/fragment_recording_list.xml`
- Modify: `app/src/main/res/layout/item_date_header.xml`
- Modify: `app/src/main/res/layout/item_recording.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/androidTest/java/com/luhaoyang/orderecho/ui/MainActivityNavigationTest.kt`
- Modify: `README.md`
- Modify: `docs/HANDOFF.md`

**Interfaces:**

```kotlin
class RecordingListAdapter(
    onPlay: (RecordingFile) -> Unit,
    onStop: () -> Unit,
    onDelete: (RecordingFile) -> Unit,
    onToggleDate: (LocalDate) -> Unit
)

fun submit(groups: List<VisibleMonthGroup>, playbackState: PlaybackState)
```

- [ ] **Step 1: Write the failing instrumentation test**

Add a test that grants storage permission, opens the recordings tab, taps an older date header, and asserts its number row appears. Tap the same header again and assert that row no longer exists:

```kotlin
onView(withText("昨天 · 1 条")).perform(click())
onView(withText("4712345678")).check(matches(isDisplayed()))
onView(withText("昨天 · 1 条")).perform(click())
onView(withText("4712345678")).check(doesNotExist())
```

Use an injected/test Callrecord directory or fixture recordings; never point the test at a real device recording directory.

- [ ] **Step 2: Compile the instrumentation test before implementation**

Run: `./gradlew.bat :app:assembleDebugAndroidTest`

Expected: test APK compiles; execution is deferred to a connected Android device if none is available.

- [ ] **Step 3: Implement the compact UI**

Render a month label followed by a clickable date header with an expand/collapse indicator and date count. Only append `Row.Recording` entries when `VisibleDateGroup.expanded` is true. Header text uses `今天 · %d 条`, `昨天 · %d 条`, otherwise `M月D日 · %d 条`.

Add a `通话录音 · %d 条` header above search. Use rounded, white recording rows with number, time/duration, and one prominent play/pause button. Keep progress and Stop hidden unless the row is active. Replace the always-visible Delete button with a small `更多` button that opens the existing confirmation flow.

Wire `onToggleDate` in `MainActivity` to `render(viewModel.toggleDate(date))`. Keep search, refresh, cleanup, and playback progress behavior unchanged.

- [ ] **Step 4: Verify the app**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`

Expected: all unit tests and both APKs build successfully. Run the new instrumentation test on the BAC-AL00 or another connected Android device before release.

- [ ] **Step 5: Update docs and commit**

Update README with the Today-first behavior and HANDOFF with build/device-test evidence.

```bash
git add app/src/main/java/com/luhaoyang/orderecho/ui app/src/main/res app/src/androidTest README.md docs/HANDOFF.md
git commit -m "feat: redesign recording list for daily calls"
```

## Acceptance checklist

- [ ] Today is expanded on first load; older dates are collapsed.
- [ ] A date header toggles only its own recordings.
- [ ] Searching expands matching dates; clearing search restores Today-first behavior.
- [ ] A compact idle row has number, time/duration, and play only.
- [ ] Progress and Stop appear only for the currently active recording.
- [ ] Deletion remains confirmed and all retention/playback/storage safety behavior is unchanged.
- [ ] Debug APK and Android-test APK build successfully.
- [ ] On BAC-AL00, the page remains responsive with at least three dates of recordings.
