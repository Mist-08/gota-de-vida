# Despliegue del backend

Todo es gratis y sin tarjeta. Lo hace el responsable de backend y nube con su propia cuenta.

## 1. MongoDB Atlas (base de datos)

1. Crea una cuenta en MongoDB Atlas y un cluster **M0 (Free)**. Región: la más cercana disponible (por ejemplo, Estados Unidos).
2. **Database Access** → *Add New Database User* → usuario y contraseña (guárdalos, no los subas al repo).
3. **Network Access** → *Add IP Address* → `0.0.0.0/0`. Render gratis no tiene IP fija, así que hay que permitir todas; la protección la da el usuario y contraseña.
4. **Connect → Drivers** → copia la cadena `mongodb+srv://...` y reemplaza `<password>`.
5. En local: copia `backend/.env.example` como `backend/.env` y pega la cadena en `MONGODB_URI`.

Ojo: Atlas pausa el cluster gratis si pasa 30 días sin conexiones. Se reactiva desde el panel.

## 2. Probar en local

```bash
cd backend
./gradlew test   # pruebas (no necesitan MongoDB)
./gradlew run    # levanta la API con tu .env
```

- `http://localhost:8080/health` → `{"estado":"ok", ...}`
- `http://localhost:8080/swagger` → documentación interactiva
- En Postman: importa `docs/api/`, elige el entorno *Local*, pon `adminEmail` y `adminPassword` (los de tu `.env`) como **valor actual** y corre la colección completa.

## 3. Render (servidor en la nube)

1. Crea una cuenta en Render con GitHub y dale acceso al repositorio.
2. **New → Blueprint** → elige el repositorio. Render lee `render.yaml` y crea el servicio `gota-de-vida-backend` en el plan gratuito.
3. Te pedirá los valores marcados como secretos:
   - `MONGODB_URI`: la cadena de Atlas.
   - `ADMIN_EMAIL` y `ADMIN_PASSWORD`: la cuenta de administrador.
   - `GOOGLE_CLIENT_ID`: déjalo vacío por ahora (Fase 2).
   - `JWT_SECRET` se genera solo.
4. Espera el primer despliegue y abre `https://SU-SERVICIO.onrender.com/swagger`.
5. **Settings → Deploy Hook** → copia la URL. En GitHub: **Settings → Secrets and variables → Actions → New repository secret** → nombre `RENDER_DEPLOY_HOOK_URL`. Desde ahí, cada fusión a `main` con cambios en `backend/` despliega sola después del CI.
6. Cambia `SU-SERVICIO` por el nombre real en:
   - `backend/src/main/resources/openapi/documentation.yaml` (servers)
   - `docs/api/render.postman_environment.json` (baseUrl)

## 4. RabbitMQ en CloudAMQP (Fase 3, gratis)

1. Crea una cuenta en https://www.cloudamqp.com y una instancia **Little Lemur** (plan gratis, no pide tarjeta).
2. Copia la **AMQP URL** (empieza con `amqps://`) en la variable `RABBITMQ_URL` de Render.
3. Al arrancar, el backend crea solo el exchange `gota.eventos`, una cola por consumidor (`gota.matching`, `gota.solicitud`, `gota.notificador`), sus colas de fallidos (`.dlq`) y las colas de retraso. Puedes verlas en el panel de CloudAMQP (RabbitMQ Manager).
4. Sin `RABBITMQ_URL` el backend usa una cola en memoria con el mismo comportamiento: sirve para desarrollo, pero para la rúbrica la demo debe correr con CloudAMQP.

## 5. Firebase Cloud Messaging (Fase 3, gratis)

1. En https://console.firebase.google.com crea un proyecto (plan Spark, sin tarjeta) y agrega una app Android con el paquete `mx.gotadevida`.
2. **App:** de *Configuración del proyecto → General* copia en `app/local.properties` `FIREBASE_APP_ID` (ID de la app), `FIREBASE_API_KEY` (clave de API web), `FIREBASE_PROJECT_ID` y `FIREBASE_SENDER_ID` (número del proyecto). No hace falta `google-services.json`.
3. **Backend:** en *Configuración → Cuentas de servicio* genera una clave privada (JSON). Pégala completa (o en base64) en la variable `FIREBASE_CREDENCIALES_JSON` de Render. **Nunca la subas al repositorio.**
4. Sin estas variables la app y el backend funcionan igual; los avisos solo se registran en el log.

## Para la demo de urgencias

- Pon `MINUTOS_ENTRE_OLEADAS=1` en Render para que la segunda oleada llegue en un minuto y no en quince.
- Crea con la colección de Postman una cuenta de banco de sangre (carpeta 4) y entra con ella en la app: verás el panel del hospital.

## Antes de la demo

Render gratis se duerme después de 15 minutos sin uso y tarda cerca de un minuto en despertar. Abran `/health` unos minutos antes de exponer.
