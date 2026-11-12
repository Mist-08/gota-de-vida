# Tokens de diseño — Gota de Vida

Fuente de verdad: el sistema de diseño "Gota de Vida" en Claude Design (enlace en `docs/design/README.md`). El tema de Compose (`core/designsystem`) se genera a partir de estos valores. Si algo cambia en Claude Design, se actualiza aquí en el mismo Pull Request.

## Colores

| Token | Claro | Oscuro | Uso | Rol Material 3 |
| --- | --- | --- | --- | --- |
| `surface` | `#FFFBFA` | `#151011` | Fondo de pantalla | `background`, `surface` |
| `surface-raised` | `#FFFFFF` | `#1F1819` | Tarjetas, hojas, diálogos | `surfaceContainerLowest` (claro) / `surfaceContainerHigh` (oscuro) |
| `surface-sunken` | `#F6EDEC` | `#0F0B0C` | Campos, barra inferior | `surfaceContainer` |
| `ink` | `#231819` | `#F5ECEB` | Texto principal | `onSurface`, `onBackground` |
| `ink-muted` | `#6A5657` | `#BCA9AA` | Texto secundario | `onSurfaceVariant` |
| `outline` | `#DCCBCB` | `#463839` | Divisores | `outlineVariant` |
| `outline-strong` | `#8C7778` | `#8F7B7C` | Borde de controles | `outline` |
| `sangre` | `#B4161F` | `#FF8C85` | Acción principal, urgencias | `primary` |
| `on-sangre` | `#FFFFFF` | `#410507` | Texto sobre sangre | `onPrimary` |
| `sangre-soft` | `#FCE4E2` | `#5A1518` | Chip de tipo de sangre, burbuja del asistente | `primaryContainer` |
| `on-sangre-soft` | `#6E0B10` | `#FFD9D6` | Texto sobre sangre-soft | `onPrimaryContainer` |
| `vida` | `#1C7547` | `#73D49E` | Elegible, confirmado | `secondary` |
| `vida-soft` | `#DDF3E6` | `#123D27` | Fondo de "Puedes donar" | `secondaryContainer` |
| `aviso` | `#8A5300` | `#F3BF5C` | En espera, sin conexión | `tertiary` |
| `aviso-soft` | `#FFF0D6` | `#4A3000` | Fondo de avisos | `tertiaryContainer` |
| `info` | `#1E5AA8` | `#94BEF5` | Enlaces, requisitos | extensión propia `GotaColors.info` |
| `error` | `#9C1C5A` | `#FF9BC6` | Errores (magenta, no rojo) | `error` |

Todos los pares de texto cumplen contraste mínimo de 4.5:1 en ambos temas. No usar color dinámico de Material You.

## Tipografía

Fuentes de Google Fonts, cargadas como fuentes descargables en Compose (`androidx.compose.ui:ui-text-google-fonts`):
- **Bricolage Grotesque** para `numeral`, `display` y `headline`.
- **Figtree** para todo lo demás.

| Estilo | Familia | Tamaño | Interlineado | Peso | Espaciado | Material 3 |
| --- | --- | --- | --- | --- | --- | --- |
| `numeral` | Bricolage Grotesque | 44 sp | 48 sp | 800 | −0.5 sp | `displayMedium` (cifras tabulares) |
| `display` | Bricolage Grotesque | 32 sp | 38 sp | 700 | −0.4 sp | `headlineLarge` |
| `headline` | Bricolage Grotesque | 24 sp | 30 sp | 700 | −0.2 sp | `headlineSmall` |
| `title` | Figtree | 18 sp | 24 sp | 600 | 0 | `titleMedium` |
| `body` | Figtree | 16 sp | 24 sp | 400 | 0 | `bodyLarge` |
| `body-sm` | Figtree | 14 sp | 20 sp | 400 | 0 | `bodyMedium` |
| `label` | Figtree | 14 sp | 20 sp | 600 | 0.1 sp | `labelLarge` |
| `caption` | Figtree | 12 sp | 16 sp | 500 | 0.2 sp | `labelSmall` |

## Espaciado

| Token | Valor | Uso |
| --- | --- | --- |
| `space-1` | 4 dp | Ícono y texto dentro de un chip |
| `space-2` | 8 dp | Entre chips y líneas de metadatos |
| `space-3` | 12 dp | Dentro de una tarjeta |
| `space-4` | 16 dp | Margen lateral y padding de tarjetas |
| `space-6` | 24 dp | Entre secciones |
| `space-8` | 32 dp | Aire sobre el encabezado principal |

## Radios

| Token | Valor | Uso |
| --- | --- | --- |
| `radius-sm` | 8 dp | Campos de texto |
| `radius-md` | 16 dp | Tarjetas, botones, burbujas |
| `radius-lg` | 28 dp | Hojas inferiores, tarjeta de estado, QR |
| `radius-full` | 50 % | Chips de tipo de sangre, insignias, progreso |

## Elevación

`elevation-1`: solo tarjetas de urgencia y hojas inferiores. Claro: sombra suave rojiza (`#231819` al 6–8 %); oscuro: sombra negra al 50 %. Lo demás se separa por color de superficie.

## Otras reglas

- Área táctil mínima: 48 × 48 dp.
- Íconos: Material Symbols Rounded, 24 dp.
- Tipos de sangre con signo de menos real (`O−`, U+2212).
- Una sola acción principal en `sangre` por pantalla.
