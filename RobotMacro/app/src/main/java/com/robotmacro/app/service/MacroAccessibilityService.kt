package com.robotmacro.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.robotmacro.app.model.*
import kotlinx.coroutines.*
import java.util.UUID

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MacroService"
        var instance: MacroAccessibilityService? = null
        fun isRunning() = instance != null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRecording = false
    private var isExecuting = false
    private var currentMacroId: String? = null
    private val recordedActions = mutableListOf<MacroAction>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Servicio conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRecording) return

        val action = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                event.source?.let { node ->
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    MacroAction(
                        type = ActionType.TAP,
                        x = rect.centerX().toFloat(),
                        y = rect.centerY().toFloat(),
                        targetPackage = event.packageName?.toString() ?: "",
                        viewId = node.viewIdResourceName ?: "",
                        viewText = node.text?.toString() ?: ""
                    )
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                event.source?.let { node ->
                    MacroAction(
                        type = ActionType.INPUT_TEXT,
                        text = event.text?.joinToString("") ?: "",
                        targetPackage = event.packageName?.toString() ?: "",
                        viewId = node.viewIdResourceName ?: ""
                    )
                }
            }
            else -> null
        }
        action?.let { recordedActions.add(it) }
    }

    override fun onInterrupt() {}

    fun startRecording(macroName: String): String {
        if (isRecording || isExecuting) return ""
        isRecording = true
        recordedActions.clear()
        currentMacroId = UUID.randomUUID().toString()
        return currentMacroId!!
    }

    fun stopRecording(): List<MacroAction> {
        isRecording = false
        return recordedActions.toList()
    }

    fun executeMacro(macro: Macro, onComplete: ((Boolean) -> Unit)? = null) {
        if (isExecuting) { onComplete?.invoke(false); return }
        isExecuting = true
        scope.launch {
            var iteration = 0
            var shouldStop = false
            while (!shouldStop) {
                iteration++
                for (action in macro.actions) {
                    if (!isExecuting) break
                    executeAction(action)
                    delay(action.delayAfter)
                }
                shouldStop = when {
                    !isExecuting -> true
                    macro.loopConfig.maxIterations > 0 && iteration >= macro.loopConfig.maxIterations -> true
                    checkStopCondition(macro.loopConfig.stopCondition) -> true
                    else -> { delay(macro.loopConfig.delayBetweenIterations); false }
                }
            }
            isExecuting = false
            withContext(Dispatchers.Main) { onComplete?.invoke(true) }
        }
    }

    fun stopExecution() { isExecuting = false }

    private suspend fun executeAction(action: MacroAction) {
        when (action.type) {
            ActionType.TAP -> performTap(action.x, action.y)
            ActionType.LONG_PRESS -> performLongPress(action.x, action.y)
            ActionType.SWIPE -> performSwipe(action.x, action.y, action.x + 300, action.y)
            ActionType.INPUT_TEXT -> performInputText(action.viewId, action.text)
            ActionType.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            ActionType.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            ActionType.SCROLL -> performScroll()
            ActionType.WAIT_FOR_ELEMENT -> waitForElement(action.viewId, action.viewText)
        }
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100)).build(), null, null)
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 800)).build(), null, null)
    }

    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build(), null, null)
    }

    private fun performInputText(viewId: String, text: String) {
        rootInActiveWindow?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()?.let { node ->
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    private fun performScroll() {
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private suspend fun waitForElement(viewId: String, text: String) {
        var found = false
        val start = System.currentTimeMillis()
        while (!found && System.currentTimeMillis() - start < 10000) {
            val root = rootInActiveWindow ?: break
            found = if (viewId.isNotEmpty()) {
                root.findAccessibilityNodeInfosByViewId(viewId)?.isNotEmpty() == true
            } else {
                findNodeByText(root, text) != null
            }
            if (!found) delay(500)
        }
    }

    private fun checkStopCondition(condition: StopCondition?): Boolean {
        if (condition == null) return false
        val root = rootInActiveWindow ?: return false
        return when (condition.type) {
            StopType.TEXT_APPEARS -> findNodeByText(root, condition.targetText) != null
            StopType.TEXT_DISAPPEARS -> findNodeByText(root, condition.targetText) == null
            StopType.ELEMENT_PRESENT -> root.findAccessibilityNodeInfosByViewId(condition.targetViewId)?.isNotEmpty() == true
            StopType.ELEMENT_GONE -> root.findAccessibilityNodeInfosByViewId(condition.targetViewId)?.isEmpty() == true
            else -> false
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (root.text?.toString()?.contains(text) == true) return root
        for (i in 0 until root.childCount) {
            findNodeByText(root.getChild(i) ?: continue, text)?.let { return it }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
    }
}
