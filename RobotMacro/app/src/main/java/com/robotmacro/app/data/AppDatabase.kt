package com.robotmacro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.robotmacro.app.data.converter.*
import com.robotmacro.app.data.dao.*
import com.robotmacro.app.data.entity.*

@Database(
    entities = [MacroEntity::class, ExecutionLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    ActionListConverter::class,
    TriggerConfigConverter::class,
    LoopConfigConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "macro_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
