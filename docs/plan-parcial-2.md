# Plan de trabajo: Evaluación Parcial 2 (DSY1105)

Fuentes revisadas: `Ev. Parcial 2_Estudiante_Encargo_DSY1105.pdf` (6 páginas) y `Temáticas sugeridas o ejemplo Ev. Parcial 2 y 3 _DSY1105_ Anexo Docente.pdf` (2 páginas), entregados por el curso. El primero contiene las exigencias y la rúbrica; el segundo ofrece ejemplos adaptables. ACIS corresponde a la temática de servicios técnicos, sin obligación de copiar todas sus funciones de ejemplo.

## Alcance evaluado

El parcial 2 solicita una **interfaz funcional** en Android Studio para el caso elegido. Es un avance de la EFT. Exige navegación, formularios con validaciones visuales por campo e íconos, lógica de validación desacoplada, estado coherente, animaciones, estructura modular/MVVM, persistencia local y acceso seguro a **dos recursos nativos del dispositivo**. También exige GitHub, planificación colaborativa en Trello o similar y un README con integrantes, funciones y pasos de ejecución.

Estado actualizado al 18/11/2026. La tabla identifica evidencia y pendientes; no asigna una calificación.

| Indicador | Peso | Evidencia actual | Pendiente |
| --- | ---: | --- | --- |
| IE 2.1.1 Interfaz y navegación | 15 % | Compose y Material 3, navegación por perfil, detalle y formularios; acciones del informe desplazables. | Revisión visual específica del último ajuste. |
| IE 2.1.2 Formularios | 15 % | Errores junto a campos, íconos y validación de OT, informe, observación y catálogo. | Completar pruebas de pantalla pendientes. |
| IE 2.2.1 Validación y estado | 10 % | domain/validation, ViewModels y estados StateFlow; pruebas de corrección y recorridos. | Conservar esta separación. |
| IE 2.2.2 Animaciones | 10 % | Transiciones de navegación, estado y expansión de componentes. | Mostrar los ejemplos durante la defensa. |
| IE 2.3.1 Modularidad y persistencia | 15 % | Paquetes por responsabilidad, Room 9, migraciones y DataStore. | Mostrar conservación al reiniciar la APK firmada. |
| IE 2.3.2 Colaboración | 20 % | GitHub con historial y ramas. El equipo confirma que no ha creado un tablero compartido. | Crear tablero, distribuir tareas reales y revisar acceso del docente. No se declara cumplido este punto. |
| IE 2.4.1 Recursos nativos | 15 % | Cámara y ubicación opcional, más galería y apertura de Maps. Manejo de permiso denegado en ubicación. | Registrar revisión específica en la APK de entrega. |

Las secciones siguientes conservan el plan inicial y el avance histórico; para el estado técnico vigente consultar README y docs/entrega.md.

## Orden de implementación

1. **Base de arquitectura:** separar pantallas/componentes de `AcisApp.kt`, introducir ViewModel para estado y eventos, definir interfaz de repositorio y reglas de dominio. Conservar el recorrido existente.
2. **Formularios:** centralizar reglas, errores específicos e íconos para creación de OT, informe técnico y conformidad. Validar antes de persistir o cambiar estado.
3. **Persistencia:** pasar a almacenamiento estructurado local, incluyendo una migración de las OT actuales. Mantener el trabajo sin conexión.
4. **Recursos nativos:** cámara del sistema para adjuntar foto al informe y ubicación puntual al registrar check-in. Pedir el permiso de ubicación en contexto y mantener un camino funcional si se rechaza.
5. **Entrega y colaboración:** README completo, tablero compartido, tareas/commits repartidos y revisión del proyecto en dispositivo físico. Preparar el código fuente para GitHub y AVA.
6. **Pulido final:** consistencia de Material 3, adaptabilidad, accesibilidad y animaciones. La referencia está en `docs/material3-referencia.md`.

## Avance en feature/mvvm-validaciones

Las reglas de validación están en `domain/ValidadorOt.kt` y el estado de los formularios y las acciones en `ui/OrdenesViewModel.kt`. El repositorio entrega las OT mediante `StateFlow`. La pantalla muestra errores por campo e íconos al crear una OT y al completar informe, observación y respuesta. La persistencia usa SQLite y conserva los datos anteriores en SharedPreferences como respaldo.

## Criterios de cierre antes de entregar

- Cada formulario requerido muestra el error junto al campo correspondiente y no guarda entradas inválidas.
- La UI observa estado del ViewModel y las reglas no dependen de componentes Compose.
- Las órdenes permanecen al reiniciar la app y se conserva la información ya registrada.
- Cámara y ubicación funcionan desde el flujo de OT; los permisos denegados se manejan sin bloquear la app.
- Hay evidencia de trabajo de ambos integrantes en GitHub y planificación visible en Trello o similar.
- README incluye descripción, integrantes, funciones y pasos de ejecución.
- La versión de GitHub y el código subido a AVA coinciden. Tras la fecha de entrega no se debe modificar el repositorio antes de la evaluación, según el encargo.

La pauta indica entrega en semana 11 y defensa posterior, pero no da una fecha de calendario concreta. Confirmarla en AVA antes de congelar la versión de entrega.

## Estado administrativo al 18 de noviembre de 2026

El equipo confirmó que no ha creado tablero Trello o equivalente. No se ha informado número de equipo ni fecha límite. La activación individual y el envío grupal de AVA no están confirmados. La lista docs/planificacion-cierre.md permite repartir las tareas pendientes sin inventar evidencias pasadas. La preparación local de un ZIP no equivale a su entrega en AVA ni a la publicación de la fuente en GitHub.
