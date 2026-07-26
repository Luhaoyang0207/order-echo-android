# OrderEcho First Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline Android app that lists, plays, and safely retains Huawei AMR call recordings on the restaurant's Android 8 phone.

**Architecture:** A single Android app module validates and reads AMR files from Huawei's fixed directory. A repository, retention policy, settings store, grouping service, cleanup worker, and `MediaPlayer` controller back two XML screens: recordings and settings.

**Tech Stack:** Kotlin, XML layouts, Android Gradle Plugin 8.2.2, Kotlin 1.9.22, AndroidX AppCompat/RecyclerView/Lifecycle/WorkManager, SharedPreferences, `MediaPlayer`, JUnit 4, AndroidX test runner.

## Global Constraints

- Application ID is `com.luhaoyang.orderecho`; minimum SDK is Android 8.0 / API 26.
- Support the Huawei BAC-AL00 only; read recordings exclusively from `/storage/emulated/0/Sounds/Callrecord/`.
- Do not capture calls, change Huawei's phone app, move/rename files, use root, or request `INTERNET`.
- No networking, accounts, cloud sync, ads, analytics, or database.
- Request only `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `RECEIVE_BOOT_COMPLETED` when required.
- Keep all UI strings in Simplified Chinese with large touch targets.
- Before deletion, verify the canonical path is inside Callrecord, is a direct child, and ends with `.amr`; never delete folders.
- Retention options are exactly 7, 30, 60, 90, and 180 natural calendar days; default is 30.
- Daily cleanup is best effort. App-start cleanup and manual cleanup are mandatory fallbacks.

## File Map

| Path | Responsibility |
| --- | --- |
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts` | Project, SDK, dependencies, tests. |
| `app/src/main/AndroidManifest.xml` | Permissions, launcher, boot receiver; no internet permission. |
| `model/RecordingFile.kt` | Immutable validated metadata. |
| `data/RecordingRepository.kt` | File validation, AMR parsing/listing/deletion. |
| `data/RetentionPolicy.kt` | Choices and natural-day cutoff. |
| `data/AppSettings.kt` | Retention and last cleanup timestamp. |
| `data/RecordingGrouper.kt` | In-memory month/date groups. |
| `playback/PlaybackController.kt` | One active AMR player. |
| `cleanup/*.kt` | Retention cleaner, worker, scheduler, restart receiver. |
| `ui/*.kt`, `res/layout/*.xml` | Recording list, permission state, settings. |
| `app/src/test`, `app/src/androidTest` | Unit and device tests. |

### Task 1: Create an installable offline Android shell

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/luhaoyang/orderecho/ui/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`
- Test: `app/src/test/java/com/luhaoyang/orderecho/ProjectConfigurationTest.kt`

**Interfaces:** Produces launcher `MainActivity : AppCompatActivity` and the installable `com.luhaoyang.orderecho` app used by every later task.

- [ ] **Step 1: Write the failing configuration test**

```kotlin
class ProjectConfigurationTest {
    @Test fun appUsesApi26AsMinimum() {
        assertEquals(26, BuildConfig.MIN_SDK_FOR_TEST)
    }
}
```

- [ ] **Step 2: Verify that it fails before project creation**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.luhaoyang.orderecho.ProjectConfigurationTest`

Expected: FAIL because Gradle and `BuildConfig` do not exist.

- [ ] **Step 3: Implement the minimal project**

Configure `minSdk = 26`, `targetSdk = 28`, and `buildConfigField("int", "MIN_SDK_FOR_TEST", "26")`. Add AppCompat, RecyclerView, Lifecycle, WorkManager, JUnit, and AndroidX test dependencies. The manifest declares storage and boot permissions only, and contains this launcher activity:

```xml
<activity android:name=".ui.MainActivity" android:exported="true">
  <intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
  </intent-filter>
</activity>
```

Make the activity inflate a layout with title `餐厅通话录音`.

