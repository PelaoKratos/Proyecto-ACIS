# Continuación de la Fase 2

Rama: Interface2.0. Último commit: 7627a99 (avance de interfaz y flujos). Los cambios de la Fase 2 están guardados en los archivos, sin nuevo commit ni push.

## Implementado

- Historial de eventos y reglas de transición; actualización y evento en una transacción.
- SQLite versión 6: clientes, locales, equipos y relación estable de cada OT con su equipo.
- Migración conservando órdenes, fotos y eventos anteriores.
- Selectores desde SQLite y formulario para crear o editar equipos.
- Actualización de la última mantención al aprobar, dentro de la transacción del cierre.
- Historial y mantenciones agrupados por identificador del equipo, conservando los textos históricos de las OT.

## Prioridad para retomar

1. Ejecutar HistorialDbTest en un emulador o dispositivo que permita instalar la APK de prueba. El teléfono conectado rechazó la instalación; las pruebas Android solo se han compilado.
2. Revisar en el teléfono crear y editar equipo, conservar datos al actualizar y completar una OT hasta aprobar.
3. Completar la administración de clientes y locales, con dirección y coordenadas opcionales y validación por campo.
4. Conectar Maps con la dirección del local cuando exista. Actualmente busca por nombre.
5. Revisar los pendientes de arquitectura y validación de la Fase 2 antes de pasar a clima, SLA y sincronización simulada de la Fase 3.

La modificación previa de MainActivity.kt corresponde únicamente al fin de línea y se ha mantenido fuera de los commits.
