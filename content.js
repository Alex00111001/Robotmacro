// content.js - Script que se ejecuta en la página del ICP+

let isMonitoring = false;
let checkInterval = null;
let config = {};
let lastCheckTime = 0;
let consecutiveFailures = 0;
const MAX_FAILURES = 5;

// Elementos a monitorizar en la página del ICP+
const SELECTORS_TO_CHECK = [
  'select[id*="fecha"]',
  'select[id*="hora"]',
  'input[type="submit"][value*="Citar"]',
  '.calendario',
  '[class*="cita"]',
  '[id*="calendario"]',
  'table[class*="calendario"]'
];

// Escuchar mensajes del background script
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'startMonitoring') {
    config = request.config || config;
    startMonitoring();
    sendResponse({ status: 'started' });
  } else if (request.action === 'stopMonitoring') {
    stopMonitoring();
    sendResponse({ status: 'stopped' });
  } else if (request.action === 'checkPage') {
    const result = checkForAvailableSlots();
    sendResponse(result);
  }
  return true;
});

function startMonitoring() {
  if (isMonitoring) return;
  
  isMonitoring = true;
  console.log('[ICP+ Auto] Iniciando monitoreo...');
  
  // Verificar inmediatamente
  checkForAvailableSlots();
  
  // Programar siguientes verificaciones con intervalo aleatorio
  scheduleNextCheck();
}

function stopMonitoring() {
  if (!isMonitoring) return;
  
  isMonitoring = false;
  if (checkInterval) {
    clearTimeout(checkInterval);
    checkInterval = null;
  }
  console.log('[ICP+ Auto] Monitoreo detenido');
}

function scheduleNextCheck() {
  if (!isMonitoring) return;
  
  const delay = getRandomInterval();
  checkInterval = setTimeout(() => {
    checkForAvailableSlots();
    scheduleNextCheck();
  }, delay);
}

