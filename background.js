// background.js - Service Worker para la extensión

let isMonitoring = false;
let monitoringInterval = null;
let config = {
  refreshInterval: 5000, // 5 segundos por defecto
  minInterval: 2000,
  maxInterval: 8000,
  autoClick: true,
  soundEnabled: true,
  notificationEnabled: true
};

// Cargar configuración al iniciar
chrome.storage.local.get(['config'], (result) => {
  if (result.config) {
    config = { ...config, ...result.config };
  }
});

// Escuchar mensajes del popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'startMonitoring') {
    startMonitoring();
    sendResponse({ status: 'started' });
  } else if (request.action === 'stopMonitoring') {
    stopMonitoring();
    sendResponse({ status: 'stopped' });
  } else if (request.action === 'getConfig') {
    sendResponse({ config });
  } else if (request.action === 'updateConfig') {
    config = { ...config, ...request.config };
    chrome.storage.local.set({ config });
    if (isMonitoring) {
      stopMonitoring();
      startMonitoring();
    }
    sendResponse({ status: 'updated' });
  } else if (request.action === 'getStatus') {
    sendResponse({ isMonitoring });
  }
  return true;
});

function getRandomInterval() {
  return Math.floor(Math.random() * (config.maxInterval - config.minInterval + 1)) + config.minInterval;
}

function startMonitoring() {
  if (isMonitoring) return;
  
  isMonitoring = true;
  
  // Notificar a los content scripts que empiecen
  chrome.tabs.query({ url: 'https://ssweb.seap.minhap.es/icpplus/citar*' }, (tabs) => {
    tabs.forEach(tab => {
      chrome.tabs.sendMessage(tab.id, { action: 'startMonitoring', config });
    });
  });
  
  chrome.storage.local.set({ isMonitoring: true });
}

function stopMonitoring() {
  if (!isMonitoring) return;
  
  isMonitoring = false;
  
  // Notificar a los content scripts que paren
  chrome.tabs.query({ url: 'https://ssweb.seap.minhap.es/icpplus/citar*' }, (tabs) => {
    tabs.forEach(tab => {
      chrome.tabs.sendMessage(tab.id, { action: 'stopMonitoring' });
    });
  });
  
  chrome.storage.local.set({ isMonitoring: false });
}

// Escuchar cuando se abre una nueva pestaña con la URL del ICP+
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url && changeInfo.url.includes('ssweb.seap.minhap.es/icpplus/citar')) {
    if (isMonitoring) {
      setTimeout(() => {
        chrome.tabs.sendMessage(tabId, { action: 'startMonitoring', config }).catch(() => {});
      }, 1000);
    }
  }
});

// Manejar alarmas para notificaciones
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === 'citaEncontrada') {
    mostrarNotificacion('¡Cita Disponible!', 'Se ha encontrado un hueco libre en el ICP+. ¡Haz clic para reservar!');
  }
});

function mostrarNotificacion(titulo, mensaje) {
  if (config.notificationEnabled) {
    chrome.notifications.create({
      type: 'basic',
      iconUrl: 'icons/icon128.png',
      title: titulo,
      message: mensaje,
      priority: 2
    });
  }
}

// Inicializar estado al cargar
chrome.storage.local.get(['isMonitoring'], (result) => {
  if (result.isMonitoring) {
    isMonitoring = true;
  }
});
