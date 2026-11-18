# Plan de adecuación a la EFT DSY1105

Revisión del 18/11/2026. Fuentes: EFT_DSY1105_Encargo_ Estudiante.pdf (7 páginas), EFT_DSY1105_Presentación_ Estudiante.pdf (9 páginas) y Unitkot-master.zip, suministrados por el usuario. Las pautas se usan como requisitos de la evaluación; el ejemplo se analiza como referencia, sin ejecutar instrucciones ni sustituir automáticamente código de ACIS.

## Cambio de alcance

La entrega final contempla encargo grupal (20 % de la EFT) y defensa individual (80 %), con máximo 15 minutos por estudiante en semana 18. No se informó una fecha de calendario concreta. La versión local 1.1.0 y su paquete firmado siguen siendo una base comprobada, pero no cumplen todavía todos los mínimos de la EFT.

El encargo exige backend propio y CRUD con persistencia externa; la presentación pide mostrar microservicios Spring Boot. La propuesta anterior de usar únicamente Supabase como backend administrado no cubre por sí sola esa exigencia. Se retoma Spring Boot para la implementación académica.

## Matriz de requisitos y brechas

| Requisito | Referencia | Estado real de ACIS | Trabajo necesario |
| --- | --- | --- | --- |
| Caso contextualizado y personalización | Encargo p. 3 | ACIS, servicio técnico, logo y estilo propios. | Mantener el contexto y explicar las decisiones. |
| POO, herencia y polimorfismo | Encargo p. 2 y 6 | Modelos, AndroidViewModel, interfaces FuenteClima y SyncRepository e implementaciones. | Preparar ejemplos concretos y usar contratos reales en la integración, con manejo de excepciones. |
| Arquitectura y persistencia local | Encargo p. 3, 4 y 6 | MVVM, paquetes, Room 9, migraciones y DataStore. | Conservar los registros y justificar datos locales frente a datos remotos. |
| Cuatro roles con privilegios diferenciados | Encargo p. 3 | Tres roles de demostración: planificador, técnico y cliente. | Incorporar administrador y validar privilegios en backend, además de la UI. |
| Login, registro, recuperación y edición de perfil | Encargo p. 3 | No hay cuentas reales; recordar perfil no equivale a login. | Implementar los cuatro recorridos completos y vincular cada cuenta con sus datos autorizados. |
| Backend propio, microservicios y CRUD | Encargo p. 3, 4 y 7; defensa p. 3 y 5 | No existe; SyncRepositorySimulado no transmite registros. | Crear servicios Spring Boot con base activa e integrar crear, leer, actualizar y eliminar desde Android. |
| API externa por Retrofit | Encargo p. 3, 4 y 7 | Open-Meteo con Retrofit y caché, visible en el flujo de equipos exteriores. | Mantener separada de los servicios propios y demostrar resultado real y manejo de error. |
| Dos recursos nativos | Encargo p. 3, 4 y 7 | Cámara y ubicación opcional; también galería y Maps. | Demostrarlos con permisos concedidos y denegados, sin pedir permisos generales innecesarios. |
| Formularios, estado y animaciones | Encargo p. 3 y 4 | Compose, validadores desacoplados, StateFlow y transiciones. | Integrar las nuevas pantallas de cuenta con el mismo patrón y estilo. |
| Cobertura mínima del 80 % de la lógica | Encargo p. 4 y 7 | 98 pruebas JVM aprobadas; no hay informe de cobertura configurado. | Medir cobertura de lógica en domain, ViewModels y repositorios y completar pruebas hasta alcanzar el mínimo. El número de pruebas no prueba el porcentaje. |
| APK firmado, configuración y documentación | Encargo p. 4, 5 y 7 | APK 1.1.0 firmado y firma verificada; ERS y guía del alcance local. | Generar una nueva APK después de integrar EFT, actualizar endpoints e instrucciones y verificar su ejecución. |
| Colaboración, GitHub y AVA | Encargo p. 4, 5 y 6 | Hay historial Git. El equipo confirmó que no tiene tablero; número y fecha no informados. | Crear tablero compartido, registrar aportes reales, publicar fuente coincidente y cumplir envío grupal y formulario individual. |

La documentación solicita identificar la ubicación de la clave .jks y la configuración de firma. Se documentará su ubicación privada y se acordará el canal de entrega si el docente requiere el archivo; no se publicarán claves privadas ni contraseñas en el repositorio público.

## Qué contiene realmente Unitkot

