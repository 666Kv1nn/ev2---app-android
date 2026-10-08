# Monitor IoT

Aplicación Android para monitorear y controlar dispositivos IoT conectados a una Raspberry Pi.
Proyecto de la asignatura TI3042 (Aplicaciones Móviles para IoT), Unidad 2.

- **App:** Kotlin, layouts XML, Volley y MPAndroidChart
- **Servidor:** Raspberry Pi con Apache + PHP (API) y un agente en Python que maneja los GPIO
- **Base de datos:** MySQL en AWS RDS
- **Comunicación:** Wi-Fi con HTTPS

```
Celulares Android  --(Wi-Fi, HTTPS)-->  Raspberry Pi (API PHP + agente)  --(SSL)-->  AWS RDS MySQL
                                               |
                                        sensores y actuadores (GPIO)
```

## Funciones

- Login con usuario y contraseña (las cuentas las crea el administrador)
- Panel con los sensores (temperatura, humedad, luz) y actuadores (ventilador, luz, alarma)
- Encender y apagar actuadores; los cambios se ven en los otros celulares en unos 3 segundos
- Gráfico del historial de cada sensor (1 hora, 24 horas, 7 días) y alertas si un valor sale del rango
- Bitácora de actividad (quién hizo qué)
- Gestión de usuarios con roles: administrador, operador y lector

## Seguridad (ISO 27400)

- Contraseñas guardadas con hash bcrypt
- Token de sesión aleatorio; en la base solo se guarda su hash
- Token guardado cifrado en el celular (EncryptedSharedPreferences)
- Todo por HTTPS; la app solo acepta el certificado de la Raspberry Pi
- Conexión con SSL entre la Raspberry Pi y AWS RDS
- Permisos por rol revisados en el servidor
- Bloqueo de la cuenta después de 5 intentos fallidos (15 minutos)
- Consultas preparadas (evitan inyección SQL)

## Cómo instalar

### 1. AWS RDS
En la instancia `database-1`, dejar **Publicly accessible = Yes** y en el *Security group*
permitir el puerto 3306 desde la IP pública de la red donde está la Raspberry Pi.

### 2. Raspberry Pi
Copiar la carpeta `servidor/` a la Pi y ejecutar:

```bash
bash instalar.sh
```

Después crear el primer administrador:

```bash
sudo php /var/www/html/iot/crear_usuario.php admin admin Nombre Apellido correo@ejemplo.com
```

### 3. App
Abrir el proyecto en Android Studio y ejecutarlo en dos celulares conectados al mismo Wi-Fi que la Pi.

### Si la Raspberry Pi tiene otra IP (no 10.16.1.28)
1. Ejecutar `bash servidor/ssl/generar_certificado.sh <IP>`
2. Copiar `servidor/ssl/servidor_iot.crt` a `app/src/main/res/raw/`
3. Cambiar la IP en `Conexion.kt` y en `res/xml/network_security_config.xml`
4. Volver a ejecutar `instalar.sh` en la Pi

## Archivos que no se suben a GitHub

Tienen datos privados, por eso están en `.gitignore`:

- `servidor/api/config.php` (datos de RDS). Hay un `config.example.php` de ejemplo
- `servidor/agente/agente.conf` (clave del agente). Hay un `agente.conf.example`
- `servidor/ssl/servidor_iot.key` (clave privada del certificado)

## API

| Archivo | Método | Qué hace |
|---|---|---|
| login.php | POST | Inicia sesión y devuelve el token |
| logout.php | POST | Cierra la sesión |
| perfil.php | GET | Datos del usuario conectado |
| dispositivos.php | GET | Lista de dispositivos y estado de la Pi |
| lecturas.php | GET | Historial de un sensor |
| control.php | POST | Enciende o apaga un actuador (admin y operador) |
| eventos.php | GET | Bitácora |
| usuarios.php | GET/POST | Listar, crear y activar/desactivar usuarios (solo admin) |
| agente.php | POST | Lo usa la Raspberry Pi para mandar lecturas |
