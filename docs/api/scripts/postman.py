"""Ayudante para agregar requests a la colección de Postman sin editar el JSON a mano.

Uso desde otro script:  from postman import req, carpeta, guardar, cargar
"""
import json
import os

RUTA = os.path.join(os.path.dirname(__file__), "..", "GotaDeVida.postman_collection.json")


def cargar():
    with open(RUTA, encoding="utf-8") as f:
        return json.load(f)


def guardar(coleccion):
    with open(RUTA, "w", encoding="utf-8") as f:
        json.dump(coleccion, f, ensure_ascii=False, indent=2)
        f.write("\n")


def req(nombre, metodo, ruta, token=None, cuerpo=None, pruebas=(), antes=(), descripcion=None):
    """ruta sin {{baseUrl}}, por ejemplo "/api/v1/solicitudes"; token es el nombre de la variable."""
    sin_query, _, query = ruta.partition("?")
    url = {
        "raw": "{{baseUrl}}" + ruta,
        "host": ["{{baseUrl}}"],
        "path": [p for p in sin_query.split("/") if p],
    }
    if query:
        url["query"] = [{"key": k, "value": v} for k, v in (q.split("=", 1) for q in query.split("&"))]
    r = {
        "method": metodo,
        "header": [{"key": "Content-Type", "value": "application/json"}] if cuerpo is not None else [],
        "url": url,
        "auth": {"type": "bearer", "bearer": [{"key": "token", "value": "{{%s}}" % token, "type": "string"}]}
        if token else {"type": "noauth"},
    }
    if cuerpo is not None:
        r["body"] = {"mode": "raw", "raw": json.dumps(cuerpo, ensure_ascii=False, indent=2), "options": {"raw": {"language": "json"}}}
    if descripcion:
        r["description"] = descripcion
    item = {"name": nombre, "request": r, "event": []}
    if antes:
        item["event"].append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": list(antes)}})
    if pruebas:
        item["event"].append({"listen": "test", "script": {"type": "text/javascript", "exec": list(pruebas)}})
    return item


def carpeta(coleccion, nombre, descripcion, items):
    """Reemplaza (o agrega) la carpeta con ese nombre."""
    coleccion["item"] = [i for i in coleccion["item"] if i["name"] != nombre]
    coleccion["item"].append({"name": nombre, "description": descripcion, "item": items})


def variables(coleccion, *nombres):
    existentes = {v["key"] for v in coleccion.get("variable", [])}
    for n in nombres:
        if n not in existentes:
            coleccion.setdefault("variable", []).append({"key": n, "value": ""})


# Espera para dar tiempo a los consumidores de RabbitMQ (procesan en segundo plano).
ESPERAR = ["setTimeout(() => {}, 1500);"]
