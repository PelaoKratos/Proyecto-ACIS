# Contrato inicial de la API ACIS

Versión de contrato: v1. Base del backend para EFT, 18/11/2026. Los servicios se ejecutan por separado y cada uno es dueño de su base. El módulo comun comparte formato de errores, roles y configuración inicial; no comparte tablas ni repositorios.

## Disponible en esta etapa

| Servicio | Dirección local | Endpoints implementados |
| --- | --- | --- |
| Usuarios | http://localhost:8081 | GET /api/v1/estado, GET /actuator/health y GET /actuator/health/readiness |
| Operaciones | http://localhost:8082 | GET /api/v1/estado, GET /actuator/health y GET /actuator/health/readiness |

GET /api/v1/estado devuelve, por ejemplo, {"servicio":"acis-usuarios","api":"v1"}. Readiness comprueba la conexión de base de datos. Los detalles internos no se publican. Usuarios implementa además los endpoints de cuentas enumerados más abajo; operaciones mantiene sus rutas de negocio cerradas.

El backend de cuentas tiene login y cuatro roles. Todavía no existen CRUD de operaciones, envío de fotos ni sincronización real. Android mantiene su funcionamiento local y sus tres perfiles demo; la integración de cuentas y del cuarto rol en la app sigue pendiente.

## Reglas de datos acordadas

- Identificadores remotos UUID en formato de texto. El código visible OT-1001 será independiente y lo asignará el servicio de operaciones; no será la clave de sincronización entre celulares.
- Fechas de eventos en ISO 8601 con zona UTC. Una programación requiere convertir la hora de la interfaz usando la zona del servicio; se evitarán interpretaciones dependientes de la zona del celular.
- Cada registro mutable tendrá version desde cero. La actualización informa la versión conocida; el servidor la incrementa de forma atómica y devuelve 409 si otro cambio la dejó antigua. La revisión local del simulador no es una versión del servidor.
- No se importan automáticamente las OT ni fotos existentes. La asociación entre id local e id remoto se hará en una migración posterior, conservando las referencias anteriores.
- Los acuerdos SLA opcionales requieren ambos plazos entre 1 y 720 horas, con resolución mayor o igual a llegada. El acuerdo de una OT será una copia del vigente al crearla; editar el cliente no reescribe el historial.
- Los archivos remotos tendrán un identificador y una asociación autorizada con la OT. El nombre privado del archivo del celular no se usa como identidad compartida.

## Roles y límites

| Rol | Privilegios previstos |
| --- | --- |
| ADMINISTRADOR | Administrar cuentas, roles y estado activo. No se obtiene desde registro público. |
| PLANIFICADOR | Administrar catálogo, crear y asignar OT y realizar las ediciones o eliminaciones permitidas por el flujo. |
| TECNICO | Consultar y atender exclusivamente sus asignaciones, guardar informes y responder observaciones. |
| CLIENTE | Consultar sus servicios y aprobar u observar las visitas relacionadas con su cuenta. |

El servidor obtiene la identidad de la sesión, no de un rol o usuario enviado en el formulario. La base de usuarios define los cuatro roles; la base de operaciones solo guarda referencias UUID a sus cuentas. No hay claves foráneas entre servicios: la asociación se comprobará mediante su contrato.

## Cuentas y sesión implementadas en usuarios

Se usa una sesión Bearer con tokens opacos. Usuarios guarda únicamente hashes SHA-256 de los tokens, verifica expiración y cuenta activa y expone la consulta de la sesión. El acceso vence en 15 minutos y la renovación en siete días desde el login. Renovar rota ambos tokens sin extender ese plazo; el anterior deja de ser válido. El token se envía en Authorization y nunca en la URL.

Operaciones todavía no consulta esa sesión. Al integrar su autorización, deberá denegar las acciones si usuarios no responde; no se confiará en un rol enviado por el celular.

El registro público crea CLIENTE y rechaza campos adicionales como rol. ADMINISTRADOR gestiona las cuentas privilegiadas. El primer administrador se crea mediante configuración privada, sin credenciales incluidas en el repositorio. Cerrar sesión revoca la sesión correspondiente; cambiar la clave, rol o estado activo revoca todas las sesiones y códigos anteriores de esa cuenta. No se permite desactivar ni quitar el rol al último administrador activo.