- [ ] **Step 4: Verify build**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS; `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app
git commit -m "chore: create offline Android app shell"
```

### Task 2: Implement safe recordings and retention domain

**Files:**
- Create: `app/src/main/java/com/luhaoyang/orderecho/model/RecordingFile.kt`
- Create: `app/src/main/java/com/luhaoyang/orderecho/data/RetentionPolicy.kt`
- Create: `app/src/main/java/com/luhaoyang/orderecho/data/RecordingRepository.kt`
- Test: `app/src/test/java/com/luhaoyang/orderecho/data/RetentionPolicyTest.kt`, `RecordingRepositoryTest.kt`

**Interfaces:**

```kotlin
data class RecordingFile(val file: File, val phoneNumber: String?, val recordedAt: LocalDateTime, val sizeBytes: Long)
object RetentionPolicy {
  val allowedDays: Set<Int>
  fun cutoffDate(today: LocalDate, days: Int): LocalDate
  fun isExpired(recordedDate: LocalDate, today: LocalDate, days: Int): Boolean
}
class RecordingRepository(val baseDirectory: File) {
  fun list(): List<RecordingFile>
  fun delete(recording: RecordingFile): Boolean
}
```

- [ ] **Step 1: Write failing boundary and parsing tests**

```kotlin
@Test fun thirtyDaysIncludesTodayAndPreviousTwentyNineDays() {
  val today = LocalDate.of(2026, 7, 26)
  assertEquals(LocalDate.of(2026, 6, 27), RetentionPolicy.cutoffDate(today, 30))
  assertFalse(RetentionPolicy.isExpired(LocalDate.of(2026, 6, 27), today, 30))
  assertTrue(RetentionPolicy.isExpired(LocalDate.of(2026, 6, 26), today, 30))
}
@Test fun parsesKnownHuaweiName() {
  assertEquals("4712345678", repository.list().single().phoneNumber)
}
```

Also test the five permitted values, reject 14, ignore non-AMR files, and prevent outside-path deletion.

- [ ] **Step 2: Verify tests fail**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.luhaoyang.orderecho.data.*"`

Expected: FAIL because production domain classes do not exist.

- [ ] **Step 3: Implement the safe domain layer**

Use:

```kotlin
fun cutoffDate(today: LocalDate, days: Int): LocalDate {
  require(days in allowedDays)
  return today.minusDays((days - 1).toLong())
}
fun isExpired(date: LocalDate, today: LocalDate, days: Int) = date.isBefore(cutoffDate(today, days))
```

List only direct `.amr` children whose canonical path begins with `baseDirectory.canonicalPath + File.separator`. Parse `<number>_<yyyyMMdd>_<HHmmss>.amr`; use `lastModified()` in device time when parsing fails. Repeat the canonical-path, direct-child, file, and extension checks immediately before `File.delete()`.

- [ ] **Step 4: Verify tests pass**

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luhaoyang/orderecho/model app/src/main/java/com/luhaoyang/orderecho/data app/src/test
git commit -m "feat: add safe recording discovery and retention policy"
```

### Task 3: Add settings, grouping, and scheduled cleanup

**Files:**
- Create: `data/AppSettings.kt`, `data/RecordingGrouper.kt`
- Create: `cleanup/RetentionCleaner.kt`, `cleanup/CleanupWorker.kt`, `cleanup/CleanupScheduler.kt`, `cleanup/BootCompletedReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `data/AppSettingsTest.kt`, `data/RecordingGrouperTest.kt`

**Interfaces:**

```kotlin
data class DateGroup(val date: LocalDate, val recordings: List<RecordingFile>)
data class MonthGroup(val month: YearMonth, val dates: List<DateGroup>)
class RecordingGrouper { fun group(recordings: List<RecordingFile>): List<MonthGroup> }
class AppSettings(context: Context) {
  fun retentionDays(): Int; fun setRetentionDays(days: Int)
  fun lastCleanupAt(): Long; fun setLastCleanupAt(epochMillis: Long)
}
class RetentionCleaner(
  private val repository: RecordingRepository,
  private val settings: AppSettings,
  private val clock: () -> LocalDate = { LocalDate.now() }
) { fun clean(): CleanupResult }
```

