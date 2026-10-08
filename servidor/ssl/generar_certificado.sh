#!/bin/bash
# Genera el certificado TLS autofirmado de la Raspberry Pi para HTTPS.
#
# La app confía SOLO en este certificado para hablar con la Pi
# (app/src/main/res/raw/servidor_iot.crt + network_security_config.xml),
# así que si cambia la IP de la Pi hay que:
#   1. Ejecutar:  bash generar_certificado.sh <IP-de-la-Pi>
#   2. Copiar servidor_iot.crt a app/src/main/res/raw/servidor_iot.crt
#   3. Actualizar la IP en app/build.gradle.kts y network_security_config.xml
#   4. Volver a ejecutar instalar.sh en la Pi y recompilar la app.
set -e

IP="${1:-10.16.1.28}"
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# MSYS_NO_PATHCONV evita que Git Bash (Windows) convierta "/CN=..." en una ruta.
MSYS_NO_PATHCONV=1 openssl req -x509 -newkey rsa:2048 -sha256 -days 3650 -nodes \
  -keyout servidor_iot.key -out servidor_iot.crt \
  -subj "/CN=$IP/O=Monitor IoT" \
  -addext "subjectAltName=IP:$IP,IP:127.0.0.1,DNS:raspberrypi.local,DNS:localhost" \
  -addext "keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign" \
  -addext "extendedKeyUsage=serverAuth"

echo "Certificado generado para $IP:"
echo "  $DIR/servidor_iot.crt  (público: copiarlo a app/src/main/res/raw/)"
echo "  $DIR/servidor_iot.key  (privado: NO subir a GitHub)"
