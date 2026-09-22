package com.luhaoyang.orderecho.calls

/** Selects exactly one first-call presentation; Keyguard can cover ordinary app overlays. */
internal enum class FirstCallPresentation {
    OVERLAY,
    ACCESSIBILITY_OVERLAY,
    HEADS_UP_NOTIFICATION;

    companion object {
        fun forKeyguard(
            isLocked: Boolean,
            isAccessibilityOverlayAvailable: Boolean
        ): FirstCallPresentation = when {
            !isLocked -> OVERLAY
            isAccessibilityOverlayAvailable -> ACCESSIBILITY_OVERLAY
            else -> HEADS_UP_NOTIFICATION
        }
    }
}