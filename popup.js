// popup.js - Lógica de la interfaz del popup

let isMonitoring = false;
let config = {
  minInterval: 2000,
  maxInterval: 8000,
  soundEnabled: true,
  notificationEnabled: true,
  autoClick: true,
  autoRefresh: false
};

// Elementos del DOM
const toggleBtn = document.getElementById('toggleBtn');
const statusText = document.getElementById('statusText');
const statusDot = document.getElementById('statusDot');
const minIntervalSlider = document.getElementById('minInterval');
const maxIntervalSlider = document.getElementById('maxInterval');
const minValueDisplay = document.getElementById('minValue');
const maxValueDisplay = document.getElementById('maxValue');
const soundToggle = document.getElementById('soundToggle');
const notificationToggle = document.getElementById('notificationToggle');
const autoClickToggle = document.getElementById('autoClickToggle');
const autoRefreshToggle = document.getElementById('autoRefreshToggle');
const logArea = document.getElementById('logArea');

// Inicializar
document.addEventListener('DOMContentLoaded', () => {
  loadSettings();
  updateStatus();
  setupEventListeners();
});

function loadSettings() {
  chrome.storage.local.get(['config'], (result) => {
    if (result.config) {
      config = { ...config, ...result.config };
    }
    
    // Actualizar UI con la configuración
    minIntervalSlider.value = config.minInterval;
    maxIntervalSlider.value = config.maxInterval;
    minValueDisplay.textContent = config.minInterval;
    maxValueDisplay.textContent = config.maxInterval;
    soundToggle.checked = config.soundEnabled;
    notificationToggle.checked = config.notificationEnabled;
    autoClickToggle.checked = config.autoClick;
    autoRefreshToggle.checked = config.autoRefresh;
  });
}

function setupEventListeners() {
  // Botón de iniciar/detener
  toggleBtn.addEventListener('click', toggleMonitoring);
  
  // Sliders de intervalo
  minIntervalSlider.addEventListener('input', (e) => {
    const value = parseInt(e.target.value);
    minValueDisplay.textContent = value;
    config.minInterval = value;
    if (value > config.maxInterval) {
      config.maxInterval = value;
      maxIntervalSlider.value = value;
      maxValueDisplay.textContent = value;
    }
    saveConfig();
  });
  
  maxIntervalSlider.addEventListener('input', (e) => {
    const value = parseInt(e.target.value);
    maxValueDisplay.textContent = value;
    config.maxInterval = value;
    if (value < config.minInterval) {
      config.minInterval = value;
      minIntervalSlider.value = value;
      minValueDisplay.textContent = value;
    }
    saveConfig();
  });
  
  // Toggles
  soundToggle.addEventListener('change', (e) => {
    config.soundEnabled = e.target.checked;
    saveConfig();
  });
  
  notificationToggle.addEventListener('change', (e) => {
    config.notificationEnabled = e.target.checked;
    saveConfig();
  });
  
  autoClickToggle.addEventListener('change', (e) => {
    config.autoClick = e.target.checked;
    saveConfig();
  });
  
  autoRefreshToggle.addEventListener('change', (e) => {
    config.autoRefresh = e.target.checked;
    saveConfig();
  });
  
  // Escuchar mensajes del background script
  chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'citaEncontrada') {
      addLog(`¡CITA ENCONTRADA! ${request.slots.length} huecos disponibles`, 'success');
      updateStatus();
    }
    return true;
  });
}

function saveConfig() {
  chrome.storage.local.set({ config });
  if (isMonitoring) {
    chrome.runtime.sendMessage({ action: 'updateConfig', config });
  }
}

function toggleMonitoring() {
  if (isMonitoring) {
    stopMonitoring();
  } else {
    startMonitoring();
  }
}

function startMonitoring() {
  chrome.runtime.sendMessage({ action: 'startMonitoring' }, (response) => {
    if (response && response.status === 'started') {
      isMonitoring = true;
      updateStatus();
      addLog('Monitorización iniciada');
      
      // Verificar que haya una pestaña del ICP+ abierta
      chrome.tabs.query({ url: 'https://ssweb.seap.minhap.es/icpplus/citar*' }, (tabs) => {
        if (tabs.length === 0) {
          addLog('⚠️ No hay pestañas del ICP+ abiertas', 'error');
          addLog('Abre https://ssweb.seap.minhap.es/icpplus/citar en una pestaña');
        } else {
          addLog(`✓ Detectadas ${tabs.length} pestaña(s) del ICP+`);
        }
      });
    }
  });
}

function stopMonitoring() {
  chrome.runtime.sendMessage({ action: 'stopMonitoring' }, (response) => {
    if (response && response.status === 'stopped') {
      isMonitoring = false;
      updateStatus();
      addLog('Monitorización detenida');
    }
  });
}

function updateStatus() {
  if (isMonitoring) {
    statusText.textContent = 'Monitoreando...';
    statusDot.classList.add('active');
    toggleBtn.textContent = 'Detener Monitorización';
    toggleBtn.classList.remove('btn-primary');
    toggleBtn.classList.add('btn-danger');
  } else {
    statusText.textContent = 'Detenido';
    statusDot.classList.remove('active');
    toggleBtn.textContent = 'Iniciar Monitorización';
    toggleBtn.classList.remove('btn-danger');
    toggleBtn.classList.add('btn-primary');
  }
}

function addLog(message, type = '') {
  const entry = document.createElement('div');
  entry.className = `log-entry ${type}`;
  const timestamp = new Date().toLocaleTimeString();
  entry.textContent = `[${timestamp}] ${message}`;
  logArea.appendChild(entry);
  logArea.scrollTop = logArea.scrollHeight;
  
  // Mantener solo las últimas 20 entradas
  while (logArea.children.length > 20) {
    logArea.removeChild(logArea.firstChild);
  }
}

function updateStatus() {
  chrome.runtime.sendMessage({ action: 'getStatus' }, (response) => {
    if (response && response.isMonitoring !== undefined) {
      isMonitoring = response.isMonitoring;
      if (isMonitoring) {
        statusText.textContent = 'Monitoreando...';
        statusDot.classList.add('active');
        toggleBtn.textContent = 'Detener Monitorización';
        toggleBtn.classList.remove('btn-primary');
        toggleBtn.classList.add('btn-danger');
      } else {
        statusText.textContent = 'Detenido';
        statusDot.classList.remove('active');
        toggleBtn.textContent = 'Iniciar Monitorización';
        toggleBtn.classList.remove('btn-danger');
        toggleBtn.classList.add('btn-primary');
      }
    }
  });
}
