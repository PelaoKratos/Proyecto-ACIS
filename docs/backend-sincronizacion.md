# Propuesta para backend y sincronización

Este documento prepara la conversación posterior a la entrega. Todavía no se agrega servidor, autenticación ni envío real a ACIS. Las decisiones de despliegue y proveedor quedan pendientes.

## Alcance inicial

Proponemos Spring Boot con una API REST y PostgreSQL, siguiendo el enfoque de backend propio del curso. Retrofit ya está incorporado en Android. Room seguirá alimentando las pantallas y guardando el trabajo sin conexión; el servidor validará permisos, estados e información compartida. Las fotos se conservarán en almacenamiento de archivos, con sus referencias y acceso autorizado desde la API.

La primera demostración debe conectar tres dispositivos: planificador crea y asigna una OT; el técnico la recibe, acepta y completa; el cliente revisa y aprueba. Al cerrar y abrir las aplicaciones, todos deben ver la información confirmada por el servidor.

## Etapas propuestas

1. Definir usuarios reales, permisos y contrato de la API. Relacionar cada técnico con su usuario y cada cliente con sus locales. El servidor debe comprobar el acceso; elegir un rol en la pantalla no concede permisos.
2. Implementar autenticación y catálogo compartido, con pruebas del servidor. Incorporar inicio y cierre de sesión en Android y separar los datos locales por cuenta. No mezclar automáticamente los ejemplos con los registros reales.
3. Compartir OT y acciones del flujo con conexión. Usar identificadores UUID para evitar que dos celulares creen el mismo OT-1001. El código visible lo asignará el servidor; se debe acordar cómo importar las OT locales anteriores sin perder fotos e historial.
4. Agregar una cola local de operaciones y sincronización mediante WorkManager. Cada cambio y su operación pendiente se guardan juntos en Room. Los reintentos conservan un identificador único de operación para evitar duplicados al perderse una respuesta.
5. Sincronizar fotos y cambios desde otros dispositivos. Las fotos tienen su propio estado de subida; la interfaz distingue guardado local, pendiente, confirmado y error. Consultar cambios con paginación y un cursor del servidor; incluir borrados para que no reaparezcan registros eliminados.
6. Probar desconexión, reintentos, conflictos y recuperación en los tres dispositivos antes de sustituir el simulador.

## Conflictos y confirmaciones

Cada OT compartida necesita una versión del servidor. Al enviar una acción, el dispositivo informa la versión que conoce. Si cambió, el servidor devuelve un conflicto, la app conserva el trabajo local y ofrece revisar la versión reciente. No se resuelve una aprobación, reasignación o cierre sobrescribiendo por la fecha del celular.

El servidor conserva el historial de acciones y valida transiciones en una transacción. Los reintentos de la misma operación devuelven el resultado ya confirmado. Una foto no aparece como enviada hasta que su subida y asociación estén confirmadas. Un borrado remoto necesita una marca que también llegue a dispositivos que estuvieron desconectados.

El diseño local y la cola de trabajo siguen las recomendaciones de [Android para aplicaciones que funcionan sin conexión](https://developer.android.com/topic/architecture/data-layer/offline-first). Usar una cola no resuelve por sí solo autenticación, duplicados ni conflictos: esos acuerdos forman parte de la API.

## Decisiones por conversar

Antes de implementar, acordar el alojamiento del servidor, presupuesto, almacenamiento de fotos, cuentas iniciales y recuperación de acceso. Definir si el profesor exige Spring Boot u otra tecnología y qué datos locales se importarán. La primera versión puede actualizar al abrir o al solicitar sincronización; las notificaciones en tiempo real pueden incorporarse después.
