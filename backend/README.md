# Backend ACIS

Backend del plan EFT. Spring Boot 3.5.16, Java 17 y Maven Wrapper 3.9.9. Dos aplicaciones independientes comparten contratos, roles y manejo de errores en comun.

- usuarios-service: puerto 8081 y base acis_usuarios.
- operaciones-service: puerto 8082 y base acis_operaciones.
- PostgreSQL 17 local: puerto 55432, accesible solo desde el equipo. Las dos cuentas de base tienen contraseñas independientes.

## Qué funciona ahora

Usuarios permite registro, login, renovación, cierre de sesión, recuperación por correo y edición del nombre propio. El administrador puede listar cuentas y cambiar su rol o estado activo. El registro público crea clientes; los otros roles no se eligen desde el formulario. Ver el [contrato de la API](contrato-api.md).

Operaciones conserva sus rutas de negocio cerradas. Todavía faltan su CRUD remoto y la conexión con Android, que mantiene los tres perfiles de demostración locales.

## Compilar y probar

Desde PowerShell, dentro de backend:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd -B -ntp verify
```

El wrapper descarga su Maven la primera vez. Las pruebas arrancan ambos servicios en puertos aleatorios y usan H2 en memoria con modo PostgreSQL. No necesitan Docker ni contraseñas. Probar con H2 no sustituye la comprobación con PostgreSQL real.

## Preparar PostgreSQL

1. Iniciar Docker Desktop.
2. Copiar .env.example como .env y completar tres contraseñas diferentes solo en el equipo. Si ya existe .env, conservarlo.
3. Usar valores simples sin comillas, comentarios en la misma línea ni interpolación de variables. El script de inicio lee clave=valor sin ejecutar su contenido.
4. Ejecutar desde backend:

```powershell
docker compose up -d postgres correo
docker compose ps
```

La inicialización crea dos bases separadas. Los datos quedan en el volumen local de Compose. El usuario de inicialización se usa solo para preparar el contenedor; las aplicaciones usan su cuenta propia. Cambiar las contraseñas del archivo después de crear el volumen no modifica automáticamente las cuentas existentes.

.env está excluido de Git. No incluirlo en la entrega ni mostrar su contenido al compartir logs. Las credenciales de firma del APK no se usan en el backend.

Para crear el primer administrador, completar ACIS_ADMIN_NOMBRE, ACIS_ADMIN_CORREO y ACIS_ADMIN_CLAVE en .env antes de iniciar usuarios. Sin correo y clave, no se crea ninguna cuenta administrativa. El arranque no modifica un administrador existente ni convierte un cliente registrado en administrador. Una vez creada la cuenta inicial, retirar esas variables de la configuración. El administrador gestiona los roles ADMINISTRADOR, PLANIFICADOR, TECNICO y CLIENTE.

Mailpit recibe el correo de desarrollo por SMTP en localhost:1025. Abrir http://localhost:8025 para leer el código de recuperación y enviarlo a /api/v1/auth/restablecer junto con la nueva clave. No entrega mensajes a buzones externos. El despliegue necesitará un proveedor SMTP y su configuración segura; el correo de recuperación actual no tiene cola ni reintento automático si falla el envío.

## Ejecutar los servicios

Después de compilar y con PostgreSQL listo, abrir dos terminales en backend. Configurar JAVA_HOME en ambas y ejecutar:

```powershell
.\iniciar-servicio.ps1 usuarios
```

```powershell
.\iniciar-servicio.ps1 operaciones
```

El script carga las variables ACIS del archivo .env sin imprimirlas y conserva las que ya estén definidas en la terminal. Cada servicio también acepta ACIS_USUARIOS_DB_URL o ACIS_OPERACIONES_DB_URL, el usuario de base correspondiente y ACIS_USUARIOS_PORT o ACIS_OPERACIONES_PORT. Los valores predeterminados de URL y usuario están en application.yml.

Para comprobar desde otra terminal:

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/estado
Invoke-RestMethod http://localhost:8082/api/v1/estado
Invoke-RestMethod http://localhost:8081/actuator/health/readiness
Invoke-RestMethod http://localhost:8082/actuator/health/readiness
```

