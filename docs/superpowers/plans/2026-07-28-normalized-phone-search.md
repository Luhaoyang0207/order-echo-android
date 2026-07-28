# Normalized Phone-number Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make digit-only phone searches find recordings whose displayed Huawei number contains spaces or other separators, and place Refresh and Search in their approved locations.

**Architecture:** Keep number normalization in the recording-list UI layer as a pure helper. The ViewModel compares normalized digit sequences only; the repository and `RecordingFile` keep their current original data. The layout moves the existing Refresh action into the header and adds Search beside the input.

**Tech Stack:** Kotlin, Android XML Views, AndroidX Espresso, JUnit 4, Gradle.

## Global Constraints

- Support Android 8.0/API 26 and do not add dependencies.
- The app remains offline and must not request `android.permission.INTERNET`.
- Read recordings only from `/storage/emulated/0/Sounds/Callrecord/`; do not move or rename Huawei-generated files.
- Do not change deletion validation, playback behavior, retention behavior, or metadata-free scan behavior.
- Search normalization retains ASCII digits `0` through `9` only and matches a contiguous normalized substring.
- A separator-only query is equivalent to an empty query and restores the normal Today-first expansion state.
- The header shows `通话录音 · X 条` with `刷新` on the right; the next row has the number input with `搜索` on the right.

---

## File Structure

- `app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListViewModel.kt` — owns pure digit normalization and filters the existing in-memory list.
- `app/src/test/java/com/luhaoyang/orderecho/ui/RecordingListViewModelTest.kt` — regression coverage for matching and reset behavior.
- `app/src/main/res/layout/fragment_recording_list.xml` — header and search control hierarchy.
- `app/src/main/res/values/strings.xml` — Simplified Chinese `搜索` text.
- `app/src/main/java/com/luhaoyang/orderecho/ui/MainActivity.kt` — one shared search submission handler.
- `app/src/androidTest/java/com/luhaoyang/orderecho/ui/MainActivityNavigationTest.kt` — screen-level control and search test.
- `docs/HANDOFF.md` — delivered behavior and device-validation note.

### Task 1: Normalize phone-number matching in the ViewModel

**Files:**
- Modify: `app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListViewModel.kt`
- Modify: `app/src/test/java/com/luhaoyang/orderecho/ui/RecordingListViewModelTest.kt`

**Interfaces:**
- Produces: `internal fun normalizePhoneSearch(value: String?): String`, returning ASCII digits only.
- Produces: `RecordingListViewModel.setQuery(value: String): RecordingListState`, which treats an empty normalized value as `clearQuery()`.
- Consumes: existing `RecordingFile.phoneNumber: String?`, `RecordingGrouper`, and `RecordingListState.Content` unchanged.

- [ ] **Step 1: Write failing ViewModel regression tests**

Add these tests using the existing temporary `RecordingRepository` fixture:

```kotlin
@Test
fun digitsOnlyQueryMatchesPhoneNumberDisplayedWithSpaces() {
    val spaced = createRecording(LocalDate.now(), "123 12 123")
    val state = viewModel().apply { refresh() }.setQuery("12312123") as RecordingListState.Content

    assertEquals(listOf(spaced.phoneNumber), state.groups.flatMap { it.dates }.flatMap { it.recordings }.map { it.phoneNumber })
}

@Test
fun queryAndPhoneNumberIgnoreCommonSeparators() {
    val formatted = createRecording(LocalDate.now(), "+47 (123)-45 678")
    val state = viewModel().apply { refresh() }.setQuery("47-123 45") as RecordingListState.Content

    assertEquals(listOf(formatted.phoneNumber), state.groups.flatMap { it.dates }.flatMap { it.recordings }.map { it.phoneNumber })
}

@Test
fun separatorOnlyQueryRestoresUnfilteredTodayFirstState() {
    val state = viewModel().apply { refresh() }.setQuery(" -() ") as RecordingListState.Content

    val dates = state.groups.flatMap { it.dates }
    assertEquals(3, dates.flatMap { it.recordings }.size)
    assertTrue(dates.single { it.date == LocalDate.now() }.expanded)
    assertFalse(dates.single { it.date == LocalDate.now().minusDays(1) }.expanded)
}
```

Do not add `spaced` or `formatted` as class fixtures; each test creates its own safe direct-child AMR file.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest
```

Expected: the first two tests fail because `contains()` compares formatting literally; the separator-only test fails because it becomes a non-empty no-match query.

- [ ] **Step 3: Implement the smallest normalized matching helper**

Add this top-level helper after the ViewModel class:

```kotlin
internal fun normalizePhoneSearch(value: String?): String =
    value.orEmpty().filter { it in '0'..'9' }
```

Store the normalized query in `setQuery`. If it is empty, return `clearQuery()`. Use one private comparison helper in both `setQuery` and `displayedState()`:

```kotlin
private fun phoneNumberMatches(phoneNumber: String?, normalizedQuery: String): Boolean =
    normalizedQuery.isEmpty() || normalizePhoneSearch(phoneNumber).contains(normalizedQuery)
