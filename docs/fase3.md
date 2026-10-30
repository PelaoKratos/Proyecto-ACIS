# Fase 3

## 3.1 Clima — feature/clima

Implementado: pronóstico horario para equipos exteriores en inicio del planificador (hasta tres próximas OT), paso 3 de la OT y jornada del técnico. Regla de alerta: lluvia desde 60% o viento desde 40 km/h. Consulta HTTPS a Open-Meteo con coordenadas del local; no utiliza ubicación de llegada del técnico.

La lógica se separa entre ReglasClima, ClimaRepository, OpenMeteo y ClimaViewModel. WeatherBanner muestra carga, datos, falta de ubicación/fecha, error y respaldo. No se agregan dependencias ni se cambia SQLite.

Caché persistente por coordenadas: 30 minutos de vigencia y respaldo de hasta 48 horas ante fallo. Actualizar clima permite reintentar. La orden siempre se puede guardar aunque el clima no esté disponible. Las fechas fuera del pronóstico no se reemplazan por el clima actual.

Comprobación de la API con coordenadas públicas de ejemplo: respuesta de 384 horas, lluvia en porcentaje, viento en km/h y tiempo Unix. Se consulta la documentación oficial: https://open-meteo.com/en/docs

### Verificaciones

- Compilación correcta y 29 pruebas unitarias sin fallos.
- 13 pruebas Android sin fallos en emulador Android 17: lectura de datos incompletos, persistencia/corrupción de caché y regresiones de órdenes y migraciones.
- Contrato de la API comprobado con coordenadas públicas de ejemplo.
- Arranque correcto de la APK final en el emulador con el nuevo ViewModel.
- Pendiente revisión de los nuevos avisos y conexión real desde el celular.

### Revisión manual

1. En Equipos, editar un equipo y marcar Instalado en exterior.
2. Registrar la ubicación de su local, estando en ese lugar. La dirección escrita por sí sola no permite consultar clima.
3. Crear una OT para mañana, revisar el paso 3 y guardarla. Confirmar que guardar sigue disponible durante la consulta.
4. Revisar la orden en inicio del planificador y jornada del técnico al que se asignó.
5. Tras obtener datos, desactivar internet y pulsar Actualizar clima: debe mostrar el respaldo y su hora.
6. Probar local sin ubicación y fecha fuera del pronóstico: debe informar la falta de datos, sin impedir guardar.

## Pendiente

- 3.2 implementado: pendiente revisión del equipo en el celular.
- 3.3: estado de sincronización mediante un repositorio simulado, identificado expresamente como simulación.

No hay backend ni sincronización real.


### Correcciones de uso

La sección del planificador se llama Catálogo porque reúne clientes, locales y equipos. Las OT del técnico se presentan por número de creación descendente; una actualización de una orden antigua no la convierte en nueva. La visita destacada conserva su selección por horario y da prioridad a una visita en curso.

Inicio muestra siempre una explicación de Clima de las visitas. Si no hay órdenes exteriores pendientes, indica cómo configurar equipo y local. Se incluyen hasta tres pendientes, priorizando las futuras y luego las vencidas más recientes. El detalle de una OT exterior también muestra el pronóstico o el motivo por el que no está disponible.


## 3.2 SLA — feature/sla

- SQLite 7 conserva el catastro anterior y agrega plazos por cliente. Las OT nuevas guardan una copia del acuerdo y programación original; las anteriores no reciben contratos retroactivos.
- Plazos opcionales y validados en Catálogo > Clientes. Valores demo: 4/24 h y 8/48 h.
- Reporte por mes de programación original: cumplimiento, cobertura, tiempo medio desde eventos, detalle por cliente y órdenes en riesgo o vencidas por atender.
- La resolución usa el informe del técnico; la aprobación del cliente no altera el plazo. Modificar o postergar no reinicia el acuerdo original.
- Compilación correcta, 38 pruebas unitarias y 15 pruebas Android sin fallos. Incluyen migraciones 4→7, 5→7 y 6→7, así como conservación del acuerdo al editar cliente y orden.
- Las pruebas Android se ejecutaron directamente con adb en emulator-5580. El teléfono Xiaomi rechazó instalar la APK de pruebas y no se volvió a intentar allí.
- Revisados el arranque, el reporte sin datos y el cambio de mes en el emulador. Captura local: app/build/acis-sla.png.
- Pendiente revisión del equipo en celular: formulario de cliente, acuerdo de OT nueva y resultados al finalizar una visita.
