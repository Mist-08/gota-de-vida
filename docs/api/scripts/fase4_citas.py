"""Fase 4: carpeta 6. Cita y donación. Ejecutar: python3 docs/api/scripts/fase4_citas.py"""
from postman import ESPERAR, cargar, carpeta, guardar, req, variables

c = cargar()
variables(c, "manana", "citaId", "codigoQr")
OK200 = 'pm.test("Responde 200", () => pm.response.to.have.status(200));'
MANANA = [
    "// Mañana en La Laguna (UTC−6), formato AAAA-MM-DD.",
    "const d = new Date(Date.now() - 6 * 3600 * 1000 + 24 * 3600 * 1000);",
    'pm.collectionVariables.set("manana", d.toISOString().substring(0, 10));',
]

items = [
    req("Horarios disponibles mañana", "GET", "/api/v1/hospitales/{{hospitalId}}/horarios?fecha={{manana}}", "tokenDonador", None,
        [OK200, "const h = pm.response.json();",
         'pm.test("12 horarios de 8:00 a 13:30", () => { pm.expect(h.length).to.eql(12); pm.expect(h[0].hora).to.eql("08:00"); });',
         'pm.test("Con 3 lugares cada uno", () => pm.expect(h.every(x => x.lugares === 3 && x.disponible)).to.eql(true));'],
        antes=MANANA),
    req("Horario que no existe (400)", "POST", "/api/v1/citas", "tokenDonador",
        {"hospitalId": "{{hospitalId}}", "fechaHora": "{{manana}}T07:15"},
        ['pm.test("Responde 400", () => pm.response.to.have.status(400));']),
    req("Agendar cita mañana a las 9:00", "POST", "/api/v1/citas", "tokenDonador",
        {"hospitalId": "{{hospitalId}}", "fechaHora": "{{manana}}T09:00"},
        ['pm.test("Responde 201", () => pm.response.to.have.status(201));',
         "const r = pm.response.json();",
         'pm.test("Agendada con código QR", () => { pm.expect(r.estado).to.eql("AGENDADA"); pm.expect(r.codigoQr).to.be.a("string"); });',
         'pm.collectionVariables.set("citaId", r.id);',
         'pm.collectionVariables.set("codigoQr", r.codigoQr);'],
        descripcion="Publica cita.agendada: el RecordatorioConsumer programa avisos 24 h y 2 h antes."),
    req("No se puede tener dos citas a la vez (409)", "POST", "/api/v1/citas", "tokenDonador",
        {"hospitalId": "{{hospitalId}}", "fechaHora": "{{manana}}T10:00"},
        ['pm.test("Responde 409", () => pm.response.to.have.status(409));']),
    req("El horario ocupado tiene un lugar menos", "GET", "/api/v1/hospitales/{{hospitalId}}/horarios?fecha={{manana}}", "tokenDonador", None,
        [OK200, 'pm.test("Quedan 2 lugares a las 9:00", () => pm.expect(pm.response.json().find(x => x.hora === "09:00").lugares).to.eql(2));']),
    req("Mis citas", "GET", "/api/v1/citas/me", "tokenDonador", None,
        [OK200, 'pm.test("Incluye la cita", () => pm.expect(pm.response.json().map(x => x.id)).to.include(pm.collectionVariables.get("citaId")));']),
    req("El hospital ve las citas de mañana", "GET", "/api/v1/hospitales/me/citas?fecha={{manana}}", "tokenHospital", None,
        [OK200, "const r = pm.response.json().find(x => x.id === pm.collectionVariables.get(\"citaId\"));",
         'pm.test("Con el nombre del donador y sin el código", () => { pm.expect(r.donadorNombre).to.eql("Mariana Rodríguez"); pm.expect(r).to.not.have.property("codigoQr"); });']),
    req("Código QR falso (409)", "POST", "/api/v1/citas/checkin-qr", "tokenHospital",
        {"codigoQr": "gota:{{citaId}}:codigo-falso"},
        ['pm.test("Responde 409", () => pm.response.to.have.status(409));']),
    req("Un donador no puede hacer check-in (403)", "POST", "/api/v1/citas/checkin-qr", "tokenDonador",
        {"codigoQr": "gota:{{citaId}}:{{codigoQr}}"},
        ['pm.test("Responde 403", () => pm.response.to.have.status(403));']),
    req("El hospital escanea el QR y confirma la donación", "POST", "/api/v1/citas/checkin-qr", "tokenHospital",
        {"codigoQr": "gota:{{citaId}}:{{codigoQr}}"},
        [OK200, 'pm.test("Cita completada", () => pm.expect(pm.response.json().estado).to.eql("COMPLETADA"));'],
        descripcion="Publica donacion.confirmada: el HistorialConsumer registra la donación y recalcula la próxima fecha."),
    req("Historial actualizado con insignia", "GET", "/api/v1/donaciones/me", "tokenDonador", None,
        [OK200, "const r = pm.response.json();",
         'pm.test("Una donación registrada", () => { pm.expect(r.totalDonaciones).to.eql(1); pm.expect(r.vidasQuePuedesAyudar).to.eql(3); });',
         'pm.test("Insignia de primera donación", () => pm.expect(r.insignias.find(i => i.codigo === "PRIMERA_DONACION").obtenida).to.eql(true));',
         'pm.test("Ya no puede donar hasta dentro de 60 días", () => { pm.expect(r.elegible).to.eql(false); pm.expect(r.proximaFechaElegible).to.be.a("string"); });'],
        antes=ESPERAR),
    req("Quien donó hace poco recibe \"no elegible\"", "GET", "/api/v1/donadores/me/elegibilidad", "tokenDonador", None,
        [OK200, 'pm.test("No elegible con fecha próxima", () => { const r = pm.response.json(); pm.expect(r.elegible).to.eql(false); pm.expect(r.diasParaVolverADonar).to.be.above(50); });']),
    req("Estadísticas del hospital", "GET", "/api/v1/hospitales/me/estadisticas", "tokenHospital", None,
        [OK200, "const r = pm.response.json();",
         'pm.test("Cuenta urgencias y donaciones", () => { pm.expect(r.urgenciasTotales).to.be.at.least(1); pm.expect(r.donacionesConfirmadas).to.eql(1); });']),
]
carpeta(c, "6. Cita y donación", "Horarios, agenda, check-in con QR, historial y estadísticas. Es el final del flujo completo de la demo.", items)
guardar(c)
print(len(items), "requests en 6. Cita y donación")
