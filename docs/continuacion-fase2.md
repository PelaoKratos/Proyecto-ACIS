# Cierre de la Fase 2

Rama: Interface2.0. Fase 2 terminada: catastro, historial, validaciones y separación del flujo de catastro. El equipo confirmó que el avance funciona bien.

## Implementado

- Historial de eventos y reglas de transición; actualización y evento en una transacción.
- SQLite versión 6: clientes, locales, equipos y relación estable de cada OT con su equipo.
- Migración conservando órdenes, fotos y eventos anteriores.
- Selectores desde SQLite y formularios para crear o editar clientes, locales y equipos.
- Dirección y ubicación aproximada opcionales por local, sin mostrar coordenadas.
- Maps por ubicación del local, dirección o nombre, sin confundir el destino con la llegada del técnico.
- Actualización de la última mantención al aprobar, dentro de la transacción del cierre.
- Historial y mantenciones agrupados por identificador del equipo, conservando los textos históricos de las OT.
- CatastroViewModel separado y repositorio único compartido desde AcisApplication.

## Revisión manual

1. Entrar como Planificador y abrir Equipos > Clientes. Probar guardar vacío, crear un cliente y editar su nombre.
2. Abrir Equipos > Locales. Registrar uno asociado al cliente con dirección, sin ubicación. Probar nombres duplicados.
3. Editarlo, registrar ubicación estando en el lugar, guardarlo y volver a abrirlo. Probar también denegar el permiso y quitar la ubicación.
4. Crear un equipo en ese local. Comprobar que ambos aparezcan al crear una OT.
5. Asignar a Brian Bravo y comprobar que el otro técnico no vea esa OT.
6. Abrir Cómo llegar al local, completar la visita y aprobar como Cliente. Revisar historial y última mantención.
7. Renombrar el local. Las OT anteriores conservan el texto original y Maps utiliza la dirección actual vinculada.
8. Cerrar y abrir la app para comprobar la persistencia; revisar también los campos inferiores con teclado abierto.

## Verificaciones del 30/10/2026

- 22 pruebas unitarias sin fallos.
- 10 pruebas Android sin fallos en Medium_Phone_API_37.0, Android 17. Incluyen las migraciones 4→6 y 5→6, transacciones, catastro y recorrido completo.
- Arranque y navegación a Clientes y Locales en el emulador; creación de cliente con confirmación y errores visibles de campos obligatorios.
- Las pruebas Android utilizan bases y preferencias temporales con nombres únicos. El emulador se ejecutó en modo temporal, sin tocar el teléfono.
- El equipo confirmó el funcionamiento del avance. La lista de revisión manual se conserva para futuras regresiones. No se inició la fase 3.

La fase 3 contempla clima, SLA y sincronización simulada. No se presenta ninguna de esas funciones como implementada todavía.
