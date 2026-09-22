package com.luhaoyang.orderecho.calls

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.luhaoyang.orderecho.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fixed vocabulary only: never pass a number, Intent, cursor or exception message here. */
internal enum class CallDiagnosticEvent(val label: String) {
    MONITOR_READY("正式来电监听已注册，持续运行中"),
    MONITOR_RINGING("正式运行时监听：收到响铃"),
    MONITOR_FAILED("正式来电监听启动失败，请重新开启并查看权限"),
    MONITOR_STOPPED("正式来电监听已停止"),
    RINGING("收到响铃广播"),
    IDLE("收到空闲／挂断广播"),
    OFFHOOK("收到接通／通话中广播"),
    PERMISSIONS_MISSING("停止：电话、通话记录或悬浮窗权限未就绪"),
    NUMBER_MISSING("广播没有提供号码，等待后续广播"),
    NUMBER_INVALID("号码格式无法识别"),
    SESSION_IGNORED("本次事件被去重，或会话已结束"),
    SERVICE_REQUESTED("已请求启动识别服务"),
    SERVICE_FAILED("识别服务启动失败"),
    SERVICE_CREATED("识别服务已创建"),
    STALE_REQUEST("停止：请求已过期或会话不匹配"),
    STATE_IDLE("状态检查：系统报告空闲，停止识别"),
    STATE_OFFHOOK("状态检查：系统报告通话中，停止识别"),
    STATE_OTHER("状态检查：系统未报告响铃，停止识别"),
    STATE_FAILED("无法读取系统通话状态"),
    QUERY_STARTED("开始查询历史来电"),
    HISTORY_FIRST("查询成功：未找到该号码的历史来电"),
    HISTORY_PREVIOUS("查询成功：找到该号码的历史来电"),
    HISTORY_UNKNOWN("无法完成通话记录查询"),
    RESULT_IGNORED("查询结果已过期或已处理"),
    OVERLAY_ADDED("系统已接受来电悬浮窗；是否可见需目视确认"),
    LOCKED_HINT_POSTED("锁屏：已发出系统首次来电提示"),
    ACCESSIBILITY_SERVICE_CONNECTED("锁屏顶部提示服务已连接"),
    ACCESSIBILITY_SERVICE_DISCONNECTED("锁屏顶部提示服务已断开"),
    ACCESSIBILITY_OVERLAY_ADDED("系统已接受锁屏顶部提示；是否可见需目视确认"),
    ACCESSIBILITY_OVERLAY_FAILED("锁屏顶部提示添加失败，已改用系统提示"),
    ACCESSIBILITY_OVERLAY_REMOVED("锁屏顶部提示已移除"),
    LOCKED_HINT_FALLBACK("锁屏顶部提示服务不可用，已改用系统提示"),
    LOCKED_HINT_NOTIFICATIONS_BLOCKED("锁屏提示：系统已关闭本应用通知"),
    LOCKED_HINT_CHANNEL_NOT_HIGH("锁屏提示：通知渠道未处于高优先级"),
    LOCKED_HINT_CHANNEL_READY("锁屏提示：通知渠道允许横幅提示"),
    LOCKED_ACTIVITY_CREATED("锁屏提示页面已创建"),
    LOCKED_ACTIVITY_STARTED("锁屏提示页面已启动"),
    LOCKED_ACTIVITY_STOPPED("锁屏提示页面已停止"),
    LOCKED_HINT_TIMEOUT("锁屏首次提示达到显示时限，已移除"),
    OVERLAY_FAILED("来电悬浮窗添加失败"),
    OVERLAY_TIMEOUT("来电悬浮窗达到显示时限，已移除"),
    OVERLAY_REMOVED("来电悬浮窗已移除"),
    DEADLINE("识别达到安全超时，已停止"),
    TEST_ADDED("系统已接受测试悬浮窗；是否可见需目视确认"),
    TEST_FAILED("测试悬浮窗添加失败"),
    PROBE_REQUESTED("已请求启动监听对照测试"),
    PROBE_START_FAILED("监听对照测试启动失败"),
    PROBE_STARTED("监听测试已启动，约 60 秒后自动停止"),
    PROBE_ALREADY_RUNNING("监听测试正在运行，未重复注册或延长"),
    PROBE_PHONE_ACCESS_ALLOWED("系统电话访问规则：允许"),
    PROBE_PHONE_ACCESS_BLOCKED("系统电话访问规则：忽略或拒绝"),
    PROBE_PHONE_ACCESS_DEFAULT("系统电话访问规则：默认，由系统判断"),
    PROBE_PHONE_ACCESS_UNKNOWN("系统电话访问规则：无法判断"),
    PROBE_RECEIVER_READY("运行时广播监听已注册"),
    PROBE_RECEIVER_FAILED("运行时广播监听注册失败"),
    PROBE_RECEIVER_RINGING("运行时广播：收到响铃"),
    PROBE_RECEIVER_IDLE("运行时广播：收到空闲／挂断"),
    PROBE_RECEIVER_OFFHOOK("运行时广播：收到通话中"),
    PROBE_LISTENER_READY("已请求注册一路电话状态回调"),
    PROBE_LISTENER_FAILED("一路电话状态回调注册失败"),
    PROBE_SIM_FAILED("无法完成额外 SIM 卡监听注册"),
    PROBE_LISTENER_RINGING("默认电话状态回调：收到响铃"),
    PROBE_SIM1_RINGING("SIM 1 电话状态回调：收到响铃"),
    PROBE_SIM2_RINGING("SIM 2 电话状态回调：收到响铃"),
    PROBE_LISTENER_IDLE("电话状态回调：空闲（注册时也可能立即回调）"),
    PROBE_LISTENER_OFFHOOK("电话状态回调：通话中"),
    PROBE_NUMBER_PRESENT("此事件带有号码（未记录号码内容）"),
    PROBE_NUMBER_MISSING("此事件没有提供号码"),
    PROBE_TIMEOUT("监听测试到时，正在停止"),
    PROBE_CLEANUP_FAILED("部分监听注销失败，已停止处理回调"),
    PROBE_STOPPED("监听测试已停止"),
    CLEARED("已清空诊断，请进行一次来电测试")
}

