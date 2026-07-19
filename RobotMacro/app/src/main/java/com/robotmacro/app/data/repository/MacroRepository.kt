package com.robotmacro.app.data.repository

import com.robotmacro.app.data.dao.*
import com.robotmacro.app.data.entity.*
import com.robotmacro.app.model.*
import kotlinx.coroutines.flow.*

class MacroRepository(
    private val macroDao: MacroDao,
    private val logDao: ExecutionLogDao
) {
    val allMacros = macroDao.getAllMacros()
    val enabledMacros = macroDao.getEnabledMacros()

    suspend fun saveMacro(macro: Macro) {
        macroDao.insertMacro(
            MacroEntity(
                id = macro.id,
                name = macro.name,
                actions = macro.actions,
                trigger = macro.trigger,
                loopConfig = macro.loopConfig
            )
        )
    }

    suspend fun getMacro(id: String): Macro? = macroDao.getMacroById(id)?.toDomain()

    suspend fun deleteMacro(macro: Macro) = macroDao.deleteMacro(macro.toEntity())

    suspend fun logExecutionStart(macroId: String): Long {
        return logDao.insertLog(
            ExecutionLogEntity(
                macroId = macroId,
                startedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun logExecutionEnd(
        logId: Long,
        status: ExecutionStatus,
        iterations: Int,
        error: String? = null
    ) {
        logDao.updateLog(logId, status = status, iterations = iterations, error = error)
    }

    fun getLogsForMacro(macroId: String) = logDao.getLogsForMacro(macroId)
    fun getRecentLogs() = logDao.getRecentLogs()

    fun MacroEntity.toDomain() = Macro(
        id = id, name = name, actions = actions,
        trigger = trigger, loopConfig = loopConfig
    )

    fun Macro.toEntity() = MacroEntity(
        id = id, name = name, actions = actions,
        trigger = trigger, loopConfig = loopConfig
    )
}