```

Keep this logic in the UI file. Do not alter repository parsing, file names, or scans.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the Step 2 command again.

Expected: all `RecordingListViewModelTest` tests pass, including existing no-match, date expansion, playback, and cleanup tests.

- [ ] **Step 5: Run the complete unit suite and commit**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
git diff --check
```

Commit only Task 1 files:

```powershell
git add app/src/main/java/com/luhaoyang/orderecho/ui/RecordingListViewModel.kt app/src/test/java/com/luhaoyang/orderecho/ui/RecordingListViewModelTest.kt
git commit -m "fix: normalize phone number search"
```

### Task 2: Move controls and wire the explicit Search action

**Files:**
- Modify: `app/src/main/res/layout/fragment_recording_list.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/luhaoyang/orderecho/ui/MainActivity.kt`
- Modify: `app/src/androidTest/java/com/luhaoyang/orderecho/ui/MainActivityNavigationTest.kt`
- Modify: `docs/HANDOFF.md`

**Interfaces:**
- Consumes: `R.id.recording_count_header`, existing `R.id.refresh`, `R.id.search_number`, and `RecordingListViewModel.setQuery(String)`.
- Produces: `R.id.search`, an explicit Search button whose click and editor action share one local `submitSearch()` function.
- Preserves: `R.id.refresh` calling only `viewModel.refresh()`; no file mutation is added.

- [ ] **Step 1: Write the failing instrumentation test**

In `MainActivityNavigationTest`, add imports for `typeText`, `closeSoftKeyboard`, and `allOf`. Add this test:

```kotlin
@Test
fun headerRefreshAndSearchButtonSupportDigitsOnlySearch() {
    File(testDirectory, "123 12 123_${LocalDate.now().toString().replace("-", "")}_141530.amr").writeText("amr")
    grantStoragePermissions()

    ActivityScenario.launch(MainActivity::class.java).use {
        onView(withId(R.id.recording_count_header)).check(matches(isDisplayed()))
        onView(withId(R.id.refresh)).check(matches(allOf(isDisplayed(), withText(R.string.refresh))))
        onView(withId(R.id.search)).check(matches(allOf(isDisplayed(), withText(R.string.search))))
        onView(withId(R.id.search_number)).perform(typeText("12312123"), closeSoftKeyboard())
        onView(withId(R.id.search)).perform(click())
        onView(withText("123 12 123")).check(matches(isDisplayed()))
    }
}
```

Create the second file only inside this test so `olderDateHeaderTogglesItsRecordingRows` keeps its expected `昨天 · 1 条` label.

- [ ] **Step 2: Compile the Android test APK and verify RED**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebugAndroidTest
```

Expected: compilation fails because `R.id.search` and `R.string.search` do not exist.

- [ ] **Step 3: Implement the approved control placement and shared handler**

In `fragment_recording_list.xml`, replace the standalone header TextView with a horizontal row. Keep `@+id/recording_count_header` at width `0dp` and weight `1`; place the existing `@+id/refresh` button as its right sibling. In the next horizontal row, keep `@+id/search_number` and replace the old refresh button with:

```xml
<Button
    android:id="@+id/search"
    android:layout_width="wrap_content"
    android:layout_height="48dp"
    android:text="@string/search" />
```

Add:

```xml
<string name="search">搜索</string>
```

to `strings.xml`. In `MainActivity.showRecordingList()`, keep the existing refresh listener and add one local function before listeners:

```kotlin
fun submitSearch() {
    render(viewModel.setQuery(content.findViewById<EditText>(R.id.search_number).text.toString()))
}
```

Bind both controls:

```kotlin
content.findViewById<Button>(R.id.search).setOnClickListener { submitSearch() }
content.findViewById<EditText>(R.id.search_number).setOnEditorActionListener { _, _, _ ->
    submitSearch()
    true
}
```

Do not trigger a scan on Search and do not clear input on Refresh.

- [ ] **Step 4: Build tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
git diff --check
```

Expected: all tasks succeed. The Android-test APK compiles; execution remains a physical-device requirement if no ADB device is attached.

- [ ] **Step 5: Update the handoff and commit**

Add a concise `Completed: normalized phone-number search and control placement` section near the top of `docs/HANDOFF.md`. State the digit-only matching rule, separator-only behavior, moved Refresh control, Search action, test evidence, and that BAC-AL00 visual/device testing remains outstanding.

Commit only Task 2 files:

```powershell
git add app/src/main/res/layout/fragment_recording_list.xml app/src/main/res/values/strings.xml app/src/main/java/com/luhaoyang/orderecho/ui/MainActivity.kt app/src/androidTest/java/com/luhaoyang/orderecho/ui/MainActivityNavigationTest.kt docs/HANDOFF.md
git commit -m "feat: add explicit normalized phone search"
```

