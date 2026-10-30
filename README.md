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

La fase 1 sigue en desarrollo. El informe rápido y las evidencias múltiples ya están implementados; el cierre requiere revisar los puntos restantes del plan. La app conserva el almacenamiento local. La asignación permite elegir Técnico demo o Brian Bravo y muestra las visitas abiertas de ese día. Al entrar como Técnico se elige una identidad de demostración y solo se muestran sus órdenes. Esta elección no equivale a autenticación.

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
