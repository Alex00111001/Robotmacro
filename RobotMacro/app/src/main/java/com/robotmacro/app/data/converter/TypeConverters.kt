package com.robotmacro.app.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.robotmacro.app.model.*

class ActionListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<MacroAction> {
        val type = object : TypeToken<List<MacroAction>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<MacroAction>): String = gson.toJson(list)
}

class TriggerConfigConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): TriggerConfig = gson.fromJson(value, TriggerConfig::class.java)

    @TypeConverter
    fun fromConfig(config: TriggerConfig): String = gson.toJson(config)
}

class LoopConfigConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): LoopConfig = gson.fromJson(value, LoopConfig::class.java)

    @TypeConverter
    fun fromConfig(config: LoopConfig): String = gson.toJson(config)
}
