# OrderEcho — 餐厅通话录音管理 / Restaurant Call Recording Manager

OrderEcho 是一款面向餐厅专用 Android 手机的离线录音管理应用。它管理华为电话应用已经生成的通话录音；它**不负责录制电话**。

OrderEcho is an offline Android app for a restaurant's dedicated phone. It manages call recordings already created by Huawei's Phone app; it **does not record calls itself**.

## 使用场景 / Use case

目标设备是华为 BAC-AL00（Huawei nova 2 Plus），Android 8.0 / API 26。华为系统电话应用会将录音保存为 `.amr` 文件；OrderEcho 用于查看、搜索、播放和安全清理这些文件。

The target device is a Huawei BAC-AL00 (Huawei nova 2 Plus) running Android 8.0 / API 26. Huawei's system Phone app saves calls as `.amr` files; OrderEcho lets staff review, search, play, and safely clean up those files.

录音目录：

```text
/storage/emulated/0/Sounds/Callrecord/
```

Recording directory:

```text
/storage/emulated/0/Sounds/Callrecord/
```

## 功能 / Features

- 按月份和日期虚拟分组显示录音，不移动、不重命名华为原始文件。
- 今天的录音默认展开；较早日期保持紧凑，点击日期即可展开或收起。
- App 内播放 AMR 录音；同一时间只播放一条。
- 搜索电话号码时忽略空格、连字符、括号和 `+` 等格式字符。例如输入 `12312123` 可以找到 `123 12 123`。
- 顶部显示“通话录音 · X 条”和刷新按钮；搜索框右侧有明确的“搜索”按钮。
- 可以确认后删除单条录音。
- 可设置保留 7、30、60、90 或 180 个自然日；默认 30 天。
- 启动、每日后台任务和手动操作都会执行保留期清理。
- 完全离线：不需要账号、云端、广告或网络权限。

- Groups recordings virtually by month and date without moving or renaming Huawei files.
- Opens today's recordings by default; older dates stay compact until tapped.
- Plays AMR recordings inside the app, with one active playback at a time.
- Ignores spaces, hyphens, parentheses, `+`, and other formatting while searching phone numbers. For example, `12312123` finds `123 12 123`.
- Shows “通话录音 · X 条” with Refresh at the top and an explicit Search button beside the phone-number field.
- Deletes an individual recording only after confirmation.
- Retention choices are 7, 30, 60, 90, or 180 calendar days; the default is 30 days.
- Retention cleanup runs at app start, through a best-effort daily background job, and on request.
- Fully offline: no accounts, cloud service, advertising, or network permission.

## 不包含的功能 / Out of scope

- 不录制、接听、拒接或控制电话，不替换华为电话应用。仅为首次来电提示只读监听来电状态。
- 不上传、同步或分享录音。
- 不要求 Root。
- 不移动或重命名录音文件。

- It does not record, answer, reject, or control calls or replace Huawei's Phone app. Read-only call-state observation is used solely for the first-caller hint.
- It does not upload, sync, or share recordings.
- It does not require root access.
- It does not move or rename recording files.

## 隐私与文件安全 / Privacy and file safety

OrderEcho 没有 `INTERNET` 权限。所有读取、播放和删除操作都限制在配置的 `Sounds/Callrecord` 目录中，并且只接受该目录直属的 `.amr` 文件。删除前会再次检查文件路径和类型。

OrderEcho has no `INTERNET` permission. Reading, playback, and deletion are limited to direct-child `.amr` files in the configured `Sounds/Callrecord` directory. The path and file type are validated again immediately before deletion.

## 构建与安装 / Build and install

需要 JDK 17；Android Studio 附带的 JDK 17 可以使用。

JDK 17 is required; Android Studio's bundled JDK 17 is suitable.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Debug APK 位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

在 BAC-AL00 上安装后，请允许存储权限，然后确认能看到真实录音、播放双方声音，并验证搜索、刷新、日期展开、删除和保留期设置。

After installing on the BAC-AL00, grant storage permission. Confirm that real recordings appear, both sides of a call can be heard, and search, refresh, date expansion, deletion, and retention settings work as expected.

## 测试 / Tests

运行单元测试、生成 Debug APK 和 Android 测试 APK：

Run unit tests and assemble both the Debug APK and Android-test APK:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

仪器测试使用应用缓存目录中的隔离录音样本，不会访问或清理餐厅真实的 `Callrecord` 录音目录。连接 BAC-AL00 后，仍应执行仪器测试和人工验收。

Instrumentation tests use isolated recording fixtures in the app cache. They do not access or clean the restaurant's real `Callrecord` directory. Run the instrumentation tests and complete manual acceptance once the BAC-AL00 is connected.

