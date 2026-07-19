package com.robotmacro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.robotmacro.app.data.converter.*
import com.robotmacro.app.model.*

@Entity(tableName = "macros")
@TypeConverters(
    ActionListConverter::class,
    TriggerConfigConverter::class,
    LoopConfigConverter::class
)
data class MacroEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val actions: List<MacroAction>,
    val trigger: TriggerConfig,
    val loopConfig: LoopConfig,
    val isEnabled: Boolean = true,
    val executionCount: Int = 0,
    val lastExecutionTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val macroId: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val iterationsCompleted: Int = 0,
    val status: ExecutionStatus = ExecutionStatus.RUNNING,
    val errorMessage: String? = null
)

enum class ExecutionStatus { RUNNING, COMPLETED, STOPPED_BY_USER, STOPPED_BY_CONDITION, ERROR }
