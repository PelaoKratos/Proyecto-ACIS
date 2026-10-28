# Proyecto ACIS

Aplicación Android en Kotlin para registrar órdenes de trabajo (OT) de mantenimiento. Este repositorio contiene un prototipo local desarrollado para DSY1105.

## Integrantes

- Matías Guerra
- Brian Bravo

## Docente

- Alejandro Sepúlveda

## Funciones disponibles

- Planificador: crear OT con local, equipo, descripción, tipo, prioridad, fecha y hora; asignarla al técnico de demostración.
- Técnico: aceptar la OT, registrar check-in, tomar una foto, guardar un borrador y finalizar el informe con diagnóstico, actividad y materiales.
- Cliente: aprobar el trabajo o registrar una observación; el técnico puede responderla.
- Las OT se ordenan por su actividad más reciente y permanecen guardadas en el dispositivo mediante SQLite.
- El check-in solicita ubicación aproximada. Si no se concede el permiso o no se obtiene una posición, la visita puede continuar sin coordenadas.

La app comienza con una selección de perfil. Cada perfil tiene un inicio con sus funciones y registros disponibles. La barra inferior permite ir a Inicio o Registros; la barra superior permite retroceder o cambiar de perfil. El ViewModel comprueba el rol antes de ejecutar acciones sobre las OT. Son perfiles de demostración en un mismo dispositivo; todavía no hay cuentas ni sincronización con un servidor.

## Cómo ejecutar

1. Clonar el repositorio y abrir la carpeta `Proyecto-ACIS` en Android Studio.
2. Esperar la sincronización de Gradle. El proyecto usa el Gradle Wrapper incluido y Android SDK 37.
3. Conectar un teléfono Android o iniciar un emulador con Android 7.0 (API 24) o superior.
4. Ejecutar el módulo `app` desde Android Studio.

Para recorrer el flujo: crear una OT como Planificador, asignarla, cambiar a Técnico para aceptar e iniciar la visita, completar el informe y cambiar a Cliente para aprobar u observar. La cámara del sistema se abre desde el informe técnico. La ubicación se solicita al iniciar la visita.

## Estructura principal

- `app/src/main/java/com/example/proyecto_acis/ui`: pantallas Compose y estado de presentación.
- `app/src/main/java/com/example/proyecto_acis/domain`: reglas de validación.
- `app/src/main/java/com/example/proyecto_acis/data`: modelo, base de datos y repositorio de OT.
- `docs`: requisitos, flujo y planificación del parcial.

Las funciones principales se trabajan en ramas que pueden reutilizarse para cambios relacionados. La documentación y los ajustes pequeños se hacen en `main`. Las ramas de las etapas principales se conservan; los commits de las ramas integradas también se pueden consultar desde el historial de `main`.

## Alcance actual

Los datos y las fotos se guardan solo en el dispositivo. Cambiar de teléfono o borrar los datos de la app elimina esos registros. Los perfiles son de demostración y no restringen el acceso mediante inicio de sesión. Las funciones de sincronización, clima y SLA descritas en la planificación aún no están implementadas.