Las rutas privadas de usuarios requieren Authorization: Bearer seguido del token de acceso obtenido en login. Las de administración exigen además el rol correspondiente. Las rutas de negocio de operaciones siguen cerradas hasta integrar la consulta de sesión. Para detener un servicio, usar Ctrl+C en su terminal. Para detener los contenedores sin borrar registros: docker compose stop.

El emulador podrá acceder al computador con 10.0.2.2 al integrar Android. Un celular físico usa una dirección accesible del equipo. La conexión HTTP local es para desarrollo; se deberá configurar HTTPS para el despliegue real.

## Organización

Usuarios se organiza por función y luego por responsabilidad:

```text
cl.acis.usuarios
  autenticacion/  controlador, dto, modelo, repositorio, servicio
  usuario/        controlador, dto, modelo, repositorio, servicio, validacion
  seguridad/      configuracion, filtro, modelo, tokens
  notificaciones/ modelo, servicio
  configuracion/  reloj, cifrado y administrador inicial
  web/            estado del servicio
```

Los controladores reciben DTO y delegan en servicios. Los servicios validan reglas y transacciones; los repositorios hacen las consultas JDBC. comun contiene modelo/Rol y web para estados, errores y seguridad base. Las migraciones están en src/main/resources/db/migration y las pruebas en src/test, separadas por función. El perfil test usa H2 y correo simulado; no activarlo para demostrar persistencia o envío SMTP reales.

V2 agrega sesiones y códigos de recuperación. Solo se guardan hashes de esos tokens. Sus claves foráneas eliminan las dependencias en cascada al eliminar una cuenta. Esto conserva la integridad de la base; no añade una acción pública para borrar usuarios.

## Siguiente etapa

Integrar las pantallas de cuentas y los cuatro roles en Android, separar los datos locales por cuenta y después implementar el primer CRUD remoto. La aplicación móvil y la APK local 1.1.0 permanecen sin cambios en este avance.

## Verificación de cuentas

Maven verify completó 50 pruebas sin fallos: seis de comun, 37 de usuarios y siete de operaciones. Incluyen validaciones, permisos, control de versión, expiración, renovación concurrente, revocación de sesiones, recuperación de uso único, protección del último administrador y cascadas. El número de pruebas no representa un porcentaje de cobertura.

Se comprobó además el JAR empaquetado contra PostgreSQL y SMTP local de Mailpit: migración V2, registro, cambio de rol, permisos, recepción del código, cambio de clave, revocación y rechazo de reutilización. Solo se eliminaron las cuentas creadas para esa comprobación y se verificaron sus cascadas. El JAR se detuvo al finalizar; los contenedores locales permanecen disponibles. La comprobación no demuestra entrega a un buzón externo ni integración de Android.

## Verificación de la etapa 1 del 18 de noviembre de 2026

- Maven Wrapper compiló y empaquetó los dos JAR ejecutables. Pasaron 20 pruebas: seis de errores comunes y siete de cada servicio, sin fallos ni omisiones.
- Las pruebas usan HTTP real en puertos aleatorios y H2 en memoria. Cubren respuesta pública, readiness, rutas cerradas sin sesión, método de escritura no permitido, migraciones separadas, rol inválido, acuerdo SLA incompleto y conservación de filas al volver a aplicar Flyway.
- Se inició Docker Desktop y PostgreSQL 17 con credenciales locales aleatorias en .env, excluido de Git. Se verificaron ambas bases, migración v1, readiness UP y respuesta 401 de las rutas de negocio en los JAR empaquetados.
- Las dos cuentas de base no tienen permiso CONNECT sobre la base del otro servicio. Se guardó un cliente de comprobación, se reiniciaron los dos servicios, se comprobó que seguía presente y se eliminó únicamente ese registro. Las bases de trabajo quedaron sin usuarios ni clientes demo y con su migración aplicada.
- La base PostgreSQL queda disponible en Docker. Los ejecutables usados para la comprobación se detienen al terminar; las instrucciones anteriores permiten iniciarlos de nuevo. No se modificó ni reinstaló Android, ni se alteró la APK firmada 1.1.0.
- Todavía faltan sesiones, endpoints de negocio y su conexión desde Android. No se afirma cobertura del 80 % ni funcionamiento de los cuatro roles reales en esta etapa.

Referencias de la base: [requisitos Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [migraciones Flyway](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html) y [salud del servicio](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html).
