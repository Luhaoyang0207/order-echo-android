# D10：无障碍锁屏首次来电顶部提示设计

日期：2026-09-22
状态：待用户审阅后进入实现计划

## 目标

在 Huawei BAC-AL00（Android 8 / API 26）锁屏来电时，为已判定为首次来电的号码显示一个小型顶部标签 `第一次来电`。华为原生来电界面、号码、接听、挂断、短信和提醒操作必须持续可见且可直接使用。

## 已确认的边界

- 继续用既有 PHONE_STATE 与只读 Call Log 判定首次来电；不记录号码、联系人或屏幕内容，也不联网。
- 不录制、接听、拒接、拦截或控制电话，不替换华为电话应用。
- 标签只显示 `第一次来电`；不增加铃声或震动。
- 解锁时沿用现有 `TYPE_APPLICATION_OVERLAY`。锁屏时不再启动 Activity 或全屏 Intent。
- D9 的静音通知仅作为无障碍服务未开启时的被动降级。

## 证据与方案

D5 至 D8 启动 `LockedFirstCallActivity` 时，ADB 记录 Huawei `InCallActivity` 被移到后台、窗口消失并销毁表面。D9 的诊断与系统归档证明高优先级静音通知已发出，但 EMUI 把横幅压在锁屏来电界面下方。

因此 D10 使用用户手动启用的 `AccessibilityService` 所拥有的 `TYPE_ACCESSIBILITY_OVERLAY`。它不启动 Activity，窗口层级独立于普通应用 Overlay 和通知。

## 组件与数据流

### `FirstCallAccessibilityService`

- 新增非导出的 `AccessibilityService`，以 `android.permission.BIND_ACCESSIBILITY_SERVICE` 保护。
- 服务配置不请求窗口内容、手势、按键过滤或截图能力；`onAccessibilityEvent` 为空，不订阅或处理屏幕事件。
- 服务连接后拥有进程内 `FirstCallAccessibilityOverlay`，仅提供 `isAvailable()`、`show()` 和幂等 `hide()`；服务断开或销毁立即 hide。

### 显示选择

在已有 FIRST、权限和 RINGING 校验后：

1. 未锁屏：现有普通悬浮窗。
2. 已锁屏且无障碍服务已连接：无障碍 Overlay。
3. 已锁屏但服务不可用：D9 静音通知，不启动 Activity。

接听、挂断、12 秒提示超时、15 秒服务安全超时、权限变化或服务销毁时，移除对应 Overlay 和通知。清理必须可重复调用。

### 窗口

无障碍服务通过 `WindowManager` 添加 `TYPE_ACCESSIBILITY_OVERLAY`，标签 `WRAP_CONTENT`、顶部居中、只含固定字符串。窗口使用 `FLAG_NOT_FOCUSABLE`、`FLAG_NOT_TOUCHABLE`、`FLAG_NOT_TOUCH_MODAL`，不接收按键或触摸，不遮挡底部来电操作区。

系统决定最终层级；不能以服务已连接或 `addView` 成功代替 Huawei 真机可见性验收。

## 设置、隐私与安全

设置页显示“锁屏顶部提示：已开启／未开启”，入口使用 `Settings.ACTION_ACCESSIBILITY_SETTINGS`。仅用户在系统页面主动开启后，锁屏 Overlay 才可用；应用不能自动开启。

不新增网络、号码数据库、联系人访问、无障碍事件存储、录屏、截图、手势、按键过滤或电话控制。窗口只接收既有内存中的“首次”布尔结果。

## 测试与验收

- 单元测试：锁屏且服务可用选择无障碍 Overlay；未锁屏仍选普通 Overlay；服务不可用回退 D9 通知。
- Android 测试：固定标签、不可焦点/不可触摸窗口参数、重复 show/hide、服务断开和来电终止后的清理；设置入口与启用状态。
- BAC-AL00：用户手动开启服务，清除测试号码的历史来电后锁屏拨入。确认顶部仅显示 `第一次来电`，没有号码、铃声或震动；华为原生来电界面和全部接听/挂断操作保持直接可用；接听、挂断、超时或关闭服务后无残留。

## 不做的事

- 不再尝试新的 Activity、全屏通知或普通应用 Overlay。
- 不承诺在用户未开启无障碍服务或 EMUI 拒绝该窗口时显示提示。
