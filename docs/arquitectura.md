# Arquitectura y ubicación de archivos

ACIS usa MVVM: la pantalla recibe acciones, el ViewModel coordina estado y validación, y el repositorio accede a las fuentes de datos. Los paquetes permiten encontrar cada responsabilidad sin buscar pantallas dentro de un archivo de navegación.

## Paquetes

Bajo `app/src/main/java/com/example/proyecto_acis`:

```text
MainActivity.kt                   Inicio de la actividad y tema
AcisApplication.kt                Repositorios compartidos
session/                          Perfil y técnico de demostración
ui/
  navigation/                     NavHost, rutas y barra de navegación
  screens/
    inicio/                       Panel inicial según perfil
    perfil/                       Selección y consulta de perfil
    ordenes/                      Listado, creación y detalle de OT
    catalogo/                     Clientes, locales y equipos
    reportes/                     Reporte mensual y actividad general
  viewmodel/                      Estado y acciones de cada flujo
  state/                          Modelos del estado de los formularios
  components/                     Campos, secciones, estados, fotos y avisos
  theme/                          Colores, tipografía y formas
domain/
  model/                          OrdenTrabajo, catastro y eventos
  validation/                     Formularios y errores por campo
  rules/                          Transiciones y orden de OT del técnico
  service/                        Cálculos de SLA, clima, tiempos y resúmenes
data/
  local/                          Coordinación de Room y caché del clima
    room/                         Base de datos y conversión de modelos
      dao/                        Consultas y operaciones de cada grupo
      entity/                     Tablas de órdenes, catastro y registros
    migration/                    Migración a Room y versiones anteriores
  remote/                         Retrofit, servicio y respuesta de Open-Meteo
  serialization/                  Conversión JSON del clima
  repository/                     Coordinación de las fuentes de datos
  sync/                           Contrato y simulación de sincronización
  demo/                           Datos de demostración
device/
  location/                       Consulta puntual de ubicación
  maps/                           Apertura de Maps mediante intent
```

## Recorrido para estudiar el código

1. `MainActivity` obtiene los ViewModels y abre `ui/navigation/AcisApp.kt`.
2. La navegación selecciona una pantalla de `ui/screens` y le entrega el estado y las acciones necesarias.
3. `CrearOrden.kt` llama a `OrdenesViewModel.crear()`; las reglas del formulario están en `domain/validation/ValidadorOt.kt`.
4. `data/repository/OrdenesRepository.kt` prepara la operación; `data/local/OrdenesDb.kt` guarda OT, evento y revisión pendiente en una transacción.
5. El repositorio actualiza StateFlow y Compose observa el resultado para actualizar la pantalla.

`DetalleOrden.kt` contiene el flujo de visita, informe y revisión. Los campos, badges, evidencias e historial reutilizables están fuera de esa pantalla en `ui/components`. El ViewModel de órdenes conserva secciones breves para acceso demo, informe, validación, creación, listados y acciones.

## Criterios para agregar código

- Una pantalla nueva pertenece a la carpeta de su función; la navegación solo conecta destinos.
- Un componente usado en varias pantallas pertenece a `ui/components`.
- Validaciones y cálculos no dependen de componentes Compose.
- El acceso a Room se mantiene en `data/local`, sin consultas desde pantallas y fuera del hilo de la interfaz.
- Las pruebas conservan el paquete de la responsabilidad que comprueban, en `src/test` o `src/androidTest`.

## Estado de formularios

`EstadoCrearOrden` agrupa los datos de creación o edición, el paso actual y la información de errores. `EstadoCatastro` reúne los formularios de cliente, local y equipo. `EstadoDetalleOrden` conserva el informe, observaciones, respuestas, avisos de borrador y errores de asignación por orden.

Los ViewModels publican estos modelos mediante StateFlow de solo lectura. Las pantallas los observan con `collectAsState()` y comunican las acciones al ViewModel. Las reglas continúan en `domain/validation`; los errores de catálogo se recalculan al modificar campos después del primer intento de guardar.

La preparación del detalle se ejecuta desde un efecto de entrada; consultar el formulario ya no modifica el estado durante la composición. El clima también expone un StateFlow de consultas y libera su indicador de carga al terminar o cancelarse.

El estado del ViewModel conserva el formulario entre recomposiciones y cambios de configuración mientras se mantiene su instancia. No sustituye el almacenamiento de registros ni garantiza recuperar un formulario nuevo después de que Android termine el proceso.

## Consulta del clima

`ServicioClima` declara la ruta y los parámetros de Open-Meteo. `ClienteClima` configura Retrofit, Gson y los tiempos de espera de OkHttp. `RespuestaClima` representa el JSON recibido y lo convierte al modelo `Pronostico`, descartando horas incompletas o valores fuera de rango.

`OpenMeteo` sigue implementando `FuenteClima`, por lo que `ClimaRepository` y las pantallas conservan el mismo contrato. Las llamadas son suspendidas; cancelar una consulta cancela también la petición HTTP. El repositorio propaga la cancelación y utiliza el respaldo local ante otros errores.

