package com.robotmacro.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.robotmacro.app.model.*
import com.robotmacro.app.service.MacroAccessibilityService
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
            adapter.submitList(macros)
            findViewById<TextView>(R.id.tvEmpty).visibility = 
                if (macros.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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
    }

    private fun executeMacro(macro: Macro) {
        MacroAccessibilityService.instance?.executeMacro(macro) { success ->
            runOnUiThread {
                // Actualizar UI
            }
        }
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
    }
}
