package com.luhaoyang.orderecho.calls

/** Selects one presentation only; Keyguard can cover ordinary app overlays. */
internal enum class FirstCallPresentation {
    OVERLAY,
    HEADS_UP_NOTIFICATION;

    companion object {
        fun forKeyguard(isLocked: Boolean): FirstCallPresentation =
            if (isLocked) HEADS_UP_NOTIFICATION else OVERLAY
    }
}
