# 首次来电识别：安装与验收

## 功能和安装

目标：Huawei nova 2 Plus BAC-AL00，Android 8.0 / EMUI 8。APK：
`app/build/outputs/apk/debug/app-debug.apk`。安装更新应保留原应用数据。

首次安装后打开一次 App，进入「设置」：

1. 点击「允许电话和通话记录权限」，允许两个运行时权限。
2. 点击「允许显示在其他应用上层」，打开此应用的悬浮窗开关，返回确认三项均显示「已允许」。
3. 若永久拒绝过权限，使用「授权失败？打开应用权限设置」恢复。
4. 原有录音功能仍需存储权限；拒绝存储权限不会阻止进入来电权限设置。
5. 在 EMUI 的手机管家／电池／应用启动管理中，找到餐厅通话录音，关闭自动管理后允许自动启动、关联启动和后台运行。在系统「忽略电池优化／电池优化」中允许本应用不受限制。不同固件的名称、位置可能不同；搜索这些设置名称。切勿把省电白名单操作当成代码已保证后台唤醒的证据。
6. 用另外一部手机完成下面清单。首次安装和手动「强行停止」后，Android 要求至少主动打开 App 一次。普通重启后，先解锁手机，再测试不打开 App 的来电；不承诺解锁前的 Direct Boot。

Android 8 的 manifest PHONE_STATE 广播属于后台广播例外。每次有可用号码时，短时前台服务执行查询和管理悬浮窗，系统会显示一条无号码的低优先级工作通知，完成后消失；没有全天常驻服务，也不需要每次来电前打开 App。

## 判断规则与边界

- 唯一历史来源是本机 `CallLog.Calls`，只读，不插入、不修改、不删除通话记录；不保存客户号码或建立数据库。
- 只计入 `INCOMING_TYPE`、`MISSED_TYPE`、`REJECTED_TYPE` 和 `BLOCKED_TYPE`；拨出、语音信箱等不算以前的主动来电。
- 第一条 RINGING（即使尚无号码）固定 `ringStartedAt`；仅匹配 `DATE < ringStartedAt - 5000`。重复 RINGING 不移动边界，不重复查询、不延长提示。
- Android `PhoneNumberUtils` 做规范化／E164 格式化；挪威本地八位、`+47`、`0047` 和常用分隔符等价。完整号码比较，不能凭尾号或子串匹配跨国家号码。
- 查询在线程池执行，最少列为 NUMBER/DATE/TYPE，按时间倒序，找到匹配即停止并关闭 Cursor。权限缺失、空 Cursor 返回、异常、取消都视为无法确认，不显示首次提示。
- 无号码、Private/Unknown、特殊隐藏号码占位符均忽略。日志仅 Debug 输出状态，完全不打印号码、Intent 或可能含号码的异常。
- 悬浮窗只有「第一次来电」，顶部居中，不获得焦点、不接收触摸。OFFHOOK/IDLE 立即移除；额外每 500ms 检查当前通话状态；显示最多 12 秒，服务总计最多 15 秒。Activity 退出不会留下 Activity Window 引用。
- 电话状态广播没有与 Call Log 对应的公开通话 ID 或精确起始时间。五秒余量能排除正常时序中的当前记录，但不能证明覆盖所有 OEM 延迟、异常时钟或厂商提前回填记录；上一次来电发生在这五秒内也可能被排除。请重点验收快速重拨和第一次来电。不能将此功能作为可靠客户身份凭证。
- 删除系统历史通话后，相同号码可能再次被视为首次；系统还未写入上一通电话记录时的快速重拨同样存在限制。应用不另存号码来改变这个定义。
- 华为可能限制锁屏窗口或杀死后台应用；必须实测。应用无法突破系统强停/厂商策略。设备级 PHONE_STATE 无法精确区分双卡同时来电和通话等待；这类场景优先不显示不确定提示，不影响系统接听。

## 真机验收清单

使用专门的测试号码；不要为了测试批量清除餐厅的真实通话记录。

