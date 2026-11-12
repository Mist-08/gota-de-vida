# Despliegue del backend (Fase 1)

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

## Antes de la demo

Render gratis se duerme después de 15 minutos sin uso y tarda cerca de un minuto en despertar. Abran `/health` unos minutos antes de exponer.
