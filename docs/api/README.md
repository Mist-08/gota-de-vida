# API — colección de Postman

| Archivo | Para qué |
| --- | --- |
| `GotaDeVida.postman_collection.json` | 23 requests en 5 carpetas, cada uno con pruebas |
| `local.postman_environment.json` | `baseUrl` = `http://localhost:8080` |
| `render.postman_environment.json` | `baseUrl` = el servicio en Render |
| `ci.postman_environment.json` | Lo usa GitHub Actions con Newman (credenciales de prueba, solo para el CI) |

## Uso en Postman

1. **Import** → seleccionen la colección y los entornos.
2. Elijan el entorno (*Local* o *Render*).
3. En el entorno, escriban `adminEmail` y `adminPassword` como **valor actual** (el valor actual no se exporta; nunca pongan la contraseña real en el archivo).
4. Clic derecho en la colección → **Run collection**. Corre completa y en orden: cada carpeta usa variables que guarda la anterior (tokens, id del hospital).

El plan gratis de Postman es para una sola persona: la colección se comparte por el repositorio y cada integrante la importa en su cuenta.

## Qué prueba

1. **Salud:** el servidor responde.
2. **Autenticación:** registro, duplicado (409), sin aviso de privacidad (400), login, contraseña incorrecta (401), renovar sesión, login de administrador.
3. **Donador:** sin token (401), perfil incompleto, completar perfil (ubicación redondeada, `O−` con signo real), elegible, fecha futura (400).
4. **Reglas:** públicas, un donador no puede cambiarlas (403), valor fuera de rango (400).
5. **Hospitales:** un donador no puede crearlos (403), alta de hospital y de cuenta del banco de sangre, login como HOSPITAL, hospital inexistente (404).

## En el CI

`.github/workflows/backend-ci.yml` levanta MongoDB y el backend en el runner y corre esta misma colección con Newman. Si una prueba falla, el PR no se puede fusionar.

Cada endpoint nuevo se agrega aquí y en Swagger (`backend/src/main/resources/openapi/documentation.yaml`) en el mismo Pull Request.
