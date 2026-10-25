# Plan de trabajo: Evaluación Parcial 2 (DSY1105)

Fuentes revisadas: `Ev. Parcial 2_Estudiante_Encargo_DSY1105.pdf` (6 páginas) y `Temáticas sugeridas o ejemplo Ev. Parcial 2 y 3 _DSY1105_ Anexo Docente.pdf` (2 páginas), entregados por el curso. El primero contiene las exigencias y la rúbrica; el segundo ofrece ejemplos adaptables. ACIS corresponde a la temática de servicios técnicos, sin obligación de copiar todas sus funciones de ejemplo.

## Alcance evaluado

El parcial 2 solicita una **interfaz funcional** en Android Studio para el caso elegido. Es un avance de la EFT. Exige navegación, formularios con validaciones visuales por campo e íconos, lógica de validación desacoplada, estado coherente, animaciones, estructura modular/MVVM, persistencia local y acceso seguro a **dos recursos nativos del dispositivo**. También exige GitHub, planificación colaborativa en Trello o similar y un README con integrantes, funciones y pasos de ejecución.

| Indicador | Peso | Estado observado en ACIS | Trabajo prioritario |
| --- | ---: | --- | --- |
| IE 2.1.1 Interfaz y navegación | 15 % | Lista, creación y detalle navegables; tema Material 3 básico. | Mantener jerarquía y navegación claras; pulido visual al cierre. |
| IE 2.1.2 Formularios | 15 % | Creación, informe, observación y respuesta muestran errores junto a cada campo. | Revisar el resultado en un celular antes de la entrega. |
| IE 2.2.1 Validación y estado | 10 % | Las reglas están en `ValidadorOt` y el estado de formularios en `OrdenesViewModel`. | Mantener esta separación al agregar funciones. |
| IE 2.2.2 Animaciones | 10 % | Hay transiciones de navegación, perfil y estado. | Conservarlas y revisar su coherencia al reorganizar pantallas. |
| IE 2.3.1 Modularidad y persistencia | 15 % | Órdenes guardadas en SQLite local; los datos anteriores se copian desde SharedPreferences. | Mantener esta estructura al agregar nuevos datos a la OT. |
| IE 2.3.2 Colaboración | 20 % | Existe GitHub con commits; no se ha verificado tablero compartido ni README. | Crear README, tablero Trello/similar, tareas e historial de contribuciones reales de la pareja. |
| IE 2.4.1 Recursos nativos | 15 % | La cámara guarda una foto en la OT y el check-in solicita ubicación aproximada. | Revisar la ubicación en un celular, con permiso aceptado y rechazado. |

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
