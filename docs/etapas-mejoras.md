# Mejoras de ACIS

Rama de trabajo: `feature/mejoras-acis`.

La rama conserva la reorganización por paquetes y las etapas 1 a 4. El 17/11/2026 se autorizó guardar y publicar este avance e integrarlo en main antes de comenzar la etapa 5.

## Estado del avance

- Etapa 1 implementada y verificada el 16/11/2026.
- Etapa 2 implementada y verificada el 17/11/2026.
- Etapa 3 implementada y verificada el 17/11/2026.
- Etapa 4 implementada y verificada el 17/11/2026.
- Etapas 5 y 6 pendientes, en el orden descrito abajo.
- Verificación inicial de la etapa 1: compilan la aplicación y la APK de pruebas; pasaron 42 pruebas unitarias y 23 pruebas Android en una instancia temporal del emulador.
- Se agregaron cuatro pruebas unitarias del formulario por pasos y tres pruebas Android para la corrección de errores, la conservación de datos entre pasos y la preparación del informe.
- En la etapa 1, Espresso se actualizó a 3.7.0 y AndroidX Test JUnit a 1.3.0 para ejecutar las pruebas de pantalla en el emulador actual. Esa etapa no agregó dependencias de producción.
- El clima usa Retrofit; los registros usan Room con esquema 9. La galería y las preferencias con DataStore están implementadas desde la etapa 4.

### Revisión de formularios del 17/11/2026

Los selectores de fecha y hora ahora se inicializan con los datos del formulario. Al editar una OT se abre su programación guardada y, al cancelar un selector, no se modifican sus valores. Se eliminaron las cinco variables de Compose que duplicaban la fecha y la hora del ViewModel.

Se agregó una prueba Android que comprueba la fecha y hora iniciales de los selectores y la conservación de los datos al cancelarlos.

La aplicación y las pruebas compilan; pasan las 42 pruebas unitarias y las tres pruebas de formularios existentes. La nueva comprobación de los diálogos quedó pendiente: el emulador mostró un bloqueo de `com.android.systemui` que impide dar foco al calendario. El problema persistió al reiniciar una instancia temporal con gráficos por software. No se ocultó ni deshabilitó la prueba. Falta ejecutarla en un emulador estable o comprobar en teléfono que editar y cancelar mantenga la programación de la OT.

### Etapa 2: Retrofit, 17/11/2026

- Se sustituyó HttpURLConnection por Retrofit 3.0.0, el conversor Gson y OkHttp 4.12.0.
- `ServicioClima` declara la petición y `ClienteClima` configura la conexión. `RespuestaClima` transforma los datos de la API en el modelo usado por la aplicación.
- Se mantienen la caché existente, sus plazos, la selección por hora UTC y las alertas. Las respuestas incompletas no generan valores de lluvia o viento inventados.
- Las llamadas tienen tiempos de espera y se cancelan junto con la corrutina. Los errores de red o de datos permiten utilizar el respaldo disponible.
- Pasan las 53 pruebas unitarias, incluidas 11 pruebas de red y conversión. MockWebServer solo se utiliza en pruebas. Las comprobaciones de la respuesta que antes estaban en Android se cubren ahora en la JVM.
- Compilan la APK debug y la APK de pruebas Android. No se ejecutaron nuevas pruebas en el emulador ni una consulta real a Open-Meteo en teléfono durante esta etapa. La comprobación pendiente del calendario sigue registrada arriba.
- No se realizó commit ni push.

## 1. Estado y formularios

- Agrupar datos, errores y mensajes de cada formulario en un estado observable.
- Usar StateFlow desde los ViewModels y conservar las reglas en domain/validation.
- Revisar creación, edición, informe, observaciones y formularios del catálogo.
- Comprobar que los errores se corrigen al escribir y que navegar no pierde datos.

## 2. Red con Retrofit

- Cambiar el cliente HTTP del clima por un servicio Retrofit y modelos de respuesta.
- Mantener los umbrales, la caché y el funcionamiento sin internet.
- Probar errores, cancelaciones y respuestas incompletas.

## 3. Persistencia con Room

- Crear entidades, DAO y una base de datos Room.
- Migrar SQLite 8 conservando órdenes, relaciones, eventos, acuerdos y revisiones de sincronización.
- Mantener las operaciones relacionadas dentro de transacciones.
- Probar la migración con datos antiguos antes de sustituir la base actual.

### Resultado de la etapa 3

