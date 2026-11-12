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