## 已知后续工作 / Known follow-up

当前版本的启动清理、刷新、统计和手动清理仍会在主线程扫描文件。录音数量很大时，界面可能短暂停顿。后续将使用受生命周期管理的串行后台文件任务来处理这些操作。

The current version scans files on the main thread during startup cleanup, refresh, statistics, and manual cleanup. Very large recording archives may briefly block the interface. A lifecycle-owned serialized background file-operation layer is the planned follow-up.

## 项目文档 / Project documentation

- [交接与真机验收清单 / Handoff and physical-device acceptance](docs/HANDOFF.md)
- [架构 / Architecture](docs/ARCHITECTURE.md)
- [技术决定 / Decisions](docs/DECISIONS.md)
- [AI 协作说明 / AI agent instructions](AGENTS.md)

## 首次来电识别 / First incoming caller hint

系统历史通话中没有该号码以前的接听、未接、拒接或拦截来电时，响铃期间显示「第一次来电」。解锁时显示在屏幕顶部；锁屏时只有员工在系统无障碍设置中手动开启「锁屏顶部提示」后才使用无障碍覆盖层，否则退回 Android 高优先级、静音通知横幅。两条锁屏路径都不启动 Activity。拨出记录不算历史来电。唯一来源是本机系统 Call Log，没有客户数据库、号码持久化、联系人查询或网络。

When the local system Call Log contains no earlier incoming, missed, rejected or blocked call from a number, a `第一次来电` hint appears while ringing. It is an overlay near the top when unlocked. When locked, it uses a display-only accessibility overlay only after staff manually enable `锁屏顶部提示` in Android Accessibility Settings; otherwise it falls back to a number-free, silent high-priority Android heads-up notification. Neither locked route launches an Activity. Outgoing-only history does not count. There is no customer database, stored phone-number list, contact lookup or network access.

安装或从 D1/D2 升级后，在「设置」允许电话、通话记录、悬浮窗权限，再点击「开启来电识别」。开关默认关闭。华为 EMUI 8 还需允许自启动、关联启动、后台运行，并放宽电池优化。开启后在通知栏保留「来电识别已开启」通知；可从设置或通知栏关闭。普通重启解锁或覆盖更新后会按已保存的开关尝试恢复，仍受系统限制；手动强行停止后需要打开一次。

After installation or upgrading from D1/D2, grant Phone, Call Log and Display over other apps, then tap Enable call identification in Settings. The switch defaults off. An ongoing generic notification keeps runtime reception active; Settings and the notification both offer Stop. Allow EMUI auto/secondary/background launch and battery exceptions. Boot after unlock and app updates attempt to restore the saved enabled choice, subject to system restrictions; force-stop requires reopening the app.

后台监听使用常驻前台服务；来电查询和显示另外使用短时前台服务，其工作通知完成后自动关闭。所有通知均不含号码。解锁时的悬浮窗不接收触摸、不抢焦点；锁屏时仅发送高优先级、静音、只含「第一次来电」的系统通知横幅，不启动任何 Activity；华为原生接听和挂断界面不会被应用切走。两者都会在接听、挂断或最多 12 秒后移除。旧号码不会显示首次悬浮提示。

A persistent foreground service owns runtime reception. A separate short-lived foreground service shows a generic notification during lookup/display; neither notification contains a caller number. The unlocked overlay is non-touchable and non-focusable. The locked route is a silent, high-priority Android heads-up notification containing only `第一次来电`; it starts no Activity, so Huawei keeps its native call screen. Both disappear on answer/end and last at most 12 seconds. Returning callers do not get the first-caller overlay.

按第一条 RINGING 的接收时间减 5 秒查询历史，重复事件不移动边界。挪威八位本地、`+47` 和 `0047` 格式可匹配。权限或查询异常、未知号码不显示。由于系统不提供精确通话 ID/开始时间，异常广播延迟、五秒内快速重拨、未落库记录及系统历史删除有明确限制。锁屏、EMUI 杀后台和双卡/通话等待须真机验证，不承诺突破系统限制。

History uses the first RINGING receipt time minus five seconds; duplicates cannot move the boundary. Norwegian local eight-digit, `+47` and `0047` forms match. Unknown numbers and permission/query failures produce no hint. Public APIs expose no shared call ID/exact start timestamp, so OEM delays, very fast redials, delayed Call Log writes and deleted history have documented limits. Verify lock-screen display and EMUI background behavior on the physical phone.

完整配置、限制和真机测试步骤见 [首次来电验收清单 / First-call acceptance](docs/FIRST_CALL_TESTING.md)。