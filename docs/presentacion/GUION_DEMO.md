# Guion de la presentación y la demo

- **Diapositivas (5 min):** artifact de Claude "Gota de Vida — Presentación" (https://claude.ai/artifact/2nL2ni11jg3Etcdmu1Pzwv). Es privado: quien lo creó debe compartirlo desde su menú *Share* para que los demás lo abran. Se puede descargar como PowerPoint o PDF. Cada diapositiva trae notas del orador.
- **Demo (15 min):** este documento.
- Reparto sugerido: **Integrante 1** presenta y opera el teléfono del hospital, **Integrante 2** opera el teléfono del donador, **Integrante 3** muestra Swagger, GitHub y RabbitMQ en la computadora.

## Un día antes

- [ ] Render: variables `MONGODB_URI`, `JWT_SECRETO`, `RABBITMQ_URL`, `FIREBASE_CREDENCIALES_JSON`, `GEMINI_API_KEY`, `WHATSAPP_*`, `CARGAR_DATOS_DEMO=true`, `DEMO_PASSWORD` y **`MINUTOS_ENTRE_OLEADAS=1`** (así la segunda oleada llega en un minuto).
- [ ] WhatsApp: los celulares de los 3 integrantes y del profesor registrados como destinatarios del número de prueba; plantilla `urgencia_donacion` aprobada; token de usuario del sistema (no el de 24 h).
- [ ] Teléfono del **donador**: APK instalado, sesión con `donador1@demo.gotadevida.mx` (Ana, O−, Torreón) o con la cuenta de un integrante cuyo celular esté en WhatsApp; notificaciones permitidas; widget "Mi próxima donación" en la pantalla de inicio.
- [ ] Teléfono del **hospital**: sesión con `banco1@demo.gotadevida.mx` (Hospital Universitario de Torreón).
- [ ] Computadora: pestañas abiertas con `/swagger`, el repositorio en GitHub (pestaña *Pull requests* y *Actions*), el panel de CloudAMQP (*RabbitMQ Manager → Queues*) y Postman con la colección importada.
- [ ] Ensayar todo una vez con cronómetro.

## 10 minutos antes

- [ ] Abrir `https://<servicio>.onrender.com/health` para despertar el backend (tarda cerca de 1 minuto).
- [ ] Wi-Fi o datos en los dos teléfonos; modo avión **apagado**.
- [ ] Brillo al máximo y "No molestar" apagado en el teléfono del donador.

## Guion (15 minutos)

| Min | Quién | Qué se hace | Qué se dice |
| --- | --- | --- | --- |
| 0:00 | Donador | Abre la app: inicio con "Puedes donar", su tipo O− y el widget. | "En 3 segundos sabe si puede donar." |
| 1:00 | Donador | Ajustes → cambia a inglés y regresa a español. | Extra de idiomas: todo está en `strings.xml` en dos idiomas. |
| 2:00 | Hospital | Panel → **Publicar urgencia**: O−, 2 unidades, nota "Banco de sangre, planta baja". | "El hospital publica y la API lanza el evento `solicitud.creada`." |
| 2:30 | Compu | CloudAMQP: se ven las colas `gota.matching`, `gota.notificador`… y los mensajes que pasaron. | Extra de colas: una sola conexión, un consumidor por cola, colas de fallidos. |
| 3:00 | Donador | Llega el **push** y el **WhatsApp** con la plantilla. Abre el push: detalle con mapa, distancia y "0 de 2". | "Solo le llegó a compatibles, cerca y que ya pueden donar: `$geoNear` en MongoDB." |
| 4:30 | Donador | **Activa modo avión** y toca **Sí puedo donar**: aparece "Pendiente de enviar". | Extra de Room: la acción queda en la cola de salida. |
| 5:30 | Donador | Quita el modo avión: en segundos se envía y cambia a "¡Gracias!". | WorkManager la mandó al volver la señal. |
| 6:00 | Hospital | El detalle muestra **1 de 2** y el nombre y teléfono de quien aceptó (nunca su ubicación). | Contador en vivo y privacidad. |
| 7:00 | Donador | WhatsApp: escribe "¿tengo que ir en ayunas?" y luego "3" (agendar). Gota ofrece horarios; responde con una hora. | Chatbot: preguntas frecuentes sin gastar cuota, luego agenda. Si Gemini no tiene cuota, el menú sigue funcionando. |
| 9:00 | Donador | App → tarjeta **Tu cita** → QR. **Agregar a mi calendario** y **Abrir ruta**. | QR generado en el teléfono; funciona sin señal en el hospital. |
| 10:30 | Hospital | **Escanear QR de una cita** → apunta al QR del donador → "Donación confirmada". | Evento `donacion.confirmada`: el HistorialConsumer hace el resto. |
| 11:30 | Donador | **Mi historial**: 1 donación, insignia "Primera donación", "Podrás volver a donar a partir del…". El widget cambió a días de espera. | La elegibilidad se recalcula sola con las reglas editables. |
| 12:30 | Compu | Swagger: `/api/v1/solicitudes` y su documentación. Postman: correr la carpeta **5. Solicitud**. | API propia en la nube, documentada y probada. |
| 13:30 | Compu | GitHub: un Pull Request con su revisión y el CI en verde; `docs/ai/BITACORA.md`. | Git colaborativo, CI/CD y vibecoding documentado. |
| 14:30 | Todos | Cierre. | "Gracias. ¿Preguntas?" |

## Si algo falla

| Falla | Qué hacer |
| --- | --- |
| Render no despierta | Esperar 1 minuto con `/health`; mientras, mostrar Swagger local o la colección de Postman. |
| No llega el push | Mostrar la urgencia en la lista "Urgencias cerca" del inicio (desliza hacia abajo para actualizar). |
| WhatsApp no responde | Hacer el mismo paso en la pantalla **Hablar con Gota** de la app: es el mismo asistente. |
| Gemini sin cuota | Es parte de la demo: Gota contesta con el menú (1, 2, 3, 4) y agenda igual. |
| La cámara no lee el QR | En la pantalla de escaneo, escribir el código: `gota:<id de la cita>:<código>` (se ve en Swagger con `GET /api/v1/hospitales/me/citas`). |
| No hay internet en el salón | Compartir datos desde un celular. El modo sin conexión de la app se puede mostrar igual. |

## Preguntas que puede hacer el profesor

Practiquen estas respuestas; son las partes más difíciles del proyecto (sección 16 del prompt maestro).

1. **¿Cómo encuentran a los donadores cercanos?** Con `$geoNear` de MongoDB sobre un índice `2dsphere`: filtra por tipos de sangre compatibles y notificaciones activas, y ordena por distancia. Después revisamos en Kotlin que ya puedan donar con las reglas vigentes (`DonadorService.candidatos`).
2. **¿Qué son las oleadas?** El MatchingConsumer avisa a los 20 más cercanos dentro de 10 km y deja un mensaje con retraso en RabbitMQ (cola con TTL y dead-letter). Si a los 15 minutos la urgencia sigue activa, amplía a 25 y luego a 50 km sin repetir donadores.
3. **¿Qué pasa si RabbitMQ entrega un mensaje dos veces?** Cada consumidor marca el evento en `eventos_procesados` con un `_id` único; si ya estaba, lo ignora. El contador de unidades se recalcula desde cero, así que repetirlo no suma de más.
4. **¿Cómo funciona sin conexión?** El repositorio muestra primero lo de Room. Si una acción falla por red, se guarda en `acciones_pendientes` y WorkManager la manda con reintentos y espera exponencial cuando vuelve la señal.
5. **¿Por qué los consumidores corren dentro de la API?** Render gratis no tiene workers en segundo plano. Cada consumidor está en su propio paquete para poder separarlo después.
6. **¿Cómo protegen los datos?** Contraseñas con BCrypt, JWT con roles y renovación, ubicación redondeada a unos 100 m, el hospital solo ve distancia y, al aceptar, nombre y teléfono. A Gemini no le llegan datos personales y el donador puede borrar su cuenta.
7. **¿Cómo sabe el sistema si alguien puede donar?** Con reglas guardadas en MongoDB (edad, peso, días entre donaciones, máximo al año por sexo, tatuajes) que el ADMIN puede cambiar sin tocar el código, porque la NOM-253-SSA1-2025 remite los criterios a una guía que puede cambiar. La decisión final siempre es del personal médico.