- [ ] **Step 1: Write failing settings/grouping tests**

```kotlin
@Test fun newSettingsUseThirtyDaysByDefault() { assertEquals(30, settings.retentionDays()) }
@Test fun groupingSortsNewestMonthThenNewestDate() {
  assertEquals(YearMonth.of(2026, 7), RecordingGrouper().group(files).first().month)
}
```

Test persistence of 90 and rejection of 14.

- [ ] **Step 2: Verify tests fail**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.luhaoyang.orderecho.data.*"`

Expected: FAIL because settings and grouping classes do not exist.

- [ ] **Step 3: Implement settings, grouping, and cleanup**

Use a private `SharedPreferences` file called `order_echo_settings`. Sort recordings descending, group by `YearMonth` then `LocalDate`, both descending. Schedule unique periodic work:

```kotlin
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
  "recording-retention-cleanup", ExistingPeriodicWorkPolicy.KEEP,
  PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS).build()
)
```

`RetentionCleaner.clean()` calls repository `list()`, applies `isExpired`, continues after individual deletion failure, and records its completion time. The worker delegates to this cleaner. Register the receiver only for `BOOT_COMPLETED`; it only invokes `CleanupScheduler.schedule(context)`.

- [ ] **Step 4: Verify tests and build**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test
git commit -m "feat: add grouping settings and retention scheduling"
```

### Task 4: Implement single-file AMR playback

**Files:**
- Create: `playback/PlaybackState.kt`, `playback/PlaybackController.kt`
- Test: `playback/PlaybackStateTest.kt`

**Interfaces:** `PlaybackController` exposes `play(recording)`, `pause()`, `stop()`, `state()`, and `release()`. `PlaybackState` represents `Idle`, `Playing`, `Paused`, and `Error`. The unit-testable state layer exposes `sealed interface PlaybackEvent` and `fun reducePlaybackState(current: PlaybackState, event: PlaybackEvent): PlaybackState`.

- [ ] **Step 1: Write a failing state-transition test**

```kotlin
@Test fun startingAnotherFileReplacesActiveFile() {
  val result = reducePlaybackState(PlaybackState.Playing(first, 50, 1000), PlaybackEvent.Start(second, 2000))
  assertEquals(second, (result as PlaybackState.Playing).file)
}
```

- [ ] **Step 2: Verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.luhaoyang.orderecho.playback.PlaybackStateTest"`

Expected: FAIL because state classes do not exist.

- [ ] **Step 3: Implement controller behavior**

Validate with repository before playback. Starting another file stops and releases the old `MediaPlayer`. Completion releases it and publishes Idle; player error releases it and publishes a Chinese playback error. `pause`, `stop`, and `release` are safe when idle.

