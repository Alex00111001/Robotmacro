package com.robotmacro.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.robotmacro.app.model.*
import com.robotmacro.app.service.FloatingRecordService
import com.robotmacro.app.service.MacroAccessibilityService
import com.robotmacro.app.ui.ExecutionStatsView
import com.robotmacro.app.util.MacroJson
import com.robotmacro.app.util.RootShell
import com.robotmacro.app.viewmodel.MacroViewModel
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MacroViewModel
    private lateinit var adapter: MacroAdapter
    private lateinit var statusText: TextView
    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var btnEnable: Button
    private lateinit var statusDot: View
    private var currentMacro: Macro? = null
    private var currentMacros: List<Macro> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MacroViewModel::class.java]

        statusText = findViewById(R.id.tvStatus)
        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        btnEnable = findViewById(R.id.btnEnableService)
        statusDot = findViewById(R.id.statusDot)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvMacros)
        adapter = MacroAdapter(
            onRun = { macro -> executeMacro(macro) },
            onEdit = { macro -> openEditor(macro) },
            onDelete = { macro -> viewModel.deleteMacro(macro) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allMacros.observe(this) { macros ->
            currentMacros = macros.map { Macro(it.id, it.name, it.actions, it.trigger, it.loopConfig, it.createdAt) }
            adapter.submitList(macros)
            findViewById<TextView>(R.id.tvEmpty).visibility =
                if (macros.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.recentLogs.observe(this) { logs ->
            findViewById<ExecutionStatsView>(R.id.executionStats).setLogs(logs)
        }

        viewModel.recordingState.observe(this) { state ->
            when (state) {
                is MacroViewModel.RecordingState.Recording -> {
                    btnRecord.isEnabled = false
                    btnStop.isEnabled = true
                    statusText.text = "🔴 GRABANDO..."
                }
                else -> {
                    btnRecord.isEnabled = true
                    btnStop.isEnabled = false
                    statusText.text = "Listo para grabar"
                }
            }
        }
    }

    private fun setupListeners() {
        btnRecord.setOnClickListener {
            if (!MacroAccessibilityService.isRunning()) {
                showAccessibilityDialog()
                return@setOnClickListener
            }
            viewModel.startRecording()
        }

        btnStop.setOnClickListener {
            val actions = MacroAccessibilityService.instance?.stopRecording() ?: emptyList()
            viewModel.stopRecording()
            openEditor(Macro(
                id = UUID.randomUUID().toString(),
                name = "Macro_${System.currentTimeMillis()}",
                actions = actions,
                trigger = TriggerConfig(TriggerType.MANUAL),
                loopConfig = LoopConfig(maxIterations = 1)
            ))
        }

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            openEditor(null)
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                checkPermissions()
            } else {
                startService(Intent(this, FloatingRecordService::class.java))
                Toast.makeText(this, "Botón flotante activo", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener { exportMacrosAsJson() }
        findViewById<Button>(R.id.btnImport).setOnClickListener { importMacrosFromJson() }
    }

    private fun executeMacro(macro: Macro) {
        viewModel.executeMacro(macro)
    }


    private fun exportMacrosAsJson() {
        val json = MacroJson.exportMacros(currentMacros)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_TITLE, "robotmacro-export.json")
        }
        startActivity(Intent.createChooser(sendIntent, "Exportar macros"))
    }

    private fun importMacrosFromJson() {
        val input = android.widget.EditText(this).apply {
            minLines = 8
            hint = "Pega aquí el JSON exportado"
        }
        AlertDialog.Builder(this)
            .setTitle("Importar macros JSON")
            .setView(input)
            .setPositiveButton("Importar") { _, _ ->
                runCatching { MacroJson.importMacros(input.text.toString()) }
                    .onSuccess { macros ->
                        macros.forEach { viewModel.saveMacro(it) }
                        Toast.makeText(this, "${macros.size} macros importadas", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { Toast.makeText(this, "JSON inválido", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateRootStatus() {
        val suffix = if (RootShell.isAvailable()) " · root disponible" else " · sin root"
        statusText.text = statusText.text.toString().substringBefore(" ·") + suffix
    }

    private fun openEditor(macro: Macro?) {
        val intent = Intent(this, MacroEditorActivity::class.java)
        macro?.let { intent.putExtra("macro", it) }
        startActivity(intent)
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Activa el servicio de accesibilidad 'Robot Macro' en Ajustes del sistema.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val isRunning = MacroAccessibilityService.isRunning()
        statusDot.setBackgroundResource(if (isRunning) R.drawable.circle_green else R.drawable.circle_red)
        btnEnable.text = if (isRunning) "Desactivar" else "Activar"
        updateRootStatus()
    }
}
