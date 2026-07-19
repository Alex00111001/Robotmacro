package com.robotmacro.app

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.robotmacro.app.model.*
import com.robotmacro.app.viewmodel.MacroViewModel

class MacroEditorActivity : AppCompatActivity() {

    private lateinit var viewModel: MacroViewModel
    private var existingMacro: Macro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_macro_editor)

        viewModel = ViewModelProvider(this)[MacroViewModel::class.java]
        existingMacro = intent.getParcelableExtra("macro")

        existingMacro?.let { macro ->
            findViewById<EditText>(R.id.etMacroName).setText(macro.name)
            // Configurar UI según macro existente
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            saveMacro()
        }
    }

    private fun saveMacro() {
        val name = findViewById<EditText>(R.id.etMacroName).text.toString()
        val iterations = findViewById<EditText>(R.id.etIterations).text.toString().toIntOrNull() ?: 1
        val hasStopCondition = findViewById<SwitchMaterial>(R.id.switchStopCondition).isChecked
        val stopText = if (hasStopCondition) {
            findViewById<EditText>(R.id.etStopText).text.toString()
        } else ""

        val trigger = when {
            findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbScheduled).isChecked ->
                TriggerConfig(TriggerType.SCHEDULED)
            findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbAppLaunch).isChecked ->
                TriggerConfig(TriggerType.APP_LAUNCH)
            else -> TriggerConfig(TriggerType.MANUAL)
        }

        val loop = LoopConfig(
            maxIterations = iterations,
            stopCondition = if (hasStopCondition) StopCondition(
                type = StopType.TEXT_APPEARS,
                targetText = stopText
            ) else null
        )

        val macro = Macro(
            id = existingMacro?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            actions = existingMacro?.actions ?: emptyList(),
            trigger = trigger,
            loopConfig = loop
        )

        viewModel.saveMacro(macro)
        finish()
    }
}
