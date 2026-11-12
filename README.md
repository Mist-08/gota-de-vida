# Gota de Vida

App Android que conecta a los bancos de sangre de la Comarca Lagunera (Torreón, Gómez Palacio y Lerdo) con donadores compatibles y cercanos cuando hay una urgencia.

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
| `backend/postman/` | Colección y entornos de Postman (el CI la corre con Newman) |
| `.github/workflows/` | CI/CD con GitHub Actions |

## Correr el proyecto

**App:** abre la carpeta `app/` en Android Studio y ejecuta en un emulador o teléfono. Antes copia `app/local.properties.example` como `app/local.properties`. Usa la versión más reciente de Android Studio (el proyecto usa AGP 9.5, compileSdk 37 y JDK 21). Con el backend corriendo en tu computadora, el emulador lo encuentra en `http://10.0.2.2:8080/`.

**Backend:** abre `backend/` en IntelliJ IDEA o Android Studio, copia `backend/.env.example` como `backend/.env` y ejecuta:

```bash
cd backend
./gradlew run
```

Luego abre `http://localhost:8080/health` y `http://localhost:8080/swagger`.
