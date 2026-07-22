# Grabadora

App Android de grabación de audio basada en la maqueta de claude.ai/design
(`Grabadora.dc.html`). Kotlin + Jetpack Compose, minSdk 26, target 35.

## Funcionalidad

- **Home**: audímetro en vivo (28 barras, pico en dBFS), cronómetro, ganancia de entrada
  (−25..+25 dB), profundidad 16/24/32 bits, formato rápido, botón de grabar con anillo
  de progreso (toque = grabar/pausar; mantener 850 ms = finalizar).
- **Formatos reales**: WAV (16/24-bit PCM o float32), M4A/AAC (MediaCodec),
  OGG/Opus (MediaCodec, Android 10+), **MP3 (libmp3lame 3.100 vendorizada)** y
  **FLAC (libFLAC 1.4.3 vendorizada)**, compiladas con el NDK para arm64-v8a,
  armeabi-v7a y x86_64.
- **Biblioteca**: búsqueda, orden (recientes/nombre/duración), filtros por etiqueta.
- **Detalle**: forma de onda con progreso y scrubbing, afinado ±0,1 s/±1 s, seek
  ±1/5/10 s, velocidad 0,5–2× sin cambiar el tono (ExoPlayer), etiquetas, renombrar,
  compartir y borrar.
- **Editor no destructivo hasta Guardar**: recortar extremos, cortar fragmentos
  (con empalme suave de 5 ms), capítulos/marcadores (remapeados tras la edición),
  ganancia + normalizar a −1 dBFS, fundidos de entrada/salida. El pipeline decodifica
  a PCM float32 en streaming, procesa y recodifica al formato original.
- **Ajustes**: micrófono, estéreo/mono, idioma ES/EN en caliente, tema oscuro/claro,
  notificación discreta, autoinicio, y **sincronización real por WebDAV, FTP y SFTP**
  con credenciales cifradas (AES-GCM + Android Keystore), «Probar conexión» y subida
  automática al finalizar (WorkManager).

## Compilar

Requiere JDK 17 (el proyecto trae `mise.toml`: `mise install`) y el Android SDK
(platform 35, NDK 25.2.9519653, CMake 3.22.1) apuntado desde `local.properties`.

```bash
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # suite de tests JVM
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Licencias de terceros

- **LAME 3.100** (LGPL): se compila y enlaza como biblioteca compartida separada
  (`libmp3lame.so`). Fuentes en `app/src/main/cpp/third_party/lame`.
- **libFLAC 1.4.3** (BSD): fuentes en `app/src/main/cpp/third_party/flac`.