function getRandomInterval() {
  const min = config.minInterval || 2000;
  const max = config.maxInterval || 8000;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function checkForAvailableSlots() {
  const now = Date.now();
  
  // Evitar checks demasiado rápidos
  if (now - lastCheckTime < 1000) {
    return { found: false, reason: 'too_soon' };
  }
  
  lastCheckTime = now;
  
  try {
    // Verificar si estamos en la página correcta
    if (!window.location.href.includes('ssweb.seap.minhap.es/icpplus/citar')) {
      return { found: false, reason: 'wrong_page' };
    }
    
    let slotsFound = false;
    let availableSlots = [];
    
    // Método 1: Buscar selects de fecha/hora con opciones disponibles
    const dateSelects = document.querySelectorAll('select[id*="fecha"], select[name*="fecha"]');
    dateSelects.forEach(select => {
      if (select.options.length > 1) { // Más de 1 opción significa que hay fechas
        for (let i = 1; i < select.options.length; i++) { // Saltar la primera opción (normalmente "Seleccione")
          if (select.options[i].value && select.options[i].value.trim() !== '') {
            slotsFound = true;
            availableSlots.push({
              type: 'fecha',
              value: select.options[i].value,
              text: select.options[i].text
            });
          }
        }
      }
    });
    
    const timeSelects = document.querySelectorAll('select[id*="hora"], select[name*="hora"]');
    timeSelects.forEach(select => {
      if (select.options.length > 1) {
        for (let i = 1; i < select.options.length; i++) {
          if (select.options[i].value && select.options[i].value.trim() !== '') {
            slotsFound = true;
            availableSlots.push({
              type: 'hora',
              value: select.options[i].value,
              text: select.options[i].text
            });
          }
        }
      }
    });
    
    // Método 2: Buscar elementos de calendario o tablas de citas
    const calendarElements = document.querySelectorAll(
      '.calendario, [class*="calendario"], [id*="calendario"], table[class*="cita"]'
    );
    
    calendarElements.forEach(element => {
      const text = element.textContent || element.innerText;
      if (text && /\d{1,2}\/\d{1,2}\/\d{4}/.test(text)) { // Buscar formato de fecha
        slotsFound = true;
      }
      
      // Buscar enlaces o botones dentro del calendario
      const links = element.querySelectorAll('a, button, input[type="button"]');
      links.forEach(link => {
        const linkText = link.textContent || link.innerText || link.value;
        if (linkText && !linkText.includes('no disponible') && !linkText.includes('ocupado')) {
          slotsFound = true;
          availableSlots.push({
            type: 'calendario',
            value: link.href || link.id || 'unknown',
            text: linkText
          });
        }
      });
    });
    
    // Método 3: Buscar mensajes de éxito o disponibilidad
    const successMessages = document.querySelectorAll(
      '[class*="exito"], [class*="success"], [class*="disponible"], [class*="available"]'
    );
    
    successMessages.forEach(element => {
      const text = element.textContent || element.innerText;
      if (text && (text.includes('disponible') || text.includes('disponibles'))) {
        slotsFound = true;
      }
    });
    
    // Método 4: Verificar si hay botones de cita habilitados
    const submitButtons = document.querySelectorAll('input[type="submit"], button[type="submit"]');
    submitButtons.forEach(button => {
      const buttonText = (button.value || button.textContent || '').toLowerCase();
      if ((buttonText.includes('citar') || buttonText.includes('reservar') || buttonText.includes('confirmar')) 
          && !button.disabled) {
        // Verificar si el botón está realmente habilitado (no solo visualmente)
        const style = window.getComputedStyle(button);
        if (style.opacity !== '0.5' && style.pointerEvents !== 'none') {
          slotsFound = true;
          availableSlots.push({
            type: 'boton',
            value: button.id || button.name || 'submit',
            text: button.value || button.textContent
          });
        }
      }
    });
    
    if (slotsFound) {
      consecutiveFailures = 0;
      console.log('[ICP+ Auto] ¡CITAS DISPONIBLES ENCONTRADAS!', availableSlots);
      
      // Activar alertas
      triggerAlerts(availableSlots);
      
      // Opcionalmente hacer clic automático si está configurado
      if (config.autoClick && availableSlots.length > 0) {
        setTimeout(() => {
          autoClickSlot(availableSlots[0]);
        }, 500);
      }
      
      return { 
        found: true, 
        slots: availableSlots,
        message: '¡Citas disponibles encontradas!'
      };
    } else {
      consecutiveFailures++;
      console.log(`[ICP+ Auto] No hay citas disponibles (fallos consecutivos: ${consecutiveFailures})`);
      
      // Si hay muchos fallos consecutivos, podríamos recargar la página
      if (consecutiveFailures >= MAX_FAILURES && config.autoRefresh) {
        console.log('[ICP+ Auto] Demasiados fallos, recargando página...');
        window.location.reload();
        consecutiveFailures = 0;
      }
      
      return { found: false, message: 'No hay citas disponibles' };
    }
    
  } catch (error) {
    console.error('[ICP+ Auto] Error al verificar citas:', error);
    return { found: false, error: error.message };
  }
}

function triggerAlerts(slots) {
  // Sonido de alerta
  if (config.soundEnabled) {
    playAlertSound();
  }
  
  // Vibración (si el dispositivo lo soporta)
  if (navigator.vibrate) {
    navigator.vibrate([200, 100, 200, 100, 200]);
  }
  
  // Notificación visual en la página
  showVisualAlert(slots);
  
  // Enviar notificación al background script
  chrome.runtime.sendMessage({
    action: 'citaEncontrada',
    slots: slots
  });
}

function playAlertSound() {
  // Crear un sonido simple usando Web Audio API
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);
    
    oscillator.frequency.value = 800;
    oscillator.type = 'sine';
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
    
    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + 0.5);
    
    // Repetir el sonido
    setTimeout(() => {
      const osc2 = audioContext.createOscillator();
      const gain2 = audioContext.createGain();
      osc2.connect(gain2);
      gain2.connect(audioContext.destination);
      osc2.frequency.value = 1000;
      osc2.type = 'sine';
      gain2.gain.setValueAtTime(0.3, audioContext.currentTime);
      gain2.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
      osc2.start(audioContext.currentTime);
      osc2.stop(audioContext.currentTime + 0.5);
    }, 600);
  } catch (e) {
    console.warn('[ICP+ Auto] No se pudo reproducir el sonido:', e);
  }
}

