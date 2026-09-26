# Preparación para F-Droid

La compilación candidata es la variante `release` normal:

```bash
./gradlew assembleRelease
```

El proyecto compila LAME y libFLAC desde las fuentes vendorizadas. LAME debe
seguir enlazándose como biblioteca compartida (`libmp3lame.so`) para cumplir sus
condiciones LGPL; no se debe sustituir por un enlace estático.

## Requisito pendiente: licencia del proyecto

Antes de crear `.fdroid.yml` o abrir la solicitud de inclusión, el titular del
copyright debe elegir y añadir una licencia FLOSS para Gravo. No se ha inferido
una licencia de las dependencias ni de los archivos de Gradle.

Tras añadirla, la receta debe usar la versión ya declarada y un tag inmutable:

```yaml
Categories:
  - Multimedia
License: <SPDX elegido por el titular>
SourceCode: https://github.com/borborborja/gravo
IssueTracker: https://github.com/borborborja/gravo/issues
RepoType: git
Repo: https://github.com/borborborja/gravo.git

Builds:
  - versionName: 1.1.0
    versionCode: 2
    commit: v1.1.0
    gradle:
      - yes
    output: app/build/outputs/apk/release/app-release-unsigned.apk
```

Publica primero el commit de la licencia y corta `v1.1.0` sobre el mismo
contenido que se vaya a entregar a F-Droid; después sustituye la plantilla por
`.fdroid.yml` en la raíz.
