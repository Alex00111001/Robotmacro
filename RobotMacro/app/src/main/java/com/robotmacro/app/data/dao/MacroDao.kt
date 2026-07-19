package com.robotmacro.app.data.dao

import androidx.room.*
import com.robotmacro.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY createdAt DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE isEnabled = 1")
    fun getEnabledMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :macroId")
    suspend fun getMacroById(macroId: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity)

    @Update
    suspend fun updateMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    @Query("UPDATE macros SET executionCount = executionCount + 1, lastExecutionTime = :time WHERE id = :macroId")
    suspend fun incrementExecutionCount(macroId: String, time: Long = System.currentTimeMillis())
}

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs WHERE macroId = :macroId ORDER BY startedAt DESC")
    fun getLogsForMacro(macroId: String): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs ORDER BY startedAt DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ExecutionLogEntity>>

    @Insert
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Query("UPDATE execution_logs SET finishedAt = :time, status = :status, iterationsCompleted = :iterations, errorMessage = :error WHERE id = :logId")
    suspend fun updateLog(
        logId: Long,
        time: Long = System.currentTimeMillis(),
        status: ExecutionStatus,
        iterations: Int,
        error: String? = null
    )
}