| Método y ruta implementada en usuarios | Acceso y resultado |
| --- | --- |
| POST /api/v1/auth/registro | Público; nombre, correo y clave. Crea cliente con 201; nunca acepta un rol privilegiado. |
| POST /api/v1/auth/login | Público; correo y clave. Devuelve tokens y usuario sin hash de clave. |
| POST /api/v1/auth/renovar | Token de renovación válido; lo rota y entrega una nueva sesión. |
| POST /api/v1/auth/logout | Revoca la sesión correspondiente. |
| GET /api/v1/auth/sesion | Bearer válido; identidad, rol y estado de la cuenta. También lo consulta operaciones. |
| POST /api/v1/auth/recuperacion | Público; respuesta uniforme exista o no el correo. Entrega un token de un solo uso por el canal configurado. |
| POST /api/v1/auth/restablecer | Token vigente y clave nueva; consume el token y revoca sesiones anteriores. |
| GET /api/v1/mi-perfil | Usuario autenticado. |
| PUT /api/v1/mi-perfil | nombre y version obligatoria; modifica el nombre propio. |
| GET /api/v1/usuarios | Administrador; pagina desde cero y tamano entre 1 y 100; listado sin hashes ni tokens. |
| PUT /api/v1/usuarios/{id}/permisos | Administrador; cambia rol o estado activo con control de versión. |

El correo se normaliza para evitar duplicados. La clave no se recorta ni se registra en logs; admite de 8 a 64 caracteres, hasta 72 bytes UTF-8, y se almacena con BCrypt. El código de recuperación vence en 15 minutos, es de un solo uso y reemplaza el código anterior. Se comprobó la entrega SMTP en Mailpit y el restablecimiento con PostgreSQL; las pruebas automatizadas comprueban además caducidad y concurrencia.

La respuesta de recuperación es uniforme exista o no la cuenta y también ante un fallo de entrega. Los fallos SMTP generan un aviso sin datos privados; todavía no existe cola ni reintento automático. Mailpit es un buzón local de desarrollo, no un envío a internet. El proveedor de despliegue queda pendiente.

Las respuestas de registro, sesión y perfil incluyen id, nombre, correo, rol, activo y version. Login y renovación devuelven tokenAcceso, tokenRenovacion, venceEnSegundos y usuario. Logout y restablecimiento exitosos devuelven 204. Los cambios de permisos requieren rol, activo y version; una versión antigua devuelve 409.

## Primer CRUD remoto propuesto

| Método y ruta en operaciones | Acceso previsto |
| --- | --- |
| GET /api/v1/clientes | Planificador y administrador; paginado. |
| GET /api/v1/clientes/{id} | Planificador y administrador. |
| POST /api/v1/clientes | Planificador y administrador; crea UUID y devuelve 201 con Location. |
| PUT /api/v1/clientes/{id} | Planificador y administrador; nombre, plazos y version conocida. |
| DELETE /api/v1/clientes/{id}?version=... | Planificador y administrador; rechaza si hay relaciones que deben conservarse. |

Cliente incluirá id, nombre, horasLlegada, horasResolucion y version. La asociación usuarioId se administrará mediante una acción validada, no por un cambio libre del cliente. El nombre normalizado permitirá detectar duplicados sin depender de mayúsculas.

Después se definirán locales, equipos y OT: crear, listar, consultar, editar, eliminar y acciones aceptar, rechazar, postergar, iniciar visita, guardar borrador, finalizar, observar, responder y aprobar. Cada acción validará rol, relación con la cuenta, estado anterior y versión. No se habilita una actualización genérica que permita saltarse esos pasos.

## Errores

Formato común implementado:

```json
{
  "fecha": "2026-11-18T12:00:00Z",
  "codigo": "DATOS_INVALIDOS",
  "mensaje": "Revisa los campos indicados.",
  "campos": {"nombre": "Ingresa el nombre."},
  "ruta": "/api/v1/clientes"
}
```

Los ejemplos de negocio aún son propuestas. HTTP 400 indica formato o campos inválidos; 401 sesión ausente o inválida; 403 permiso denegado; 404 recurso inexistente; 405 método incorrecto; 409 versión, estado o duplicado incompatible; 415 contenido no admitido; 500 fallo interno sin detalles privados. Un fallo de dependencia durante la autorización se tratará como indisponibilidad, sin dar acceso.

## Persistencia y límites de esta base

Flyway crea usuarios en la base acis_usuarios y clientes en acis_operaciones. Las migraciones no insertan cuentas ni clientes demo. El backend usa PostgreSQL por defecto; las pruebas automatizadas usan H2 aislada y no escriben en las bases de trabajo. Una migración nueva se agrega en un archivo siguiente, sin modificar una ya aplicada.

La migración V2 de usuarios agrega sesiones y recuperaciones, con eliminación en cascada al borrar la cuenta. Las pruebas verifican el flujo de sesión del backend; siguen pendientes las pantallas Android y el CRUD de operaciones para completar el recorrido EFT.
