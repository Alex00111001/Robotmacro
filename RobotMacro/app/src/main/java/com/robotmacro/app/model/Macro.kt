package com.robotmacro.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Macro(
    val id: String,
    val name: String,
    val actions: List<MacroAction>,
    val trigger: TriggerConfig,
    val loopConfig: LoopConfig,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class MacroAction(
    val type: ActionType,
    val x: Float = 0f,
    val y: Float = 0f,
    val text: String = "",
    val delayAfter: Long = 500,
    val targetPackage: String = "",
    val viewId: String = "",
    val viewText: String = ""
) : Parcelable

enum class ActionType {
    TAP, LONG_PRESS, SWIPE, INPUT_TEXT, BACK, HOME, WAIT_FOR_ELEMENT, SCROLL
}

@Parcelize
data class TriggerConfig(
    val type: TriggerType,
    val timeMillis: Long = 0,
    val targetPackage: String = "",
    val targetActivity: String = "",
    val intervalMinutes: Int = 0
) : Parcelable

enum class TriggerType {
    MANUAL, SCHEDULED, APP_LAUNCH, UI_ELEMENT_APPEARS, NOTIFICATION_RECEIVED
}

@Parcelize
data class LoopConfig(
    val maxIterations: Int = 1,
    val stopCondition: StopCondition? = null,
    val delayBetweenIterations: Long = 1000
) : Parcelable

@Parcelize
data class StopCondition(
    val type: StopType,
    val targetText: String = "",
    val targetViewId: String = "",
    val timeoutMillis: Long = 30000
) : Parcelable

enum class StopType {
    TEXT_APPEARS, TEXT_DISAPPEARS, ELEMENT_PRESENT, ELEMENT_GONE, ITERATION_COUNT, TIME_ELAPSED
}
