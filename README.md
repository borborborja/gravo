# Grabadora

App Android de grabación de audio basada en la maqueta de claude.ai/design
(`Grabadora.dc.html`). Kotlin + Jetpack Compose, minSdk 26, target 35.

## Funcionalidad

- **Home**: audímetro en vivo (28 barras, pico en dBFS), cronómetro, ganancia de entrada
  (−25..+25 dB), profundidad 16/24/32 bits, formato rápido, botón de grabar con anillo
  de progreso. En Ajustes se elige si el audímetro muestra **vista previa** antes de grabar
  o se enciende **solo al grabar**, y el modo del botón: **dos botones** (grande = iniciar/parar
  y uno pequeño = pausa/reanudar) o **mantener para parar** (toque = pausar; mantener 850 ms = finalizar).
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
- **Transcripción**: convierte cualquier grabación a texto con tres proveedores
  configurables — **Whisper (OpenAI-compatible), Gemini 3.5 Transcribe y Deepgram** —,
  cada uno con endpoint y modelo propios y su **clave de API cifrada** (AES-GCM +
  Android Keystore). El texto aparece en una **pantalla dedicada editable y con
  búsqueda**, con **selector de idioma** (o Auto) antes de lanzar; el audio se adapta
  automáticamente (original → MP3 mono 64 kbps → troceado) según lo que admita el
  proveedor.

## Compilar

Requiere JDK 17 (el proyecto trae `mise.toml`: `mise install`) y el Android SDK
(platform 35, NDK 25.2.9519653, CMake 3.22.1) apuntado desde `local.properties`.

```bash
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # suite de tests JVM
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Releases

CI (`.github/workflows/ci.yml`) compila y testea en cada push. Para publicar una
versión instalable, etiqueta y empuja:

```bash
git tag v1.1.0 && git push origin v1.1.0
```

El workflow `release.yml` compila el APK **firmado con la keystore del proyecto**
(secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) y lo
adjunta a la release de GitHub. Todas las versiones van firmadas con la misma clave
(SHA-256 `ff61afe6…be064`), por lo que cada APK se instala sobre el anterior sin
desinstalar. La keystore local vive en `keystore/` (fuera de git) — **haz copia de
seguridad**: si se pierde y se pierden los secretos, no se podrán firmar
actualizaciones compatibles.

## Licencias de terceros

- **LAME 3.100** (LGPL): se compila y enlaza como biblioteca compartida separada
  (`libmp3lame.so`). Fuentes en `app/src/main/cpp/third_party/lame`.
- **libFLAC 1.4.3** (BSD): fuentes en `app/src/main/cpp/third_party/flac`.
