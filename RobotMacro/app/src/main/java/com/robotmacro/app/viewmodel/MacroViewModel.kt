package com.robotmacro.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.robotmacro.app.data.AppDatabase
import com.robotmacro.app.data.repository.MacroRepository
import com.robotmacro.app.model.*
import com.robotmacro.app.service.MacroAccessibilityService
import kotlinx.coroutines.launch

class MacroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MacroRepository(
        AppDatabase.getDatabase(application).macroDao(),
        AppDatabase.getDatabase(application).executionLogDao()
    )

    val allMacros = repository.allMacros.asLiveData()

    private val _recordingState = MutableLiveData<RecordingState>(RecordingState.Idle)
    val recordingState: LiveData<RecordingState> = _recordingState

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun startRecording() {
        MacroAccessibilityService.instance?.startRecording("Macro_${System.currentTimeMillis()}")
        _recordingState.value = RecordingState.Recording
    }

    fun stopRecording() {
        MacroAccessibilityService.instance?.stopRecording()
        _recordingState.value = RecordingState.Idle
    }

    fun saveMacro(macro: Macro) {
        viewModelScope.launch {
            repository.saveMacro(macro)
            _toastMessage.value = "Macro guardada"
        }
    }

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch {
            repository.deleteMacro(macro)
        }
    }

    fun executeMacro(macro: Macro) {
        viewModelScope.launch {
            val logId = repository.logExecutionStart(macro.id)
            MacroAccessibilityService.instance?.executeMacro(macro) { success ->
                viewModelScope.launch {
                    repository.logExecutionEnd(
                        logId,
                        if (success) ExecutionStatus.COMPLETED else ExecutionStatus.ERROR,
                        0
                    )
                }
            }
        }
    }

    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
    }
}