- [ ] **Step 4: Verify test and build**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luhaoyang/orderecho/playback app/src/test/java/com/luhaoyang/orderecho/playback
git commit -m "feat: add AMR playback controller"
```

### Task 5: Build recording list and permission UI

**Files:**
- Create: `ui/RecordingListViewModel.kt`, `ui/RecordingListAdapter.kt`
- Modify: `ui/MainActivity.kt`
- Create: `res/layout/fragment_recording_list.xml`, `item_month_header.xml`, `item_date_header.xml`, `item_recording.xml`, `view_permission_required.xml`, `view_empty_recordings.xml`
- Modify: `res/values/strings.xml`
- Test: `app/src/androidTest/java/com/luhaoyang/orderecho/ui/MainActivityPermissionTest.kt`

**Interfaces:** The ViewModel exposes `refresh()`, `setQuery(query)`, `play(recording)`, `delete(recording)`, and `runCleanup()`. Adapter item types are month header, date header, and recording row.

- [ ] **Step 1: Write a failing missing-permission device test**

```kotlin
@Test fun deniedStoragePermissionShowsRecoveryAction() {
  ActivityScenario.launch(MainActivity::class.java).use {
    onView(withText("需要存储权限")).check(matches(isDisplayed()))
    onView(withText("打开系统设置")).check(matches(isDisplayed()))
  }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.luhaoyang.orderecho.ui.MainActivityPermissionTest`

Expected: FAIL. If no device is connected, record `NO_DEVICE` and run it on BAC-AL00 before release.

- [ ] **Step 3: Implement the recording screen**

Request Android 8 storage permissions before scanning. If declined, show `需要存储权限` plus an `ACTION_APPLICATION_DETAILS_SETTINGS` button. Once granted, flatten virtual groups to RecyclerView rows, display `yyyy年M月` and `M月d日 · %d 条`, use `未知号码` as fallback, filter only phone number text, and show distinct missing-directory/empty/error states. Add large play, pause, progress, time, total duration, size, and delete controls.

- [ ] **Step 4: Verify available checks**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS. Run the connected test when a device is available.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/androidTest
git commit -m "feat: add grouped recordings and permission flow"
```

### Task 6: Build settings, confirmations, and release checks

**Files:**
- Create: `ui/SettingsFragment.kt`, `res/layout/fragment_settings.xml`
- Modify: `ui/MainActivity.kt`, `ui/RecordingListAdapter.kt`, `res/values/strings.xml`
- Test: `app/src/androidTest/java/com/luhaoyang/orderecho/cleanup/CleanupBoundaryTest.kt`
- Modify: `README.md`, `docs/HANDOFF.md`

**Interfaces:** Consumes `AppSettings`, `RecordingListViewModel.runCleanup()`, and repository deletion. Produces a settings screen exposing only the five retention choices and current cleanup statistics.

- [ ] **Step 1: Write failing settings/boundary device tests**

```kotlin
@Test fun settingsShowExactlyFiveRetentionChoices() {
  listOf("7 天", "30 天", "60 天", "90 天", "180 天").forEach {
    onView(withText(it)).check(matches(isDisplayed()))
  }
}
@Test fun cleanupDoesNotDeleteOutsideCallrecord() {
  RetentionCleaner(repository, settings) { LocalDate.of(2026, 7, 26) }.clean()
  assertTrue(outsideFile.exists())
}
```

- [ ] **Step 2: Run the device test**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.luhaoyang.orderecho.cleanup.CleanupBoundaryTest`

Expected: FAIL; record `NO_DEVICE` if appropriate.

- [ ] **Step 3: Implement settings and destructive confirmations**

Show only the five values, persist selection, and display count, occupied space, oldest date/`无录音`, and last cleanup/`尚未清理`. `立即清理` states the selected retention period and requires confirmation. Individual deletion states the number/`未知号码` and requires confirmation. On a success refresh; on failure show a non-sensitive Chinese error. Add bottom navigation between `录音` and `设置`.

- [ ] **Step 4: Verify build and absence of network permission**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

Run: `rg -n "android.permission.INTERNET" app/src/main/AndroidManifest.xml`

Expected: Gradle PASS; `rg` returns no matches.

- [ ] **Step 5: Update docs and commit**

Add build/install instructions to README and real verification status to HANDOFF.

```bash
git add app README.md docs/HANDOFF.md
git commit -m "feat: complete offline recording manager first release"
```

## Physical-device acceptance

- [ ] Install debug APK on BAC-AL00 and test grant/refusal/recovery for storage access.
- [ ] Confirm real Huawei AMR files appear in the correct virtual month/date groups.
- [ ] Confirm two AMR files play and switching stops the prior file.
- [ ] Confirm every retention choice uses its natural-day boundary, including a month boundary.
- [ ] Confirm non-AMR files and every file outside Callrecord remain untouched.
- [ ] Reboot, reopen, and confirm cleanup scheduling is restored.
- [ ] Confirm manifest has no `INTERNET` permission.