La caché conserva su formato JSON y su clave por coordenadas. Se mantienen los 30 minutos de vigencia, las 48 horas máximas de respaldo y los umbrales de alerta de lluvia y viento. `FormatoClima` solo serializa la caché; el JSON de la API lo lee Retrofit.

Las pruebas de red usan MockWebServer en la JVM con respuestas controladas. No requieren credenciales, el servicio real ni el emulador.

## Alcance de la reorganización

En la reorganización inicial se separaron los antiguos archivos de pantallas múltiples, se ajustaron imports y se movieron las pruebas. En ese cambio se conservó el esquema SQLite 8. La etapa 3 posterior lo migra a Room 9, como se describe más abajo. Se conservan el identificador de la aplicación, el archivo de base de datos, las rutas y los archivos privados de evidencia. Los perfiles siguen siendo de demostración y la sincronización sigue siendo simulada.


## Verificación de la reorganización

Se comprobó que las 122 declaraciones originales conservan su contenido lógico después de normalizar paquetes, imports, comentarios y visibilidad de helpers compartidos. La aplicación y la APK de pruebas compilan con el JDK completo instalado. Las 38 pruebas unitarias pasaron con sus paquetes nuevos.

Las reglas y el formato de SQLite permanecen en versión 8; esta reorganización no agrega una migración. El documento Word existente se mantiene fuera del cambio de código.


Las 20 pruebas Android también pasaron en el emulador temporal, ejecutando explícitamente `data.local.HistorialDbTest`, `data.remote.ClimaDatosTest` y `ExampleInstrumentedTest` con AndroidJUnitRunner. La detección automática del runner se interrumpió durante el arranque lento del emulador; la selección explícita ejecutó todos los casos. No se instalaron APKs en el teléfono durante esta revisión.

## Persistencia con Room 9

`AcisRoom` define seis entidades y sus DAO. `OrdenesDb` coordina las transacciones: una orden, su evento y su revision pendiente se guardan juntos. Si falla una parte, Room revierte la operacion completa. El acuerdo SLA original se conserva al editar la orden.

La base sigue usando `acis_ordenes.db`. `MigracionRoom` pasa de la version 8 a la 9 dentro de una transaccion, copia las seis tablas, conserva identificadores y relaciones y verifica las claves foraneas. Las versiones 1 a 7 primero recorren sus migraciones anteriores hasta la 8. Esos helpers estan separados en `data/local/migration` y no se usan para las operaciones habituales.

No hay migracion destructiva ni borrado automatico si un registro antiguo no cumple el nuevo esquema. La apertura falla y la pantalla permite reintentar, conservando la base anterior. Los archivos de fotos no se trasladan ni se eliminan durante la migracion.

Los repositorios ejecutan sus acciones suspendidas en Dispatchers.IO y ordenan las escrituras con un Mutex. La aplicacion carga los registros en segundo plano y muestra carga, error o contenido. `OperacionesPantalla` evita repetir una accion mientras se guarda. El autoguardado al salir utiliza el alcance de la aplicacion para no cancelarse al cerrar la actividad.

El esquema exportado esta en `app/schemas`. Las pruebas de persistencia se comparten en `src/sharedTest`: las ejecuta Robolectric con SQLite nativo en la JVM y tambien pueden ejecutarse como pruebas Android. Incluyen migraciones antiguas, rollback, cascadas, acuerdos, revisiones y creaciones concurrentes.

## Galería y preferencias

Durante una visita, `DetalleOrden` abre el selector de fotos de Android con `PickVisualMedia`. No se pide acceso general a los archivos. El ViewModel entrega el content URI a `OrdenesRepository`, que importa la foto en Dispatchers.IO mediante `AlmacenEvidencias`.

Se comprueba el contenido, se limita la entrada a 20 MB, se reduce la imagen a un máximo de 2048 píxeles por lado y se aplica su orientación EXIF. La copia se guarda como JPEG en `files/evidencias`, con su categoría Antes, Después u Otra. La OT guarda el nombre del archivo y conserva las evidencias anteriores. No depende del permiso temporal del selector ni de que el original permanezca en la galería.

Cancelar el selector no modifica la orden. Un archivo inválido muestra un aviso. La importación limpia temporales y elimina la copia si falla la asociación con Room. Una vez iniciada esa asociación se termina antes de atender una cancelación, para no borrar una foto que ya quedó referenciada.

`data/preferences/PreferenciasRepository` guarda únicamente ajustes con Preferences DataStore. La aplicación mantiene una sola instancia. `EstadoPreferencias` publica carga, datos y avisos al ViewModel. La opción Recordar mi perfil está desactivada inicialmente; al activarla conserva el último rol y el técnico elegido, y al desactivarla elimina ese acceso guardado. Un error de lectura permite entrar por la selección manual.

El acceso recordado se aplica una vez al abrir; no reemplaza la navegación al girar la pantalla o cambiar de perfil. Sigue siendo un acceso de demostración, sin autenticación. Las órdenes permanecen en Room y la caché del clima conserva su almacenamiento anterior.
