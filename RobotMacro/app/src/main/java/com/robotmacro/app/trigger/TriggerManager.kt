package com.robotmacro.app.trigger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.robotmacro.app.model.Macro
import com.robotmacro.app.model.TriggerType
import com.robotmacro.app.service.MacroAccessibilityService

class TriggerManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun registerTrigger(macro: Macro) {
        when (macro.trigger.type) {
            TriggerType.SCHEDULED -> scheduleTimeTrigger(macro)
            TriggerType.APP_LAUNCH -> registerAppLaunchTrigger(macro)
            else -> {}
        }
    }

    private fun scheduleTimeTrigger(macro: Macro) {
        val intent = Intent(context, TimeTriggerReceiver::class.java).apply {
            putExtra("MACRO_ID", macro.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, macro.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, macro.trigger.timeMillis, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, macro.trigger.timeMillis, pendingIntent)
        }
    }

    private fun registerAppLaunchTrigger(macro: Macro) {
        context.getSharedPreferences("triggers", Context.MODE_PRIVATE)
            .edit().putString(macro.id, macro.trigger.targetPackage).apply()
    }

    fun cancelTrigger(macroId: String) {
        val intent = Intent(context, TimeTriggerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, macroId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}

class TimeTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val macroId = intent.getStringExtra("MACRO_ID") ?: return
        Log.i("Trigger", "Ejecutando macro: $macroId")
        // Recuperar de Room y ejecutar
    }
}
