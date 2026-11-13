"""Fase 6: carpeta 8. Cuenta (eliminar). Ejecutar: python3 docs/api/scripts/fase6_cuenta.py"""
from postman import cargar, carpeta, guardar, req, variables

c = cargar()
variables(c, "emailBorrar", "tokenBorrar")
items = [
    req("Registrar una cuenta para borrarla", "POST", "/api/v1/auth/registro", None,
        {"nombre": "Cuenta Temporal", "email": "{{emailBorrar}}", "password": "contrasena-segura", "telefono": "871 555 0000", "aceptaAvisoPrivacidad": True},
        ['pm.test("Responde 201", () => pm.response.to.have.status(201));',
         'pm.collectionVariables.set("tokenBorrar", pm.response.json().tokens.tokenAcceso);'],
        antes=['pm.collectionVariables.set("emailBorrar", `temporal.${Date.now()}@correo.mx`);']),
    req("Un hospital no puede borrarse a sí mismo (403)", "DELETE", "/api/v1/usuarios/me", "tokenHospital", None,
        ['pm.test("Responde 403", () => pm.response.to.have.status(403));']),
    req("El donador elimina su cuenta", "DELETE", "/api/v1/usuarios/me", "tokenBorrar", None,
        ['pm.test("Responde 204", () => pm.response.to.have.status(204));']),
    req("Ya no puede iniciar sesión (401)", "POST", "/api/v1/auth/login", None,
        {"email": "{{emailBorrar}}", "password": "contrasena-segura"},
        ['pm.test("Responde 401", () => pm.response.to.have.status(401));']),
]
carpeta(c, "8. Cuenta", "Derecho de cancelación: el donador borra su cuenta y sus datos.", items)
guardar(c)
print(len(items), "requests en 8. Cuenta")
