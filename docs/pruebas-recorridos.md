# Pruebas de recorridos

Rama: `feature/mejoras-acis`. Etapa 5.

## Comprobaciones automáticas

- `RecorridosViewModelTest`: creación, edición, asignación, visita sin ubicación, borrador, informe, observación, respuesta y conformidad. Comprueba también permisos de cada rol, separación entre técnicos, errores corregibles, rechazo, reasignación y postergación.
- `OperacionesPantallaTest`: evita acciones repetidas mientras se guarda, permite reintentar después de un error y libera la carga al cancelar. Utiliza esperas controladas, sin depender de una red real.
- `RecorridosRepositoryTest`: prueba rollback del informe con conservación del borrador, aceptación y llegada concurrentes y recuperación de la orden cerrada con su historial al reabrir Room.
- `RevisionClienteTest` (Android): deja una observación desde la pantalla, corrige campos vacíos, responde como técnico, cancela la confirmación y aprueba como cliente.
- Se conservan las pruebas anteriores de formularios, validaciones, Room, migraciones, API, galería y DataStore.

Para ejecutar las pruebas JVM y compilar las APK:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

Las pruebas Android se ejecutan únicamente en un emulador de pruebas. Usan registros del ejercicio y no deben ejecutarse contra el teléfono con datos reales. La revisión manual del teléfono debe hacerse instalando la app sin desinstalar ni borrar datos.

## Recorrido manual en el teléfono

| Recorrido | Qué comprobar |
| --- | --- |
| Actualización | Instalar sobre la versión anterior y comprobar órdenes, fotos, ubicaciones e historial. |
| Crear y editar | Provocar errores vacíos, corregirlos, avanzar y retroceder pasos. Editar fecha y hora; cancelar los selectores conserva la programación. |
| Asignar | Asignar a Brian; el otro técnico no debe verla. Rechazar, reasignar y postergar. Las nuevas órdenes aparecen primero. |
| Visita | Aceptar e iniciar sin ubicación; repetir con ubicación, denegar el permiso y abrir el mapa cuando haya coordenadas. |
| Informe | Escribir en los campos inferiores sin que el teclado los tape. Guardar borrador, salir y volver, y finalizar con diagnóstico y actividad. |
| Evidencias | Tomar una foto, cancelar la cámara, elegir una imagen de galería y cancelar el selector. Revisar Antes/Después/Otra, borrar el original de galería y volver a abrir ACIS. |
| Revisión | Observar con texto vacío y corregirlo, responder como técnico, cancelar una aprobación y finalmente confirmar. |
| Preferencias | Activar Recordar mi perfil, cerrar y abrir; comprobar el técnico elegido. Cambiar de perfil, desactivar la opción y volver a abrir. |
| Clima | Consultar un equipo exterior con coordenadas, probar sin internet y distinguir el respaldo de una consulta reciente. |

Registrar el dispositivo, versión de Android, fecha, recorrido, resultado y cualquier incidencia. Las comprobaciones JVM no sustituyen la revisión del teclado, permisos y selectores nativos.

## Resultado del 17/11/2026

Compilan las APK debug y de pruebas Android. Pasan 96 pruebas JVM, sin fallos ni omisiones, incluidas las diez nuevas de recorridos y operaciones asíncronas.

La nueva prueba de pantalla compila, pero no quedó aprobada. El primer intento terminó por timeout al esperar la respuesta del técnico. Se corrigió el montaje de la prueba para recrear el detalle al cambiar de rol, como hace la navegación real. También se observó un diálogo de bloqueo de `com.android.systemui`. Al repetir con gráficos del equipo, el emulador mostró un bloqueo del proceso `system` antes de completar la ejecución. Se cerraron ambas instancias temporales; la prueba corregida sigue pendiente y no se desactivó. El calendario de la etapa 1 también sigue pendiente de ejecución en un entorno estable.

Falta ejecutar las pruebas de pantalla en un emulador estable y completar la tabla manual en el teléfono. No se instaló ninguna APK ni se borraron datos del teléfono en esta etapa.

## Confirmación del equipo y ajustes de entrega

El usuario confirmó que la aplicación funciona en su teléfono y pidió reducir textos y el espacio ocupado por las acciones del informe. La etapa 6 mueve esas acciones al contenido desplazable y compacta el historial. Falta revisar específicamente este ajuste con el teclado abierto. La confirmación general no se cuenta como ejecución de las pruebas automatizadas de pantalla pendientes.
