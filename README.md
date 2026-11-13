# Gota de Vida

App Android que conecta a los bancos de sangre de la Comarca Lagunera (Torreón, Gómez Palacio y Lerdo) con donadores compatibles y cercanos cuando hay una urgencia. Proyecto Integrador de Desarrollo Móvil con Android.

## Cómo funciona

1. El donador se registra con su tipo de sangre y su zona.
2. El hospital publica una urgencia: "Urge O−, 3 unidades".
3. Solo los donadores compatibles, cercanos y que ya pueden donar reciben el aviso, en la app y por WhatsApp.
4. El donador acepta y agenda su cita, también platicando con el asistente "Gota".
5. En el hospital escanean su QR y la donación queda en su historial.

## Estructura del repositorio

| Carpeta | Qué contiene |
| --- | --- |
| `app/` | App Android en Kotlin + Jetpack Compose (Screaming Architecture) |
| `backend/` | API REST en Ktor con Swagger en `/swagger` |
| `docs/ai/` | Prompt maestro, bitácora de uso de IA y prompts reutilizables |
| `docs/design/` | Sistema de diseño y pantallas (Claude Design), tokens para Compose |
| `docs/api/` | Colección y entornos de Postman |
| `.github/` | CI/CD con GitHub Actions y plantillas de PR e issues |

## Correr el proyecto

**App:** abre la carpeta `app/` en Android Studio y ejecuta en un emulador o teléfono. Antes copia `app/local.properties.example` como `app/local.properties`. Usa la versión más reciente de Android Studio (el proyecto usa AGP 9.5, compileSdk 37 y JDK 21). Con el backend corriendo en tu computadora, el emulador lo encuentra en `http://10.0.2.2:8080/`.

**Backend:** abre `backend/` en IntelliJ IDEA o Android Studio, copia `backend/.env.example` como `backend/.env` y ejecuta:

```bash
cd backend
./gradlew run
```

Luego abre `http://localhost:8080/health` y `http://localhost:8080/swagger`.

## Reglas del equipo

- Nadie sube cambios directo a `main`: todo entra por Pull Request con la aprobación de otro integrante y el CI en verde.
- Ramas: `feature/<funcionalidad>`, `fix/<descripcion>`, `docs/<tema>`, `design/<pantalla>`.
- Commits con Conventional Commits en español: `feat(solicitud): agrega búsqueda por oleadas`.
- Todo uso de IA se registra en `docs/ai/BITACORA.md`.

Primeros pasos para crear el repositorio en GitHub: `docs/PRIMEROS_PASOS.md`.
Base de datos y despliegue del backend: `docs/DESPLIEGUE.md`.

## Equipo

| Integrante | Responsabilidad |
| --- | --- |
| <Integrante 1> | Backend y nube |
| <Integrante 2> | App del donador |
| <Integrante 3> | Integraciones (colas, notificaciones, chatbot, CI/CD) |

## Estado

- [x] Fase A: diseño en Claude Design
- [x] Fase B: estructura del repositorio
- [x] Fase 1: backend base (falta desplegar en Render, ver docs/DESPLIEGUE.md)
- [x] Fase 2: app base (falta revisarla en un emulador)
- [x] Fase 3: urgencias (falta conectar CloudAMQP y Firebase, ver docs/DESPLIEGUE.md)
- [x] Fase 4: citas
- [x] Fase 5: chatbot (falta crear la llave de Gemini y la plantilla de WhatsApp)
- [x] Fase 6: pulido (falta el ensayo de la demo en teléfonos)
- [x] Fase 7: presentación (guion en docs/presentacion/GUION_DEMO.md; falta el ensayo cronometrado)