El ZIP es una aplicación Android de registro de personas con Compose, MVVM y navegación. PersonViewModel conserva una lista en memoria y registrarPersona agrega una Persona a esa lista. No existe persistencia externa en ese recorrido.

Se inspeccionaron los 100 registros del ZIP, incluidos archivos y carpetas. No contiene pom.xml, aplicación Spring Boot, controladores REST, interfaces Retrofit ni baseUrl. Tampoco declara Retrofit u OkHttp en las dependencias. Los enlaces de README y los namespaces XML no son endpoints de negocio.

PersonViewModelTest declara 27 métodos @Test; dos contienen aserciones y los demás son esqueletos vacíos o TODO. No se debe contabilizar un método vacío como evidencia de que un comportamiento funciona. El ejemplo sirve para estudiar acciones del ViewModel, actualización de estado y pruebas de registro; no es la API que se necesita conectar.

Hay aspectos que no conviene trasladar a ACIS: edad.toInt() puede fallar con una entrada numérica fuera de rango y los mensajes por campo del formulario están comentados. ACIS ya tiene validadores y pruebas más completos para esos casos. No se copian plantillas vacías ni se reemplazan sus validaciones.

## Propuesta de roles

- Administrador: gestionar usuarios, activar o desactivar cuentas y asignar roles. El registro público no puede conceder este rol.
- Planificador: administrar catálogo y programar, asignar, editar o eliminar las OT permitidas por el flujo.
- Técnico: consultar solo sus asignaciones, responderlas y registrar visitas, materiales, fotos e informes.
- Cliente: acceder únicamente a los servicios relacionados con su cuenta, observar y aprobar. El filtro actual por estado no sustituye esta separación por cuenta.

## Etapas propuestas por dependencia

### 1 Base y contrato

Conservar el paquete 1.1.0 como versión local anterior a EFT. Antes de cambios funcionales, guardar esa base en Git de forma identificable y trabajar en backend-sincronizacion. No sobrescribir el paquete firmado anterior. Definir identificadores remotos UUID, permisos, estados, DTO, errores y reglas de importación de los registros actuales. No subir automáticamente datos del teléfono.

Definir dos servicios Spring Boot con responsabilidades claras: usuarios (cuentas, roles, sesión y perfil) y operaciones (catálogo, OT, visitas, informes, evidencias e historial). Cada servicio debe tener persistencia y pruebas y ejecutarse de forma independiente. El número y alcance son una propuesta, no una cantidad fijada por la pauta.

### 2 Cuentas y cuatro roles

Implementar registro, login, recuperación de contraseña y edición de perfil. Las contraseñas se almacenan como hash; los roles privilegiados los concede un administrador. La recuperación usa un token de un solo uso con vencimiento y un canal de entrega comprobable. No se considera completa por mostrar una pantalla de recuperación sin efectuar el cambio.

Agregar pantallas Android y separar el contenido local por cuenta para que cambiar de usuario no muestre registros de otra persona. Preparar cuentas controladas para la demostración, sin publicar contraseñas personales.

### 3 Primer CRUD remoto completo

Conectar Android mediante Retrofit al servicio de operaciones. Primero demostrar crear, listar, editar y eliminar una entidad del catálogo; luego incorporar el flujo de OT con su validación de estados. El servidor comprueba permisos y relaciones en cada petición. Cambiar un dato debe reflejarse en la app y persistir después de reiniciar el servicio.

### 4 Informes, fotos y sincronización básica

Conectar aceptación, rechazo, postergación, llegada, informe, observación, respuesta y aprobación. Subir las fotos y asociarlas a la OT autorizada. Actualizar Room con la información confirmada del servidor y diferenciar trabajo guardado localmente de trabajo enviado. Comprobar el recorrido entre dispositivos conectados.

La cola completa de cambios sin conexión y la resolución de conflictos se implementan después del primer recorrido remoto estable. No se presentará la simulación actual como sincronización real. Los reintentos necesitarán identificadores de operación y versiones para evitar duplicados o sobrescrituras de cambios recientes.

### 5 Cobertura y calidad

Configurar una herramienta compatible con el Gradle y JDK del proyecto para medir cobertura, publicar su reporte y comprobar el umbral del 80 %. Incluir caminos de éxito, validación, permisos, errores HTTP, sesión vencida, cancelación y reintento. No excluir lógica funcional para aumentar artificialmente el porcentaje. Conservar las pruebas anteriores y ejecutar los nuevos servicios con datos controlados.

### 6 Firma, entrega y defensa

