# Proyecto ACIS

> **Nueva pauta EFT revisada el 18/11/2026:** la versión local 1.1.0 no es todavía la entrega final EFT. Faltan backend propio Spring Boot, cuatro roles, cuentas completas y cobertura medida del 80 %. El diagnóstico y el orden de implementación están en [Plan EFT](docs/plan-eft.md). El paquete anterior se conserva como referencia del avance local.

Aplicación Android en Kotlin para gestionar órdenes de servicio técnico. Entrega académica de DSY1105, versión **1.1.0** (versionCode 2).

## Equipo

- Matías Guerra y Brian Bravo.
- Docente: Alejandro Sepúlveda.

## Funciones

- **Planificador:** administrar clientes, locales y equipos; crear, editar, eliminar y asignar OT; consultar clima para equipos exteriores y reportes SLA.
- **Técnico:** consultar sus asignaciones, aceptar, rechazar o postergar; iniciar una visita con ubicación opcional, guardar borrador, agregar materiales y fotos de cámara o galería y enviar el informe.
- **Cliente:** revisar informes, dejar observaciones y aprobar el trabajo. El técnico puede responder una observación antes de una nueva revisión.
- Historial por OT, evidencias Antes/Después/Otra y registros ordenados por actividad reciente.
- Persistencia local con Room 9 y opción de recordar el perfil con DataStore.
- Sincronización **simulada** con estados de error y reintento. No comparte registros entre dispositivos.

Los tres perfiles y las dos identidades técnicas son de demostración. No hay autenticación real ni aislamiento por cuentas de clientes. La API externa real es Open-Meteo; Google Maps se abre mediante un intent.

## Ejecutar en Android Studio

1. Clonar el repositorio y abrir la carpeta del proyecto.
2. Sincronizar Gradle usando el wrapper incluido y Android SDK 37. Configurar un JDK compatible con el plugin del proyecto; las comprobaciones registradas usaron JDK 25.
3. Conectar un teléfono o iniciar un emulador con Android 7.0 (API 24) o superior.
4. Ejecutar el módulo `app`.

Para probar el flujo, crear una OT como Planificador, asignarla, cambiar a Técnico y seleccionar la identidad asignada. Aceptar e iniciar la visita, completar el informe y cambiar a Cliente para observar o aprobar.

Una instalación nueva comienza vacía. Planificador > Mi perfil permite cargar órdenes de demostración cuando no hay órdenes guardadas. Las fotos deben agregarse en las visitas. Los registros y fotos de otro teléfono no se incluyen en la APK.

## Arquitectura

- `ui/screens`: pantallas por función; `ui/navigation`: rutas.
- `ui/viewmodel` y `ui/state`: acciones, formularios y estado observable.
- `ui/components` y `ui/theme`: elementos compartidos y estilo.
- `domain/model`, `domain/validation`, `domain/rules` y `domain/service`: modelos, validación y cálculos.
- `data/local`: Room y migraciones; `data/preferences`: ajustes; `data/remote`: clima; `data/repository`: operaciones; `data/sync`: simulación.
- `device`: ubicación y apertura de Maps; `session`: perfil de demostración.

Las escrituras de Room se realizan en segundo plano y guardan orden, evento y revisión en transacción. La migración conserva las bases anteriores y sus referencias de fotos. Ver [arquitectura](docs/arquitectura.md).

## Comprobaciones

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintRelease
```

Último resultado registrado: **98 pruebas JVM aprobadas**, compilación debug/release y de pruebas Android correcta. Lint release: 0 errores, 32 avisos de mantenimiento y 3 sugerencias. La prueba automatizada del calendario y la de revisión del cliente siguen pendientes por incidencias del emulador descritas en [pruebas de recorridos](docs/pruebas-recorridos.md). No se cuentan como aprobadas.

La [APK firmada 1.1.0](apk/ACIS-1.1.0-release.apk) fue creada por el equipo y su firma se verificó el 18/11/2026. El equipo confirmó instalación y apertura en emulador. Los archivos de la APK coinciden con la compilación release comprobada, salvo metadatos de firma. Ver [entrega](docs/entrega.md).

## Documentación y entrega

- [Especificación de requisitos Word](docs/ERS_ACIS.docx).
- [Guía de entrega Word](docs/Guia_entrega_ACIS.docx).
- [Cobertura del encargo y pendientes](docs/plan-parcial-2.md).
- [Historial y ramas](docs/etapas-mejoras.md), [notas de avances anteriores](docs/historial-desarrollo.md).
- [Compilación, firma y contenido del paquete](docs/entrega.md).

El paquete local se prepara en `entregas/ACIS-1.1.0`, excluido de Git junto con `app/release`. El código fuente para AVA se genera desde los archivos del proyecto, sin claves, cachés, configuración personal ni binarios de compilación.

## Colaboración y entrega académica

Repositorio: https://github.com/PelaoKratos/Proyecto-ACIS

El encargo exige planificación compartida y evidencia de participación real. El equipo confirmó el 18/11/2026 que todavía no tiene tablero compartido y que el docente no ha informado número de equipo ni fecha límite. El estado de envío a AVA no está confirmado. La [lista de tareas de cierre](docs/planificacion-cierre.md) es una preparación local y no reemplaza un tablero con participación real. No se inventan registros de colaboración ni se modifican autores de commits. Antes de enviar, el código publicado y el ZIP de fuente deben coincidir. El encargo establece que el repositorio no debe cambiar entre la entrega y la evaluación.

## Ampliación posterior

La base local 1.1.0 se conserva en main y feature/mejoras-acis. La adecuación a la EFT se trabajará en backend-sincronizacion: la pauta requiere microservicios Spring Boot, cuentas reales, cuatro roles y cobertura medida. No hay servidor propio desplegado ni envío real de OT o fotos.
