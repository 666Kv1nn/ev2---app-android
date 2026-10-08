#!/usr/bin/env python3
# Agente IoT de la Raspberry Pi
#
# Cada 1 segundo:
#   - pregunta a la API (agente.php) como deben estar los actuadores y los prende/apaga
# Cada 10 segundos ademas:
#   - lee los sensores y manda las lecturas
#
# Si no hay sensor DHT22 conectado los valores se simulan. En la simulacion
# el ventilador baja la temperatura y la luz sube los lux, asi en la demo
# se ve el efecto de controlar algo desde la app.
#
# Uso: python3 agente_iot.py agente.conf

import configparser
import json
import math
import random
import ssl
import sys
import time
import urllib.request

# ---------- configuracion ----------
archivo = sys.argv[1] if len(sys.argv) > 1 else "agente.conf"
conf = configparser.ConfigParser()
if not conf.read(archivo, encoding="utf-8"):
    print("No se encontro", archivo)
    sys.exit(1)

URL = conf["servidor"]["url"]
CLAVE = conf["servidor"]["clave"]
CERTIFICADO = conf["servidor"]["certificado"]
PINES = {
    "VENT-01": conf["gpio"].get("VENT-01"),
    "LED-01": conf["gpio"].get("LED-01"),
    "ALM-01": conf["gpio"].get("ALM-01"),
}
PIN_DHT = conf["gpio"].get("DHT")

# conexion HTTPS que solo acepta el certificado de la Pi
contexto_ssl = ssl.create_default_context(cafile=CERTIFICADO)

# ---------- GPIO (si se puede) ----------
salidas = {}
try:
    from gpiozero import OutputDevice
    for codigo, pin in PINES.items():
        if pin:
            salidas[codigo] = OutputDevice(int(pin), initial_value=False)
    print("GPIO listo:", PINES)
except Exception as e:
    print("Sin GPIO, los actuadores se simulan:", e)

# ---------- sensor DHT22 (si se puede) ----------
dht = None
if PIN_DHT:
    try:
        import adafruit_dht
        import board
        dht = adafruit_dht.DHT22(getattr(board, "D" + PIN_DHT))
        print("DHT22 en GPIO", PIN_DHT)
    except Exception as e:
        print("Sin DHT22, se simulan los valores:", e)

# estado actual de los actuadores
estado = {"VENT-01": False, "LED-01": False, "ALM-01": False}

# valores simulados
temperatura = 24.0
humedad = 50.0


def leer_sensores():
    global temperatura, humedad

    # con el ventilador encendido la temperatura baja hacia 21, apagado sube hacia 27
    if estado["VENT-01"]:
        temperatura += (21 - temperatura) * 0.04
        humedad += (45 - humedad) * 0.03
    else:
        temperatura += (27 - temperatura) * 0.04
        humedad += (58 - humedad) * 0.03
    temperatura += random.uniform(-0.15, 0.15)
    humedad += random.uniform(-0.4, 0.4)

    temp = temperatura
    hum = humedad
    if dht is not None:
        try:
            temp = dht.temperature
            hum = dht.humidity
        except RuntimeError:
            pass  # el DHT22 a veces falla una lectura

    luz = 180 + 120 * math.sin(time.time() / 600) + random.uniform(-8, 8)
    if estado["LED-01"]:
        luz += 420

    return {
        "TEMP-01": round(temp, 2),
        "HUM-01": round(hum, 2),
        "LUZ-01": round(max(0, luz), 1),
    }


def enviar(lecturas):
    datos = json.dumps({"lecturas": lecturas}).encode()
    peticion = urllib.request.Request(URL, data=datos, method="POST")
    peticion.add_header("Content-Type", "application/json")
    peticion.add_header("X-Device-Key", CLAVE)
    respuesta = urllib.request.urlopen(peticion, context=contexto_ssl, timeout=5)
    return json.load(respuesta)["actuadores"]


def aplicar(deseado):
    for codigo in deseado:
        encendido = deseado[codigo]
        if estado.get(codigo) == encendido:
            continue
        estado[codigo] = encendido
        if codigo in salidas:
            if encendido:
                salidas[codigo].on()
            else:
                salidas[codigo].off()
        print(time.strftime("%H:%M:%S"), codigo, "ENCENDIDO" if encendido else "APAGADO")


print("Agente iniciado ->", URL)
ultima_lectura = 0

while True:
    lecturas = {}
    if time.time() - ultima_lectura >= 10:
        lecturas = leer_sensores()
        ultima_lectura = time.time()

    try:
        aplicar(enviar(lecturas))
        if lecturas:
            print(time.strftime("%H:%M:%S"), "lecturas", lecturas)
    except Exception as e:
        print("Error al conectar con la API:", e)

    time.sleep(1)