| 场景 | 操作 | 期望 |
|---|---|---|
| 1 新号码 | 没有该号码的系统历史来电，打入 | 正常响铃，只有一个「第一次来电」 |
| 2 再次来电 | 完成第一次后，确认系统记录已落库，再次拨入 | 无首次悬浮窗 |
| 3 曾未接/拒接 | 先形成未接、拒接（或系统拦截）记录，再拨入 | 无首次悬浮窗 |
| 4 仅曾拨出 | 餐厅此前仅主动拨给 C，C 第一次打入 | 显示首次悬浮窗 |
| 5 挪威格式 | 历史 `+4791234567`，来电本地 `91234567` 或 `004791234567` | 同一号码，无首次提示 |
| 6 隐藏号码 | 隐藏 Caller ID 后拨入 | 无提示、无崩溃 |
| 7 接听 | 新号码来电，提示出现后接听 | 提示立即消失，通话正常 |
| 8 挂断/拒接 | 新号码提示出现后由任一方结束 | 提示消失，无残留 |
| 9 后台/锁屏 | 回桌面、其他 App、锁屏分别拨入 | 系统正常来电，提示可见且不挡接听/拒接 |
| 10 重启 | 完成授权与 EMUI 配置后重启、解锁，不打开 App，拨入 | 尽可能正常唤醒；记录固件与后台设置结果 |
| 11 重复事件 | 同次来电观察 Debug 日志或测试注入重复 RINGING | 仅一次 Checking previous calls 和一次 Overlay shown |
| 12 大历史 | 系统中数千条通话记录时拨入 | 电话界面无明显卡顿；匹配即停止 |
| 13 当前记录 | 在首次 RINGING 时检查当前电话提前写入记录的固件行为 | 当前记录不能被当成历史，记录广播/DATE 时差 |
| 14 超时/晚结果 | 保持响铃超过 15 秒；查询未完成前立即接听或挂断 | 提示自行消失，晚返回结果不能重新显示 |
| 15 拒绝/撤销权限 | 分别拒绝/恢复三种权限，并在提示期间撤销 | 不崩溃、不误报，设置状态正确 |
| 16 快速连续来电 | A 挂断立即 B 打入；同号立即重拨 | B 不被上一通服务停止；记录五秒历史窗口限制 |
| 17 原功能 | 搜索、刷新、展开日期、播放、确认删除和保留期清理 | 原行为保持；录音路径和文件安全边界不变 |

## 自动验证

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\luhao\AppData\Local\Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

CallHistoryCheckerTest 使用合成 MatrixCursor，从不更改系统 Call Log；界面测试沿用应用缓存中的隔离录音样本。FirstCallOverlayManagerTest 需要测试设备先允许悬浮窗权限，否则会标为 skipped；必须报告实际执行结果。不要把编译测试 APK 等同于运行测试。

本应用只作内部安装，lint 仅豁免 Google Play 的 `ExpiredTargetSdkVersion`。其余检查仍执行；旧录音 UI 与依赖升级警告不在本次重构范围。

## 平台依据

