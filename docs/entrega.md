# Entrega de ACIS 1.1.0

Este documento y el paquete 1.1.0 describen el avance local preparado antes de revisar la pauta EFT. No certifican que se cumpla la entrega final. Consultar docs/plan-eft.md para los requisitos adicionales de backend, roles, cuentas y cobertura. La APK anterior se conserva sin modificar.

La versión de aplicación es 1.1.0, con versionCode 2. Se conserva el identificador `com.example.proyecto_acis`, la base Room 9 y los datos existentes. No hay backend propio: los perfiles son de demostración y el envío de órdenes sigue siendo simulado.

## Compilar

Desde la carpeta del proyecto, con el JDK configurado:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintRelease
```

La APK de desarrollo está en `app/build/outputs/apk/debug/app-debug.apk`. Sin una clave configurada, release genera `app/build/outputs/apk/release/app-release-unsigned.apk`: es un archivo de comprobación y no una APK lista para instalar.

## Crear la clave y firmar

El equipo creó la clave fuera del proyecto y generó la APK firmada mediante Android Studio. Para futuras versiones, abrir Build > Generate Signed App Bundle or APK, elegir APK y seleccionar el módulo app. Usar Create new para crear la clave en una carpeta privada fuera del repositorio; por ejemplo, `C:/Firmas/ACIS/acis-release.jks`. Elegir alias y contraseñas propios y conservar una copia privada de la clave.

Para las siguientes compilaciones, copiar `keystore.properties.example` como `keystore.properties` en la raíz. Completar ruta, alias y contraseñas únicamente en el equipo. Usar `/` en las rutas Windows. No subir ese archivo ni la clave a GitHub. Ya están excluidos mediante .gitignore.

Con los cuatro valores completos, ejecutar `assembleRelease`: generará `app/build/outputs/apk/release/app-release.apk`, firmada con esa clave. Una configuración incompleta muestra un error; release nunca toma automáticamente la clave debug.

Verificar la APK con apksigner del Android SDK y guardar su huella pública. Por ejemplo, para Build Tools 36.0.0:

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/apksigner.bat" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
Get-FileHash app/build/outputs/apk/release/app-release.apk -Algorithm SHA256
```

Android exige la misma clave para actualizar una app instalada. La versión que se prueba desde Android Studio normalmente tiene firma debug; una release con otra clave no puede instalarse encima. Mantener la versión debug en el celular con registros reales y comprobar la primera release en otro dispositivo o emulador. No desinstalar para forzar el cambio y perder registros: todavía no hay recuperación desde servidor.

Esta configuración sigue la [guía de firma de Android](https://developer.android.com/studio/publish/app-signing).

## Revisión final

El equipo confirmó que la aplicación funciona en el teléfono. Las pruebas de pantalla automatizadas siguen pendientes por los bloqueos del emulador documentados en `pruebas-recorridos.md`. Después del ajuste de textos y botones de esta etapa, falta revisar de nuevo el informe con teclado abierto y confirmar que guardar, volver y finalizar funcionan al desplazarse hasta el final.

El usuario confirmó que instaló y abrió la APK firmada en un emulador. La instalación nueva estaba vacía, lo cual corresponde al almacenamiento independiente de cada dispositivo. No se afirma que esa confirmación cubra todos los recorridos, permisos o tamaños de pantalla.

## Resultado de la preparación

Se generaron las APK debug y release y compiló la APK de pruebas Android. Pasan 98 pruebas JVM. Lint release no tiene errores; conserva 32 avisos de mantenimiento y 3 sugerencias, principalmente versiones de dependencias, recursos sin uso y recomendaciones de Kotlin. No se ocultaron con una línea base ni se desactivaron comprobaciones.

Se comprobó en el manifiesto empaquetado el identificador com.example.proyecto_acis, versionCode 2, versión 1.1.0, Android mínimo 24 y target 37. Release no habilita depuración. Posteriormente el equipo firmó la APK desde Android Studio y confirmó su ejecución en emulador. La firma del archivo app/release/app-release.apk fue verificada el 18/11/2026. El ajuste visual del informe necesita revisión en el teléfono; no se repitieron las pruebas de pantalla bloqueadas por el emulador.

## APK de entrega verificada el 18 de noviembre de 2026

- Archivo de origen: app/release/app-release.apk.
- Aplicación: com.example.proyecto_acis; versión 1.1.0, versionCode 2; Android mínimo API 24.
- Verificación apksigner: firma v2 válida, un firmante. No es una firma debug configurada automáticamente por el proyecto.
- SHA-256 del APK: `44B0217F13F5300C87F62B428E42BA7BC57810FF2DDBC000D9DDB66E85741BB6`.
- SHA-256 del certificado público: `7B80E3694C3679E6E609E67B848AD9795014868D532ED781CD1A4681A29A840D`.
- Comparación con el APK release compilado: todos los archivos ZIP fuera de META-INF son idénticos. Se conservó la APK firmada original sin recompilarla ni volver a firmarla.

La clave privada y sus contraseñas no forman parte de la entrega. La firma confirma integridad e identidad del firmante; no sustituye las pruebas funcionales.

## Contenido del paquete local

`entregas/ACIS-1.1.0` contiene la APK firmada, ERS y guía en Word y PDF, código fuente ZIP, un archivo de instrucciones y las sumas SHA-256. El ZIP general reúne esos elementos. Es un paquete preparado localmente: no implica que esté subido a AVA o que los últimos cambios ya estén publicados en GitHub.

Al 18/11/2026 el equipo no ha creado un tablero y el docente no ha informado número de equipo ni fecha límite. Antes de enviar, completar ese tablero, revisar el acceso del docente y la activación individual en AVA. El encargo solicita fuente coincidente con GitHub y prohíbe modificar el repositorio entre entrega y evaluación; no se publica nada automáticamente al preparar estos archivos.

## Copia de la APK en el repositorio

Al consolidar el avance para iniciar backend-sincronizacion, se incorpora apk/ACIS-1.1.0-release.apk. Es una copia idéntica de la APK firmada verificada, con la misma huella indicada arriba. app/release y entregas permanecen excluidos para evitar publicar archivos generados adicionales. La clave privada y las contraseñas no se incorporan.