/** Local debug-only bounded evidence. Async preference writes are best effort; no phone data is stored. */
internal object CallDiagnostics {
    private const val PREFERENCES = "first_call_diagnostics"
    private const val EVENTS = "events"
    private const val LIMIT = 32

    fun startProbe(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        record(context, CallDiagnosticEvent.PROBE_REQUESTED)
        return try {
            // The component exists only in src/debug; release APKs contain no probe service.
            val component = context.startForegroundService(Intent().setClassName(context.packageName,
                "com.luhaoyang.orderecho.calls.CallReceptionProbeService"))
            if (component == null) record(context, CallDiagnosticEvent.PROBE_START_FAILED)
            component != null
        } catch (_: RuntimeException) {
            record(context, CallDiagnosticEvent.PROBE_START_FAILED)
            false
        }
    }

    fun observe(context: Context, changed: () -> Unit): () -> Unit {
        if (!BuildConfig.DEBUG) return {}
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == EVENTS || key == null) changed()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    @Synchronized fun record(context: Context, event: CallDiagnosticEvent) {
        if (!BuildConfig.DEBUG) return
        // Diagnostic storage failure must never interrupt call handling.
        runCatching {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val lines = prefs.getString(EVENTS, "").orEmpty().lineSequence().filter { it.isNotBlank() }.toList()
            val next = (lines + "${System.currentTimeMillis()}|${event.name}").takeLast(LIMIT)
            prefs.edit().putString(EVENTS, next.joinToString("\n")).apply()
        }
    }

    @Synchronized fun clear(context: Context) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().remove(EVENTS).apply()
        }
    }

    @Synchronized fun report(context: Context): String {
        if (!BuildConfig.DEBUG) return ""
        return runCatching {
            val format = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(EVENTS, "").orEmpty().lineSequence().mapNotNull { line ->
                    val parts = line.split('|', limit = 2)
                    val time = parts.firstOrNull()?.toLongOrNull() ?: return@mapNotNull null
                    val event = CallDiagnosticEvent.values().firstOrNull { it.name == parts.getOrNull(1) }
                        ?: return@mapNotNull null
                    "${format.format(Date(time))}  ${event.label}"
                }.toList().takeLast(LIMIT).joinToString("\n")
                .ifEmpty { "尚无诊断记录。请测试一次来电，再返回此页查看。" }
        }.getOrDefault("无法读取本地诊断记录")
    }
}