function showVisualAlert(slots) {
  // Crear overlay de alerta
  const overlay = document.createElement('div');
  overlay.id = 'icp-auto-alert-overlay';
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.8);
    z-index: 999999;
    display: flex;
    justify-content: center;
    align-items: center;
    font-family: Arial, sans-serif;
  `;
  
  const alertBox = document.createElement('div');
  alertBox.style.cssText = `
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 40px;
    border-radius: 15px;
    text-align: center;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    max-width: 500px;
    animation: pulse 1s infinite;
  `;
  
  alertBox.innerHTML = `
    <h2 style="margin: 0 0 20px 0; font-size: 28px;">🎉 ¡CITA DISPONIBLE! 🎉</h2>
    <p style="font-size: 18px; margin-bottom: 30px;">Se han encontrado ${slots.length} huecos disponibles</p>
    <div style="background: rgba(255,255,255,0.2); padding: 15px; border-radius: 8px; margin-bottom: 20px; max-height: 200px; overflow-y: auto;">
      ${slots.slice(0, 5).map(slot => `<div style="margin: 5px 0;">${slot.text || slot.value}</div>`).join('')}
      ${slots.length > 5 ? `<div style="color: #ffd700;">... y ${slots.length - 5} más</div>` : ''}
    </div>
    <button id="icp-auto-close-alert" style="
      background: #4CAF50;
      color: white;
      border: none;
      padding: 15px 30px;
      font-size: 18px;
      border-radius: 8px;
      cursor: pointer;
      font-weight: bold;
    ">¡RESERVAR AHORA!</button>
  `;
  
  overlay.appendChild(alertBox);
  document.body.appendChild(overlay);
  
  // Animación CSS
  const style = document.createElement('style');
  style.textContent = `
    @keyframes pulse {
      0% { transform: scale(1); }
      50% { transform: scale(1.05); }
      100% { transform: scale(1); }
    }
  `;
  document.head.appendChild(style);
  
  // Manejar clic en el botón
  document.getElementById('icp-auto-close-alert').addEventListener('click', () => {
    overlay.remove();
    style.remove();
  });
  
  // Cerrar automáticamente después de 30 segundos
  setTimeout(() => {
    if (overlay.parentNode) {
      overlay.remove();
      style.remove();
    }
  }, 30000);
}

function autoClickSlot(slot) {
  try {
    let element = null;
    
    if (slot.type === 'boton') {
      element = document.getElementById(slot.value) || 
                document.querySelector(`button[value="${slot.value}"]`) ||
                document.querySelector(`input[type="submit"][value*="${slot.value}"]`);
    } else if (slot.type === 'fecha' || slot.type === 'hora') {
      const select = document.querySelector('select[id*="fecha"], select[name*="fecha"], select[id*="hora"], select[name*="hora"]');
      if (select) {
        select.value = slot.value;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        element = select;
      }
    } else if (slot.type === 'calendario') {
      element = document.querySelector(`a[href="${slot.value}"]`) ||
                document.getElementById(slot.value);
    }
    
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setTimeout(() => {
        element.click();
        console.log('[ICP+ Auto] Clic automático realizado en:', slot);
      }, 1000);
    }
  } catch (error) {
    console.error('[ICP+ Auto] Error en clic automático:', error);
  }
}

// Verificar automáticamente cuando la página carga completamente
if (document.readyState === 'complete') {
  // La página ya cargó, podemos empezar a verificar
  chrome.storage.local.get(['isMonitoring', 'config'], (result) => {
    if (result.isMonitoring) {
      config = result.config || config;
      startMonitoring();
    }
  });
} else {
  window.addEventListener('load', () => {
    chrome.storage.local.get(['isMonitoring', 'config'], (result) => {
      if (result.isMonitoring) {
        config = result.config || config;
        startMonitoring();
      }
    });
  });
}

console.log('[ICP+ Auto] Content script cargado correctamente');
