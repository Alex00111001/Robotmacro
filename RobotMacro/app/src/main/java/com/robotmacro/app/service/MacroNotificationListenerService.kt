package com.robotmacro.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.robotmacro.app.data.AppDatabase
import com.robotmacro.app.data.repository.MacroRepository
import com.robotmacro.app.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MacroNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val appContext = applicationContext
        scope.launch {
            val db = AppDatabase.getDatabase(appContext)
            val repository = MacroRepository(db.macroDao(), db.executionLogDao())
            val notificationText = sbn.notification.extras?.let { extras ->
                listOfNotNull(
                    extras.getCharSequence("android.title")?.toString(),
                    extras.getCharSequence("android.text")?.toString()
                ).joinToString(" ")
            }.orEmpty()
            repository.enabledMacros.first()
                .map { with(repository) { it.toDomain() } }
                .filter { macro ->
                    macro.trigger.type == TriggerType.NOTIFICATION_RECEIVED &&
                        (macro.trigger.targetPackage.isBlank() || macro.trigger.targetPackage == sbn.packageName) &&
                        (macro.trigger.targetActivity.isBlank() || notificationText.contains(macro.trigger.targetActivity, ignoreCase = true))
                }
                .forEach { macro -> MacroAccessibilityService.instance?.executeMacro(macro) }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
