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

- 不录制电话、不监听通话状态，也不干涉华为电话应用。
- 不上传、同步或分享录音。
- 不要求 Root。
- 不移动或重命名录音文件。

- It does not record calls, observe call state, or interfere with Huawei's Phone app.
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
