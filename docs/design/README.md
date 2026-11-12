# Diseño — Gota de Vida

Fase A del prompt maestro. Todo lo visual se definió en Claude Design antes de escribir código.

## Enlaces

- **Sistema de diseño:** https://claude.ai/artifact/DLqs9AM1BZuQ54gxKHxDPJ
- **Pantallas:** https://claude.ai/artifact/SFHmHaz1J9xZLuzWMezMer

Los dos son privados: el dueño debe compartirlos (menú Compartir) para que el resto del equipo los abra.

Los valores exactos de colores, tipografía y espaciados están en `TOKENS.md`.

## Pantallas

| # | Pantalla | Rol | Estado mostrado |
| --- | --- | --- | --- |
| 01 | Bienvenida y consentimiento | Donador | Consentimiento marcado |
| 02 | Registro (paso 1 de 2) | Donador | Formulario lleno, campo enfocado |
| 03 | Perfil del donador (paso 2 de 2) | Donador | O− seleccionado |
| 04 | Inicio | Donador | "Puedes donar" + 2 urgencias |
| 05 | Detalle de urgencia | Donador | 1 de 3 unidades cubiertas |
| 06 | Agendar cita | Donador | Jueves 9:30 seleccionado |
| 07 | Mi cita (QR) | Donador | Cita confirmada |
| 08 | Historial | Donador | "Podrás volver a donar en 60 días" |
| 09 | Asistente Gota | Donador | Conversación que termina en cita |
| 10 | Panel del hospital | Hospital | Urgencia activa con respuestas |
| 11 | Ajustes | Donador | Idioma, avisos y privacidad |
| — | Inicio oscuro | Donador | Sin conexión, acción pendiente de enviar |
| — | Urgencia oscuro | Donador | Tema oscuro |
| — | Asistente oscuro | Donador | Tema oscuro, en inglés |

## Estado

- [x] Revisión del equipo: aprobado el 11 de noviembre de 2026
- [ ] Exportar las pantallas aprobadas como imágenes a esta carpeta
