# Bitácora de uso de IA — Gota de Vida

Registro honesto de cómo usamos IA en el proyecto (criterio extra "Vibecoding" de la rúbrica). Formato en `docs/ai/PROMPT_MAESTRO.md`, sección 15.

### [2026-11-11] Fase A: sistema de diseño y pantallas en Claude Design
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude y Claude Design
- Rama / PR: (todavía no hay repositorio)
- Prompt: "Corre el prompt maestro", que ejecuta la sección 5 del prompt maestro (prompts 5.2 y 5.3).
- Resultado: sistema de diseño (17 colores en claro y oscuro, 8 estilos de texto, 6 espaciados, 4 radios) y 14 pantallas: 11 en claro, 2 en oscuro y 1 en oscuro e inglés. Valores en `docs/design/TOKENS.md`.
- Decisiones y ajustes humanos: el equipo revisó las pantallas y las aprobó sin cambios.
- Lo que aprendimos: <pendiente>

### [2026-11-11] Fase B: estructura del repositorio
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude
- Rama / PR: chore/estructura-base (PR #1)
- Prompt: "Aprobado", que continúa con la Fase B del prompt maestro (sección 6).
- Resultado: monorepo con `app/` (Android, Compose, pantalla de arranque en español e inglés), `backend/` (Ktor con `/health`, Swagger y pruebas), carpetas por funcionalidad con sus cinco capas, tres workflows de GitHub Actions, plantillas de PR e issues, `CLAUDE.md` e instrucciones para Copilot y Cursor, y prompts reutilizables en `docs/ai/prompts/`.
- Decisiones y ajustes humanos: <pendiente: resultado de la primera compilación en Android Studio y en el CI, versiones que hubo que ajustar>
- Lo que aprendimos: <pendiente>

### [2026-11-11] Fase 1: backend base
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude
- Rama / PR: feature/backend-base
- Prompt: "Tú harás todo a partir de aquí en automático", que continúa con la Fase 1 del prompt maestro.
- Resultado: API en Ktor con registro, login, Google y renovación de sesión (JWT con roles DONADOR, HOSPITAL y ADMIN); perfil del donador con ubicación aproximada; cálculo de elegibilidad con reglas editables en MongoDB; hospitales y cuentas de banco de sangre; Swagger completo; 6 archivos de pruebas con repositorios en memoria; colección de Postman con 23 requests probados por Newman en el CI; `render.yaml` para desplegar gratis.
- Decisiones: inyección de dependencias manual (`core/di`) en lugar de Koin, para no depender de una librería más y poder explicarla fácil. Las validaciones compartidas viven en `core/validacion` para que una funcionalidad no dependa de otra.
- Verificación: compilado y probado con Gradle. 42 pruebas en verde (rutas, elegibilidad, compatibilidad sanguínea y mapeo BSON). La colección de Postman corrió con Newman contra el servidor real con repositorios en memoria: 23 requests y 49 verificaciones sin fallas. La conexión con un MongoDB real la prueba el CI de GitHub.
- Ajustes tras compilar: se quitó el plugin de Gradle de Ktor (su repositorio no era accesible) y la imagen Docker usa `installDist` del plugin `application`.
- Decisiones y ajustes humanos: <pendiente>
- Lo que aprendimos: <pendiente>

### [2026-11-12] Fase 2: app base
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude
- Rama / PR: feature/app-base
- Prompt: "Tú harás todo a partir de aquí en automático", que continúa con la Fase 2 del prompt maestro.
- Resultado: tema de Compose generado de `docs/design/TOKENS.md` (colores claro/oscuro, Bricolage Grotesque y Figtree como fuentes locales con licencia OFL), componentes base, bienvenida con consentimiento y aviso de privacidad, registro, inicio de sesión (correo y Google con Credential Manager), perfil del donador (tipo de sangre, nacimiento, peso, sexo para criterios médicos, ubicación aproximada o municipio, última donación), inicio con el estado de donación, ajustes con cambio de idioma y cierre de sesión. Sesión en DataStore con renovación automática del token; copia del perfil y la elegibilidad en Room para verlos sin conexión; Hilt; navegación con rutas con tipo.
- Decisiones: ubicación con el `LocationManager` de Android y permiso de ubicación aproximada (sin Google Play Services); la app solo pide la ubicación aproximada y el backend además la redondea. Se subió a AGP 9.5 y compileSdk 37 porque las versiones actuales de AndroidX ya lo piden. `MainActivity` es `AppCompatActivity` para que el cambio de idioma funcione desde Android 8.
- Verificación: compilado con Gradle; 32 pruebas unitarias en verde (validadores, cálculo del estado de donación, lectura de errores del backend y los ViewModels de registro, inicio de sesión, perfil e inicio con repositorios falsos); lint sin advertencias; APK de depuración generado. Las pantallas no se vieron en un emulador en esta sesión: falta abrir la app en Android Studio y revisarlas contra el diseño.
- Decisiones y ajustes humanos: <pendiente: revisión en emulador o teléfono>
- Lo que aprendimos: <pendiente>

### [2026-11-12] Fase 3: urgencias, cola de mensajes y avisos push
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude
- Rama / PR: feature/urgencias
- Prompt: "Acaba con lo que falte de esta fase y continúa con la que sigue, hay que terminar hoy".
- Resultado: backend con urgencias (publicar, ver cercanas y compatibles, aceptar, cerrar), búsqueda de donadores con `$geoNear`, RabbitMQ con una sola conexión, un canal por consumidor, reintentos, colas de fallidos y colas de retraso para las oleadas (10, 25 y 50 km cada 15 minutos), idempotencia con `eventos_procesados`, y avisos push con Firebase Cloud Messaging HTTP v1. En la app: urgencias cercanas en el inicio, detalle con mapa de OpenStreetMap y contador "2 de 3" en vivo, respuesta sin conexión con cola de salida (Room + WorkManager), panel del hospital (publicar, ver quién aceptó, cerrar) y notificaciones en español e inglés.
- Decisiones: las funcionalidades se hablan por interfaces en `core/dominio` (`DirectorioDonadores`, `DirectorioHospitales`, `DirectorioUsuarios`) para no importar clases internas de otra. Las notificaciones son mensajes de datos y la app arma el texto con `strings.xml`, así salen en el idioma del teléfono. Firebase se inicializa con datos de `local.properties` en lugar de `google-services.json`, para que el CI compile sin secretos. El contador en vivo se actualiza consultando cada 5 a 10 segundos (más simple que WebSockets y funciona con Render gratis).
- Verificación: 46 pruebas del backend en verde (incluye oleadas, compatibilidad, contador y permisos); la colección de Postman con 37 requests y 75 verificaciones pasó con Newman contra el servidor local; 45 pruebas unitarias de la app (incluye la cola de salida en modo avión); lint sin advertencias. `$geoNear` contra MongoDB real y RabbitMQ real se prueban en el CI.
- Decisiones y ajustes humanos: <pendiente: crear CloudAMQP y Firebase, probar el push en un teléfono>
- Lo que aprendimos: <pendiente>

### [2026-11-12] Fase 4: citas, check-in con QR e historial
- Integrante: <nombre de quien lo pidió>
- Herramienta: Claude
- Rama / PR: feature/citas
- Prompt: "Acaba con lo que falte de esta fase y continúa con la que sigue, hay que terminar hoy" (continuación automática).
- Resultado: backend con horarios por hospital (8:00 a 13:30, 3 lugares por media hora), agendar y cancelar cita, check-in con el QR (`gota:<cita>:<código>`), historial con insignias y "vidas que puedes ayudar", estadísticas del hospital, y dos consumidores nuevos: RecordatorioConsumer (avisos 24 h y 2 h antes) e HistorialConsumer (registra la donación, recalcula la próxima fecha y manda el agradecimiento). En la app: agendar desde la urgencia aceptada con requisitos vigentes, "Mi cita" con QR generado en el teléfono, agregar al calendario y ruta al hospital, historial con insignias, y en el panel del hospital escanear el QR con CameraX + ML Kit (o escribir el código), citas de hoy y estadísticas.
- Decisiones: RabbitMQ no permite "entregar a tal hora", así que los recordatorios "brincan" por colas de retraso fijas de 60, 15, 5 y 1 minutos hasta su hora, para no crear una cola por cada cita. El QR se genera en el teléfono con ZXing y la cita se guarda en Room: el donador puede mostrarlo sin señal dentro del hospital. Agendar sin conexión también va a la cola de salida.
- Verificación: 49 pruebas del backend en verde; la colección de Postman con 50 requests y 100 verificaciones pasó con Newman (incluye el flujo completo: registrar → urgencia → aceptar → agendar → check-in con QR → historial); 51 pruebas unitarias de la app; lint sin advertencias.
- Decisiones y ajustes humanos: <pendiente: probar el escaneo con dos teléfonos>
- Lo que aprendimos: <pendiente>
