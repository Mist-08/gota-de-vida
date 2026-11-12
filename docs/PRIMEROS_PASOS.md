# Primeros pasos: subir el proyecto a GitHub

La carpeta ya tiene la estructura completa. Estos pasos los hace el equipo con sus propias cuentas, porque así cada commit queda a nombre de quien lo hizo.

## 0. Antes de todo: renombrar la carpeta `github`

Los workflows de CI/CD, las plantillas y las instrucciones para Copilot están en la carpeta `github` (sin punto). Renómbrenla a **`.github`** antes del primer commit; si no, GitHub no los reconoce.

En el Explorador de Windows: clic derecho → Cambiar nombre → `.github`. O en una terminal dentro de la carpeta del proyecto:

```bash
ren github .github
```

## 1. Cada integrante (una vez)

1. Crea su cuenta de GitHub y activa el **GitHub Student Developer Pack** con su correo de la universidad (incluye GitHub Pro y Copilot gratis).
2. Instala **Git**, **Android Studio** y, para el backend, **IntelliJ IDEA Community** (o usa Android Studio también).
3. Configura su nombre y correo en Git, igual que en GitHub:

```bash
git config --global user.name "Nombre Apellido"
git config --global user.email "correo@ejemplo.com"
```

## 2. Quien tiene la carpeta: crear el repositorio

1. En GitHub: **New repository** → nombre `gota-de-vida` → público (o privado con el Student Pack) → **sin** README, sin .gitignore y sin licencia.
2. **Settings → Collaborators** → invita a los otros dos integrantes.
3. Abre una terminal en la carpeta del proyecto (`Documents\Android`) y sube un primer commit vacío, solo para que exista `main`:

```bash
git init -b main
git commit --allow-empty -m "chore: inicia el repositorio"
git remote add origin https://github.com/USUARIO/gota-de-vida.git
git push -u origin main
```

## 3. Misma persona: proteger `main`

**Settings → Branches → Add branch ruleset** (o *Add rule*) para `main`:

- Require a pull request before merging, con **1 aprobación**.
- Require status checks to pass: agreguen **Compilar y probar la app** y **Compilar y probar el backend** después de que corran por primera vez en el PR del paso 4.
- Block force pushes.
- Sin excepciones para administradores: así nadie, ni el dueño, sube directo a `main`.

## 4. Misma persona: subir la estructura por Pull Request

El contenido real entra por PR desde el primer día. En la misma terminal:

```bash
git switch -c chore/estructura-base
git add .
git commit -m "chore: estructura base del monorepo (app, backend, CI/CD y docs)"
git push -u origin chore/estructura-base
```

Abre el Pull Request en GitHub. Los dos workflows de CI corren solos.

## 5. Los otros dos integrantes

- **Integrante 2:** revisa el PR, lo aprueba cuando el CI esté en verde y lo fusiona.
- **Integrante 3:** clona el repositorio, crea la rama `docs/equipo`, pone los nombres del equipo en el `README.md` y los usuarios en `.github/CODEOWNERS`, y abre su PR. El integrante 1 lo revisa y lo fusiona.

Con esto los tres ya tienen aportes en el historial desde el primer día.

## 6. Lo que sigue

- **Render:** cuando exista el servicio (Fase 1), copien su *Deploy Hook* a **Settings → Secrets and variables → Actions** como `RENDER_DEPLOY_HOOK_URL` y desactiven *Auto-Deploy* en Render.
- **Tablero:** creen un GitHub Project con una columna por fase y un issue por tarea (plantilla *Funcionalidad*).
- **Diseño:** el dueño de los artifacts de Claude Design los comparte con el equipo (enlaces en `docs/design/README.md`).

## Si la primera compilación falla

Las versiones de las librerías están en `app/gradle/libs.versions.toml` y `backend/build.gradle.kts`. Si Android Studio o IntelliJ marcan alguna como inexistente o incompatible, acepten la versión que sugieran y regístrenlo en la bitácora.
