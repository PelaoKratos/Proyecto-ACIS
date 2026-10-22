# Primer recorrido Android

Esta rama inicia un recorrido ejecutable en una sola aplicación Android escrita en Kotlin y Jetpack Compose. El selector de perfil es una herramienta de demostración: no autentica usuarios ni aplica permisos reales.

La navegación entre la lista, el formulario y el detalle usa `NavHost` y rutas, siguiendo el patrón mostrado en `EjercicioLayoutsConNav`. Las pantallas se desplazan y aparecen con un fundido al navegar; la lista se funde al cambiar de perfil y el estado de la OT anima sus cambios en el detalle. Se adaptó la idea de navegación del ejercicio al flujo existente de ACIS, sin incorporar sus pantallas de demostración.

## Recorrido

1. En **Planificador**, crear una OT con local, equipo, descripción, tipo de trabajo, prioridad y fecha. Abrirla y asignarla al **Técnico demo**.
2. Cambiar a **Técnico**, abrir la OT, aceptarla e iniciar la visita. La app registra el check-in.
3. Escribir diagnóstico y actividad; los materiales son opcionales. Guardar borrador o finalizar la visita. Al finalizar, la app registra el check-out y deja la OT pendiente de conformidad.
4. Cambiar a **Cliente**, abrir el informe y aprobarlo o dejar una observación. Si hay observación, el técnico puede responder y devolver la OT a revisión.

Los cambios de estado se validan en `OrdenesRepository`. Las OT se guardan en almacenamiento privado del dispositivo y permanecen tras reiniciar la app.
Las OT creadas antes de incorporar tipo y prioridad siguen disponibles; muestran «Sin especificar» en esos dos campos.

## Alcance pendiente

- Cuentas, autenticación y autorización por cliente/local.
- Servidor y sincronización entre dispositivos; la conformidad actual ocurre solo en el mismo teléfono.
- Persistencia estructurada con Room y cola de sincronización para trabajo offline.
- Fotos, ubicación, reprogramación, visita no ejecutada, clima y SLA.

El siguiente paso técnico es definir el contrato remoto y sustituir el almacenamiento de demostración por una capa de datos con Room y sincronización. La interfaz puede continuar usando los mismos estados de OT.