- Se usa Room 2.8.4 con entidades, DAO y esquema exportado en `app/schemas`.
- Se conserva el archivo `acis_ordenes.db`. La migracion 8 a 9 conserva todas las tablas, sus identificadores, ubicaciones, informes, referencias de fotos, acuerdos y revisiones. Las bases mas antiguas pasan primero por sus migraciones anteriores.
- Si la migracion falla, no se reemplaza ni se borra la base. Los cambios de OT, eventos y revisiones siguen siendo una transaccion.
- La carga inicial y las escrituras se ejecutan fuera del hilo de interfaz. Se muestra carga o error al abrir y se evita repetir acciones mientras se guarda.
- Pasan 72 pruebas en la JVM, incluidas 19 de persistencia con Room y SQLite nativo mediante Robolectric. Los mismos casos de persistencia estan disponibles para Android, sin copiar su codigo.
- Compilan la aplicacion debug y su APK de pruebas. No se ejecuto una instalacion sobre el telefono ni se repitieron las pruebas de pantalla en el emulador durante esta etapa.
- Falta comprobar la actualizacion en el celular con registros reales, instalando desde Android Studio sin desinstalar ni borrar los datos. Sigue pendiente la prueba de los selectores descrita en la revision de la etapa 1.
- Se mantiene la rama y no se realizo commit ni push.

## 4. Galería y preferencias

- Adjuntar imágenes existentes a las evidencias de una OT.
- Copiar la imagen elegida al almacenamiento privado para conservarla.
- Manejar cancelación y archivos no válidos.
- Guardar preferencias sencillas con DataStore; los registros siguen en la base de datos.

### Resultado de la etapa 4

- Se agregó Elegir de galería al informe de una OT en visita, conservando la cámara y las categorías Antes, Después y Otra. Se utiliza el selector de Android, sin permisos generales de almacenamiento.
- Cada foto elegida se valida, reduce y copia al almacenamiento privado como JPEG. Se conserva su orientación y se limita la entrada a 20 MB. Cancelar o elegir un archivo inválido no altera las evidencias guardadas.
- Si falla Room, se elimina la copia nueva. Se cierra cada entrada y se limpian los temporales, incluyendo errores y cancelaciones. La asociación ya iniciada se completa antes de atender una cancelación.
- Mi perfil permite activar Recordar mi perfil, desactivado por defecto. Preferences DataStore conserva el último rol de demostración y técnico elegido; desactivarlo elimina el acceso recordado. Los registros y la caché del clima mantienen sus almacenes anteriores.
- Pasan 86 pruebas en la JVM: se agregaron ocho de fotos y su asociación con Room y seis de preferencias. Se comprueban conservación al borrar el original y reabrir Room, límites, archivos inválidos, cancelación, rollback y reapertura de ajustes. Las imágenes se prueban con gráficos nativos de Robolectric; DataStore utiliza almacenamiento Okio en las pruebas para permitir reemplazos de archivos en Windows.
- Compilan la aplicación debug y la APK de pruebas Android. No se ejecutó esta etapa en el teléfono ni en un emulador. Falta comprobar el selector real, cancelar y elegir fotos, la cámara y el acceso recordado al cerrar y volver a abrir. La comprobación pendiente del calendario de la etapa 1 sigue registrada arriba.
- Se conserva la estética, la rama feature/mejoras-acis y no se realizó commit ni push.

## 5. Pruebas de recorridos

- Automatizar creación y edición, visita e informe y revisión del cliente.
- Cubrir errores y operaciones asíncronas con datos controlados.
- Conservar pruebas unitarias y de base de datos existentes.
- Revisar en teléfono el teclado, la cámara, la galería y los permisos.

## 6. Entrega

- Preparar la configuración de release y el versionado.
- Generar y comprobar una APK firmada cuando se disponga de la clave de entrega.
- Mantener claves y contraseñas fuera del repositorio.
- Actualizar la documentación con el resultado real y sus limitaciones.

## Etapa posterior: backend propio

El material presenta Spring Boot como ejemplo de backend. Su integración se planificará aparte: contrato de API, usuarios reales, permisos, carga de evidencias y resolución de conflictos. Mientras tanto, el clima es la API externa real y el envío de órdenes continúa identificado como una simulación.

## Criterio de avance

Cada etapa debe compilar y pasar las pruebas relacionadas antes de iniciar la siguiente. Se mantiene la estética actual y no se cambia el comportamiento de negocio por una reorganización técnica.
