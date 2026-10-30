# Proyecto ACIS

Aplicación Android en Kotlin para registrar órdenes de trabajo (OT) de mantenimiento. Este repositorio contiene un prototipo local desarrollado para DSY1105.

## Integrantes

- Matías Guerra
- Brian Bravo

## Docente

- Alejandro Sepúlveda

## Funciones disponibles

- Planificador: crear OT con local, equipo, descripción, tipo, prioridad, fecha y hora; asignarla a uno de los dos técnicos de demostración.
- Técnico: aceptar la OT, registrar check-in, tomar varias fotos, guardar un borrador y finalizar el informe con diagnóstico, actividad y materiales.
- Cliente: aprobar el trabajo o registrar una observación; el técnico puede responderla.
- Las OT se ordenan por su actividad más reciente y permanecen guardadas en el dispositivo mediante SQLite.
- El check-in solicita ubicación aproximada. Si no se concede el permiso o no se obtiene una posición, la visita puede continuar sin coordenadas.

La app comienza con una selección de perfil. Cada perfil tiene un inicio con sus funciones y registros disponibles. La barra inferior adapta sus destinos al rol e incluye Perfil para cambiar de acceso; la barra superior permite retroceder. El ViewModel comprueba el rol antes de ejecutar acciones sobre las OT. Son perfiles de demostración en un mismo dispositivo; todavía no hay cuentas ni sincronización con un servidor.

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

## Avance de interfaz 2.0

- Creación y edición de órdenes en tres pasos, con validación antes de avanzar y datos conservados al retroceder.
- Filtros de inicio para el planificador: todas, sin asignar, en visita y observadas.
- Estados y prioridades con colores semánticos y etiquetas de texto.
- Jornada del técnico con visitas del día ordenadas por hora; los otros trabajos se consultan en Registros.
- Acceso directo del cliente al informe pendiente. El cliente ve horas de llegada y salida, sin coordenadas.

El avance de la Fase 1 quedó guardado en el commit 7627a99. El informe rápido y las evidencias múltiples están implementados. La app conserva el almacenamiento local. La asignación permite elegir Técnico demo o Brian Bravo y muestra las visitas abiertas de ese día. Al entrar como Técnico se elige una identidad de demostración y solo se muestran sus órdenes. Esta elección no equivale a autenticación.

### Informe de visita

El informe ofrece diagnósticos y actividades frecuentes, conservando los campos libres. El borrador se guarda al salir de la pantalla o cambiar de perfil. La evidencia permite agregar varias fotos etiquetadas Antes, Después u Otra, y eliminarlas mientras la visita está activa. Las fotos anteriores siguen siendo compatibles: el campo local de evidencia conserva nombres separados por salto de línea; no se cambia el esquema SQLite.

Para revisar: iniciar una visita, escribir un informe, agregar dos fotos, salir y volver; comprobar que el borrador y las fotos permanecen. Eliminar una evidencia, finalizar y consultar el informe como cliente.

El informe permite agregar materiales por nombre y cantidad, con validación al finalizar. Las cantidades admiten coma decimal. Los materiales anteriores se conservan como texto; las filas nuevas permanecen editables al volver al borrador. El detalle muestra el tiempo de visita a partir de la llegada, y la duración final cuando existe hora de salida.

Los controles Guardar borrador y Finalizar visita permanecen bajo el contenido desplazable del informe. El formulario de OT muestra progreso animado y vuelve arriba al cambiar de paso. La revisión del cliente incluye los hitos disponibles (llegada, salida e informe y conformidad); las órdenes antiguas no tienen fecha de creación o asignación registrada, por lo que esas fechas no se inventan.

Para comprobar en el teléfono: recorrer los tres pasos sin completar campos y revisar los errores; abrir una visita activa, escribir en el último material y comprobar el teclado y los controles; finalizar y revisar los hitos desde Cliente.

La creación incluye selectores de demostración con tres locales y cinco equipos. Se puede elegir Ingresar otro para conservar el registro libre. Cambiar el local limpia la elección del equipo. La última mantención se consulta en las órdenes cerradas del mismo local y equipo. El inicio del planificador muestra órdenes abiertas con fecha programada vencida; este indicador no representa un SLA contractual.

La barra inferior adapta sus nombres al rol: Jornada e Historial para Técnico, Servicios para Cliente y Órdenes para Planificador. Perfil muestra el acceso actual y permite cambiarlo. Al crear una OT se abre la asignación de técnico, que también puede posponerse. Las tarjetas de Jornada permiten aceptar o rechazar órdenes asignadas; el rechazo pide confirmación y una orden aceptada permite abrir el registro de llegada.

La jornada destaca la visita activa o la siguiente pendiente del día. Buscar local en Maps abre una búsqueda por el nombre guardado; todavía no existe una dirección precisa de catastro para calcular una ruta. El técnico puede consultar hasta tres visitas anteriores del mismo local y equipo. Rechazar el permiso de ubicación no inicia la visita automáticamente: se puede reintentar o elegir Iniciar sin ubicación. Esta última opción inicia sin conservar una ubicación guardada previamente.

