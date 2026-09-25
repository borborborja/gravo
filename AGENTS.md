# AGENTS.md — Grabadora

App Android de grabación de audio. Un solo módulo `:app`, paquete `com.gravo.grabadora`.
Kotlin 2.1 + Jetpack Compose (BOM), minSdk 26, compile/target 35, AGP 8.7, Gradle 8.11.

## Comandos

- `./gradlew assembleDebug` — APK en `app/build/outputs/apk/debug/`.
- `./gradlew testDebugUnitTest` — suite JVM completa.
- `./gradlew testDebugUnitTest --tests "com.gravo.grabadora.WavHeaderTest"` — un solo test.
- No hay linter/formateador (sin ktlint/detekt): la verificación es compilar + tests.

## Entorno

- JDK 17 vía `mise install` (`mise.toml` → temurin-17). El wrapper falla con otro JDK.
- **NDK 25.2.9519653 + CMake 3.22.1 son obligatorios incluso para los tests JVM**:
  `externalNativeBuild` se evalúa en la fase de configuración. CI los instala antes de
  todo (`.github/workflows/ci.yml`); reproduce ese orden en máquinas nuevas.
- `local.properties` con `sdk.dir=...` (gitignored).

## Arquitectura (lo no evidente)

- **DI manual**: `GrabadoraApp.container` → `AppContainer` (lazy, sin Hilt/Koin). Las
  dependencias globales nuevas se registran ahí.
- **Grabación**: `audio/RecordingService` (foreground, `foregroundServiceType="microphone"`)
  + `RecordingController` + `AudioCaptureEngine`.
- **Nativo** (`app/src/main/cpp`): JNI `gravocodecs` enlaza LAME 3.100 y libFLAC 1.4.3
  vendorizados en `cpp/third_party/`. **LAME es LGPL: debe seguir siendo un `.so`
  separado (`libmp3lame.so`), nunca enlace estático.** FLAC (BSD) sí va estático.
  MP3/FLAC pasan por JNI; M4A/AAC y OGG/Opus por MediaCodec.
- **Editor no destructivo hasta Guardar** (`edit/`): decodifica a PCM float32 en streaming,
  procesa y recodifica al formato original.
- **Sync** (`sync/`): WebDAV (OkHttp), FTP (commons-net), SFTP (JSch); credenciales con
  AES-GCM + Android Keystore; subida automática al finalizar con WorkManager.
- **Transcripción** (`transcription/`): contrato puro `TranscriptionProvider` + 3
  implementaciones (Whisper/OpenAI-compatible, Gemini, Deepgram); `TranscriptionManager`
  orquesta adaptación de audio, llamada y persistencia; auto opcional con
  `TranscriptionWorker` + `TranscriptionScheduler` (WorkManager). El paquete es JVM-puro
  (sin tipos Android) salvo `AudioTranscoder`/worker; tabla Room `transcripts` con FK
  CASCADE y migración v1→v2.
- Datos: Room vía KSP (`data/db/`), ajustes en DataStore (`data/settings/`).

## Tests

- Solo JVM (JUnit4 + kotlinx-coroutines-test + Turbine). **Sin Robolectric ni tests
  instrumentados**: un test que toque el framework Android no compila/funciona aquí;
  la lógica testable debe vivir en clases puras Kotlin/Java.
- Nombres de test con backticks en español: `` fun `cabecera PCM 16-bit estereo 48k`() ``.

## Releases y firma

- Publicar = tag: `git tag v1.1.0 && git push origin v1.1.0` → `release.yml` compila,
  firma y adjunta `grabadora-<tag>.apk` a la release de GitHub.
- Firma local: `keystore/keystore.properties`; en CI: env `KEYSTORE_FILE`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. **Si falta la keystore,
  `assembleRelease` sale sin firmar sin avisar** (el bloque `signingConfigs` es condicional).
- `keystore/` está gitignored y no debe commitearse jamás. Todas las releases usan la misma
  clave para permitir actualizaciones in-place; perderla = no poder publicar actualizaciones.
- `versionCode`/`versionName` se suben a mano en `app/build.gradle.kts` antes de etiquetar.

## Convenciones

- Idioma del proyecto: **español** (README, comentarios, tests, commits). UI bilingüe ES/EN
  en `res/values` + `res/values-en`; nuevas cadenas van en ambos.
- Commits: `tipo: descripción` en español (`feat:`, `fix:`, `test:`, `ci:`, `chore:`).
- Trabajar en ramas `feat/...`/`fix/...`, no directamente en `main`.
