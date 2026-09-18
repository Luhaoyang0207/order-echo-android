package com.luhaoyang.orderecho.calls

import android.content.Context
import com.luhaoyang.orderecho.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fixed vocabulary only: never pass a number, Intent, cursor or exception message here. */
internal enum class CallDiagnosticEvent(val label: String) {
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
    OVERLAY_FAILED("来电悬浮窗添加失败"),
    OVERLAY_TIMEOUT("来电悬浮窗达到显示时限，已移除"),
    OVERLAY_REMOVED("来电悬浮窗已移除"),
    DEADLINE("识别达到安全超时，已停止"),
    TEST_ADDED("系统已接受测试悬浮窗；是否可见需目视确认"),
    TEST_FAILED("测试悬浮窗添加失败"),
    CLEARED("已清空诊断，请进行一次来电测试")
}

/** Local debug-only bounded evidence. Async preference writes are best effort; no phone data is stored. */
internal object CallDiagnostics {
    private const val PREFERENCES = "first_call_diagnostics"
    private const val EVENTS = "events"
    private const val LIMIT = 32

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