La visita en curso permite abrir Escribir informe. El informe muestra diagnóstico, actividades, materiales y evidencias; Volver a la visita guarda el borrador. Las fechas se validan en la capa de lógica y la postergación requiere una fecha y hora futuras. Los errores de campo y los colores de estado incluyen transiciones visuales.

Diagnóstico permite seleccionar Otro y describir una falla propia. Las fotos se muestran en dos columnas, con opciones de ampliar y eliminar (esta última solo durante la visita). Las miniaturas se cargan fuera del hilo de interfaz y con tamaño reducido. El borrador indica cambios pendientes o el resultado del guardado; abrir una visita sin modificarla ya no altera su última actividad mediante el autoguardado.

La asignación muestra la hora original y sugerencias (09:00, 11:00 y 15:00). Guarda la hora y el técnico en una sola actualización de la orden. Las reglas de selección están en el validador y el ViewModel; la carga diaria es informativa y no representa disponibilidad por horario ni envío de notificaciones.

El inicio de Cliente muestra hasta tres equipos con su última visita cerrada, calculada por la fecha de salida real, y el historial incluye duración en sitio cuando existen horas de llegada y salida. No se calculan próximos vencimientos de mantención porque aún no se han definido intervalos de mantenimiento por equipo.

El planificador dispone de Equipos (consulta del catálogo demo y equipos de las OT, con creación de orden precompletada) y Reportes (totales, estados y duración media de visitas con horarios válidos). El acceso a Perfil del planificador está en la barra superior. Las otras funciones de Reportes, como SLA, se mantienen para la fase 3.

En una instalación sin órdenes, Perfil del planificador permite cargar seis OT de demostración tras una confirmación. Los ejemplos abarcan seis estados y cinco equipos; no incluyen fotos ni ubicaciones inventadas. La carga es opcional, transaccional y no reemplaza registros existentes. Las acciones Foto, Materiales y Nota abren la cámara o la sección correspondiente del informe.

## Fase 2: historial de eventos

SQLite pasa de versión 4 a 5 incorporando la tabla eventos_ot. Las órdenes y fotos anteriores se conservan. Cada orden existente recibe un registro de incorporación con su estado y los horarios que ya estaban disponibles; no se reconstruyen fechas desconocidas.

Desde esta versión se registran creación, asignación, edición, aceptación, rechazo, postergación, llegada, salida e informe, observación, respuesta y aprobación. Los eventos guardan fecha, rol, actor de demostración y detalle. La modificación de la OT y el evento se escriben en una misma transacción. El repositorio aplica reglas centralizadas de transición de estado. Al eliminar una orden se elimina también su historial asociado.

El detalle presenta los últimos cinco eventos y permite desplegar el historial completo. Las coordenadas no se incluyen en los textos de eventos. Los borradores y cambios de fotos conservan su persistencia, sin generar un evento de transición de estado.

Para revisar: actualizar la app conservando datos, abrir una OT anterior y comprobar su registro previo; crear una nueva, asignar, aceptar, registrar llegada, enviar informe, observar, responder y aprobar. Cerrar y abrir la app para comprobar el historial.

Las pruebas unitarias cubren el recorrido permitido entre estados. HistorialDbTest utiliza bases y preferencias temporales con nombres únicos para comprobar las migraciones 4→6 y 5→6, reversión por fallo de evento, estado anterior incorrecto, persistencia, borrado asociado y recorrido completo. Las pruebas de dispositivo no llegaron a ejecutarse en el teléfono conectado: el sistema rechazó la instalación de la APK de prueba. Se pueden ejecutar con connectedDebugAndroidTest en un dispositivo o emulador que admita instalarla.

### Catastro persistente

La versión 6 agrega clientes, locales y equipos relacionados mediante identificadores. Se incorporan dos clientes, tres locales y cinco equipos de demostración. Los equipos de las OT anteriores se vinculan sin borrar sus textos históricos; los locales desconocidos quedan bajo Cliente por identificar. Los registros incompletos conservan su contenido aunque todavía no puedan vincularse a un equipo.

Los selectores de OT leen SQLite y filtran equipos por local. El ingreso libre sigue disponible y registra un equipo/local cuando corresponde. Equipos permite crear y editar nombre, tipo, ubicación dentro del local y condición de exterior. El formulario valida campos obligatorios y nombres repetidos en un mismo local. Un equipo con órdenes conserva su local; se puede cambiar el nombre sin alterar las OT históricas.

Aprobar una OT actualiza la última mantención del equipo con la hora de salida, dentro de la misma transacción del cierre y su evento. Una mantención más antigua no reemplaza una fecha más reciente.

Para revisar: actualizar conservando datos, abrir Equipos, agregar un equipo, comprobar sus errores y editarlo. Crear una OT para él y completar su recorrido hasta la aprobación. Comprobar la última mantención, las fotos y el historial anterior. La migración y sus relaciones todavía requieren ejecutar HistorialDbTest en Android; el teléfono conectado había bloqueado instalar las pruebas. La compilación de pruebas y la verificación local del DDL no sustituyen esa ejecución.

Esta parte incorpora las entidades y el formulario de equipos. La administración de datos de clientes y direcciones de locales continúa pendiente; sus datos actuales se conservan en el catálogo.