- [Android 8 隐式广播例外](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)
- [PHONE_STATE 和重复/无号码广播](https://developer.android.com/reference/android/telephony/TelephonyManager#ACTION_PHONE_STATE_CHANGED)
- [Android 8 后台执行及短时前台服务](https://developer.android.com/about/versions/oreo/background)
- [TYPE_APPLICATION_OVERLAY 窗口层级和系统控制](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)

## 本次验证结果（2026-09-17）

- Kotlin 单元测试 61 项通过；API26 仪器测试按 13 + 1 + 1 三组全部通过。
- 调用 `assembleDebug`、`assembleDebugAndroidTest`、`lintDebug` 均成功。lint 无错误，有 20 条原有警告。
- 已用 Android 8 模拟器实际验证后台锁屏来电显示、接听移除、重复来电不显示，以及连续来电服务启动竞态。
- 华为 BAC-AL00 尚未连接，本清单的 EMUI 真机验收仍待执行。
- 调整后提示位于距可用屏幕顶部 128dp 处，避免挡住模拟器的系统号码。华为布局仍需确认。
- 15 个仪器测试必须分组运行：普通组排除 `MainActivityPermissionTest` 与 `IncomingCallServiceTest`；权限组先在 adb 中撤销存储权限，再单独运行；竞态组仅在隔离模拟器生成 GSM 来电后，以 `-e testEmulatedCall true` 单独运行。预先为测试应用开启悬浮窗，防止窗口测试被跳过。
- Android API37 预览模拟器与现有 Espresso 点击注入不兼容，未为此升级项目依赖；最终目标验证使用 API26。

## 修改文件与职责

下表中的 Kotlin 文件位于 `app/src/main/java/com/luhaoyang/orderecho/`。

| 文件 | 改动 |
|---|---|
| `calls/CallNumber.kt` | 完整号码校验、挪威本地/国际前缀等价，拒绝隐藏和异常号码 |
| `calls/AndroidCallNumber.kt` | 接入 Android PhoneNumberUtils 规范化和 E164 |
| `calls/CallHistory.kt` | 来电类型与严格时间边界过滤，三态查询结果 |
| `calls/CallHistoryChecker.kt` | 可取消后台 Call Log 查询、最小列、提前停止和 Cursor 关闭 |
| `calls/IncomingCallSession.kt` | 固定首次时间、一次处理、会话 token、过期结果抑制 |
| `calls/IncomingCallReceiver.kt` | 接收系统电话广播、记录时间、处理结束事件和启动识别 |
| `calls/IncomingCallService.kt` | 短时前台生命周期、工作线程、权限/状态复核、退出竞态保护 |
| `calls/FirstCallOverlayManager.kt` | 单个提示窗口、安全显示/隐藏与超时 |
| `calls/FirstCallPermissions.kt` | 三种权限的集中检查 |
| `ui/MainActivity.kt` | 存储回调隔离、拒绝存储也能进设置、恢复授权后初始化 |
| `ui/SettingsFragment.kt` | 来电权限状态、授权按钮、失败恢复 |
| `app/src/main/AndroidManifest.xml` | 最小新增权限、receiver 和私有 service |
| `app/src/main/res/layout/fragment_settings.xml` | 在已有设置页加入权限区 |
| `app/src/main/res/values/strings.xml` | 提示、权限说明和华为配置文案 |
| `app/build.gradle.kts` | 保留 SDK/依赖，仅豁免内部安装不适用的商店 targetSDK lint |
| 单元测试 `calls/CallNumberTest.kt` | 号码等价、不同号码/国家、异常号码 |
| 单元测试 `calls/CallHistoryTest.kt` | 历史类型、当前记录排除、早停与数千记录 |
| 单元测试 `calls/IncomingCallSessionTest.kt` | 重复广播、晚到号码、接听/挂断/超时、跨会话结果 |
| 仪器测试 `calls/CallHistoryCheckerTest.kt` | Android 号码 API、查询参数、游标关闭、失败安全处理 |
| 仪器测试 `calls/FirstCallOverlayManagerTest.kt` | 真实窗口重复显示/隐藏及超时 |
| 仪器测试 `calls/IncomingCallServiceTest.kt` | 真实服务的 A 结束、B 排队竞态，仅测试模拟器可启用 |
| 仪器测试 `ui/FirstCallSettingsTest.kt` | 无关权限回调不能覆盖设置界面 |
| 仪器测试 `ui/MainActivityPermissionTest.kt` | 存储拒绝、进入来电设置、恢复授权后可清理 |
| `AGENTS.md`、`README.md`、`docs/ARCHITECTURE.md`、`docs/DECISIONS.md`、`docs/HANDOFF.md` | 更新范围、安装说明、架构、持久决定及交接 |
| `docs/FIRST_CALL_TESTING.md`、`docs/superpowers/{specs,plans}/2026-09-17-*` | 实施方案、每文件说明、验证证据和真机验收 |