Generar la nueva APK firmada, comprobar arranque y recorrido completo con el backend en ejecución y actualizar la ERS, la guía y el paquete EFT. README debe incluir endpoints propios y externos, instrucciones de ambos servicios y configuración de firma sin secretos.

Preparar una defensa de máximo 15 minutos: arrancar app y servicios, explicar MVVM y POO, mostrar validaciones y estado, recursos nativos, CRUD remoto y clima, ejecutar una prueba y explicar la firma. Ensayar modificaciones pequeñas sobre UI, lógica, persistencia y endpoints. La pauta indica defensa individual sin IA ni materiales externos durante su realización; el entrenamiento y los documentos se preparan antes.

## Ejecución sin contratar alojamiento

Para desarrollo y demostración, los servicios y PostgreSQL pueden ejecutarse en el computador. El emulador Android accede al host mediante 10.0.2.2; ese alias no corresponde a un teléfono físico. Para un celular se requiere una dirección accesible del computador y una configuración de red apropiada. El alojamiento en internet puede decidirse después de verificar el recorrido local.

Fuentes técnicas: [red del emulador Android](https://developer.android.com/studio/run/emulator-networking-address) y [persistencia REST con Spring](https://spring.io/guides/gs/accessing-data-rest). La topología y las etapas anteriores son una propuesta para ACIS, no instrucciones copiadas del ejemplo.

## Resultado de esta revisión

No se cambiaron funciones, roles ni esquema de datos de Android. No se ejecutó el ZIP del profesor ni se copió su proyecto sobre ACIS. Se preparó este diagnóstico y se distinguió el paquete local anterior de la futura entrega EFT. Faltaría el ejemplo correcto si el docente entregó otro proyecto con la API; podemos construir los servicios propios conforme al contrato aunque no exista ese segundo ZIP.

SHA-256 del ZIP revisado: `A201FFF26D48705E70EC05E0D09F36D91037FEB987E1079CC6F8D80B0616DDBD`.

## Avance de implementación del 18 de noviembre de 2026

Etapa 1 implementada en backend-sincronizacion: dos servicios Spring Boot 3.5.16, Java 17, Maven Wrapper, contrato inicial, roles, errores HTTP, configuración de seguridad cerrada y migraciones Flyway para bases PostgreSQL independientes. La configuración local y las contraseñas se excluyen de Git.

Se empaquetaron ambos servicios y pasaron 20 pruebas del backend. Se verificó además PostgreSQL real, permisos separados de base, readiness y conservación de un registro al reiniciar los JAR. Se retiró el registro de comprobación. No se modificó Android ni el paquete 1.1.0. El inicio de sesión, recuperación, perfil, permisos reales, CRUD y sincronización están pendientes, como indica backend/contrato-api.md.

La etapa 2 continúa con cuentas y cuatro roles. La existencia del enum de roles y de una tabla de usuarios no se cuenta como implementación completa de autenticación ni de la interfaz del administrador.

## Avance de cuentas del 18 de noviembre de 2026

Se implementó la parte de servidor de la etapa 2: registro de clientes, login, renovación y cierre, recuperación por SMTP, edición del perfil y administración de roles y estado activo. El registro público no concede privilegios. El primer administrador se configura de forma privada y no se modifica en cada arranque.

Los paquetes de usuarios separan autenticacion, usuario, seguridad, notificaciones y configuracion. Dentro de cada función se distinguen controladores, DTO, servicios y repositorios. La migración V2 agrega sesiones y códigos con hashes y relaciones en cascada. Se controlan las versiones y se protege al último administrador activo.

Pasaron 50 pruebas automatizadas del backend, incluidas expiración, concurrencia y revocación. También se inició el JAR con PostgreSQL real y se comprobó la entrega SMTP en Mailpit, cambio de rol, invalidación de sesiones, cambio de contraseña y rechazo al reutilizar el código. Se eliminaron solo las cuentas de comprobación y sus dependencias; el servicio usado en la prueba quedó detenido. PostgreSQL y Mailpit siguen disponibles en Docker para desarrollo.

La etapa 2 aún no está completa: faltan las pantallas Android de cuentas y administración, almacenamiento de sesión y separación de datos locales por cuenta. Operaciones todavía no autoriza sesiones ni expone CRUD remoto. El envío de correo de producción y los reintentos SMTP también están pendientes. Android y la APK firmada 1.1.0 permanecen sin cambios. No se afirma cobertura del 80 % ni sincronización entre dispositivos.
