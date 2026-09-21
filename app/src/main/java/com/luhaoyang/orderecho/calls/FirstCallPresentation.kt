package com.luhaoyang.orderecho.calls

/** Selects one presentation only; system call UI can cover ordinary overlays on Keyguard. */
internal enum class FirstCallPresentation {
    OVERLAY,
    SYSTEM_NOTIFICATION;

    companion object {
        fun forKeyguard(isLocked: Boolean): FirstCallPresentation =
            if (isLocked) SYSTEM_NOTIFICATION else OVERLAY
    }
}
