package com.robotmacro.app.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.robotmacro.app.model.Macro

object MacroJson {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    fun exportMacros(macros: List<Macro>): String = gson.toJson(macros)
    fun importMacros(json: String): List<Macro> = gson.fromJson(json, object : TypeToken<List<Macro>>() {}.type)
}
