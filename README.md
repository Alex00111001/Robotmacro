# ICP+ Cita Automática - Extensión de Chrome

Extensión de navegador para automatizar la búsqueda de citas libres en el portal ICP+ del Ministerio de Hacienda español.

## 🎯 Características

- **Búsqueda automática**: Monitoriza constantemente la página del ICP+ en busca de huecos libres
- **Alertas múltiples**: Sonido, vibración, notificaciones y alertas visuales cuando encuentra citas
- **Intervalos aleatorios**: Evita detección usando tiempos de recarga variables
- **Clic automático**: Opcionalmente puede hacer clic automáticamente en los huecos encontrados
- **Configurable**: Ajusta intervalos, sonidos, notificaciones y comportamiento automático

## 📦 Instalación

### Paso 1: Descargar la extensión
Todos los archivos necesarios están en este directorio.

### Paso 2: Cargar en Chrome/Edge

1. Abre Chrome o Edge
2. Ve a `chrome://extensions/` (o `edge://extensions/`)
3. Activa el **"Modo de desarrollador"** (interruptor en la esquina superior derecha)
4. Haz clic en **"Cargar desempaquetada"**
5. Selecciona la carpeta `/workspace` (donde está el `manifest.json`)
6. ¡Listo! La extensión aparecerá en tu barra de extensiones

## 🚀 Cómo usar

1. **Abre la página del ICP+**: Navega a https://ssweb.seap.minhap.es/icpplus/citar?org=OIACR
2. **Completa los datos**: Rellena el formulario hasta llegar al paso del calendario
3. **Activa la extensión**: Haz clic en el icono de la extensión en tu navegador
4. **Inicia la monitorización**: Pulsa "Iniciar Monitorización"
5. **Espera la alerta**: Cuando haya un hueco libre, recibirás una alerta sonora y visual

## ⚙️ Configuración

Desde el popup de la extensión puedes ajustar:

- **Intervalo mínimo/máximo**: Tiempo entre verificaciones (en milisegundos)
- **Sonido de alerta**: Activa/desactiva el sonido cuando encuentra cita
- **Notificaciones**: Muestra notificaciones del navegador
- **Clic automático**: Hace clic automáticamente en el primer hueco encontrado
- **Auto-recargar**: Recarga la página después de varios fallos consecutivos

## 📁 Estructura de archivos

```
/workspace/
├── manifest.json       # Configuración de la extensión
├── background.js       # Service worker (lógica en segundo plano)
├── content.js          # Script que se ejecuta en la página del ICP+
├── popup.html          # Interfaz del popup
├── popup.js            # Lógica del popup
└── icons/              # Iconos de la extensión
    ├── icon16.png
    ├── icon48.png
    └── icon128.png
```

## ⚠️ Importante

- Esta extensión funciona **solo en Chrome/Edge** y derivados (Manifest V3)
- Debes tener la página del ICP+ abierta en una pestaña para que funcione
- Los intervalos muy cortos pueden causar bloqueo temporal por parte del servidor
- Usa bajo tu propia responsabilidad

## 🔧 Solución de problemas

### La extensión no detecta citas
- Asegúrate de estar en la página correcta del ICP+
- Verifica que hayas completado todos los pasos anteriores al calendario
- Revisa la consola del navegador (F12) para ver errores

### No suena la alerta
- Verifica que el sonido esté activado en la configuración
- Algunos navegadores requieren interacción previa para reproducir sonido

### La extensión se desactiva sola
- Asegúrate de no cerrar la pestaña del ICP+
- El modo de desarrollador puede requerir recargar la extensión ocasionalmente

## 📝 Notas legales

Esta extensión es una herramienta de automatización personal. El uso debe ser responsable y respetar los términos de servicio del sitio web del Ministerio de Hacienda. No garantiza la obtención de citas y debe usarse como ayuda, no como reemplazo de la interacción manual.

## 🛠️ Desarrollo

Para modificar la extensión:

1. Edita los archivos según necesites
2. En `chrome://extensions/`, haz clic en el botón de recargar junto a la extensión
3. Prueba los cambios

## 📄 Licencia

Uso personal y educativo. No comercial.
