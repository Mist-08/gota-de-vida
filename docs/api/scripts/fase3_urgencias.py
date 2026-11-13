"""Fase 3: carpeta 5. Solicitud (urgencias). Ejecutar: python3 docs/api/scripts/fase3_urgencias.py"""
from postman import ESPERAR, cargar, carpeta, guardar, req, variables

c = cargar()
variables(c, "solicitudId", "emailDonadorA", "tokenDonadorA")
OK200 = 'pm.test("Responde 200", () => pm.response.to.have.status(200));'

items = [
    req("Registrar el teléfono del donador para avisos", "PUT", "/api/v1/usuarios/me/dispositivo", "tokenDonador",
        {"tokenFcm": "token-fcm-de-prueba-postman-0001"},
        ['pm.test("Responde 204", () => pm.response.to.have.status(204));']),
    req("Un donador no puede publicar urgencias (403)", "POST", "/api/v1/solicitudes", "tokenDonador",
        {"tipoSangre": "O_NEGATIVO", "unidades": 2},
        ['pm.test("Responde 403", () => pm.response.to.have.status(403));']),
    req("Unidades fuera de rango (400)", "POST", "/api/v1/solicitudes", "tokenHospital",
        {"tipoSangre": "O_NEGATIVO", "unidades": 0},
        ['pm.test("Responde 400", () => pm.response.to.have.status(400));']),
    req("Hospital publica urgencia O−", "POST", "/api/v1/solicitudes", "tokenHospital",
        {"tipoSangre": "O_NEGATIVO", "unidades": 2, "nota": "Paciente en quirófano, piso 3"},
        ['pm.test("Responde 201", () => pm.response.to.have.status(201));',
         'const r = pm.response.json();',
         'pm.test("Activa y sin unidades cubiertas", () => { pm.expect(r.estado).to.eql("ACTIVA"); pm.expect(r.unidadesComprometidas).to.eql(0); });',
         'pm.test("Etiqueta O−", () => pm.expect(r.etiquetaTipoSangre).to.eql("O\\u2212"));',
         'pm.collectionVariables.set("solicitudId", r.id);'],
        descripcion="Publica el evento solicitud.creada; el MatchingConsumer avisa a los donadores compatibles más cercanos."),
    req("Registrar donador A+", "POST", "/api/v1/auth/registro", None,
        {"nombre": "Jorge Salas", "email": "{{emailDonadorA}}", "password": "contrasena-segura", "telefono": "871 765 4321", "aceptaAvisoPrivacidad": True},
        ['pm.test("Responde 201", () => pm.response.to.have.status(201));',
         'pm.collectionVariables.set("tokenDonadorA", pm.response.json().tokens.tokenAcceso);'],
        antes=['pm.collectionVariables.set("emailDonadorA", `jorge.${Date.now()}@correo.mx`);']),
    req("Perfil del donador A+ cerca del hospital", "PUT", "/api/v1/donadores/me", "tokenDonadorA",
        {"tipoSangre": "A_POSITIVO", "fechaNacimiento": "1998-07-21", "pesoKg": 80, "sexoParaCriteriosMedicos": "MASCULINO",
         "latitud": 25.545, "longitud": -103.41, "zona": "Centro, Torreón"},
        [OK200]),
    req("Una urgencia O− no le aparece a un donador A+", "GET", "/api/v1/solicitudes", "tokenDonadorA", None,
        [OK200, 'pm.test("No ve la urgencia O−", () => pm.expect(pm.response.json().map(s => s.id)).to.not.include(pm.collectionVariables.get("solicitudId")));']),
    req("El donador O− ve la urgencia cerca", "GET", "/api/v1/solicitudes?radioKm=15", "tokenDonador", None,
        [OK200,
         'const s = pm.response.json().find(x => x.id === pm.collectionVariables.get("solicitudId"));',
         'pm.test("Aparece la urgencia", () => pm.expect(s).to.be.an("object"));',
         'pm.test("Con distancia aproximada y sin responder", () => { pm.expect(s.distanciaKm).to.be.below(2); pm.expect(s.miRespuesta).to.eql(undefined); });']),
    req("El donador A+ no puede aceptar una urgencia O− (409)", "POST", "/api/v1/solicitudes/{{solicitudId}}/respuestas", "tokenDonadorA",
        {"acepta": True}, ['pm.test("Responde 409", () => pm.response.to.have.status(409));']),
    req("El donador O− acepta la urgencia", "POST", "/api/v1/solicitudes/{{solicitudId}}/respuestas", "tokenDonador",
        {"acepta": True},
        [OK200, 'const r = pm.response.json();',
         'pm.test("Contador 1 de 2", () => { pm.expect(r.unidadesComprometidas).to.eql(1); pm.expect(r.unidades).to.eql(2); });',
         'pm.test("Queda registrada su respuesta", () => pm.expect(r.miRespuesta).to.eql(true));']),
    req("Hospital ve la urgencia en vivo", "GET", "/api/v1/solicitudes/{{solicitudId}}", "tokenHospital", None,
        [OK200, 'const r = pm.response.json();',
         'pm.test("El SolicitudConsumer actualizó el contador", () => pm.expect(r.unidadesComprometidas).to.eql(1));',
         'pm.test("El MatchingConsumer avisó a la primera oleada", () => { pm.expect(r.oleada).to.eql(1); pm.expect(r.donadoresNotificados).to.be.at.least(1); });',
         'pm.test("Ve nombre y teléfono de quien aceptó, nunca su ubicación", () => { const a = r.aceptaciones[0]; pm.expect(a.nombre).to.eql("Mariana Rodríguez"); pm.expect(a.telefono).to.be.a("string"); pm.expect(a).to.not.have.property("latitud"); });'],
        antes=ESPERAR),
    req("Urgencias de mi hospital", "GET", "/api/v1/hospitales/me/solicitudes", "tokenHospital", None,
        [OK200, 'pm.test("Incluye la urgencia publicada", () => pm.expect(pm.response.json().map(s => s.id)).to.include(pm.collectionVariables.get("solicitudId")));']),
    req("Hospital cierra la urgencia", "PATCH", "/api/v1/solicitudes/{{solicitudId}}/estado", "tokenHospital",
        {"estado": "CERRADA"}, [OK200, 'pm.test("Cerrada", () => pm.expect(pm.response.json().estado).to.eql("CERRADA"));']),
    req("No se puede aceptar una urgencia cerrada (409)", "POST", "/api/v1/solicitudes/{{solicitudId}}/respuestas", "tokenDonadorA",
        {"acepta": True}, ['pm.test("Responde 409", () => pm.response.to.have.status(409));']),
]
carpeta(c, "5. Solicitud", "Urgencias: publicar, ver cercanas y compatibles, aceptar y contador en vivo.", items)
guardar(c)
print(len(items), "requests en 5. Solicitud")
