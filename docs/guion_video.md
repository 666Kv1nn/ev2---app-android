# Guion del video: Monitor IoT (máx. 10 min, objetivo ~9 min)

Basado en los criterios de la guía (2.1.1 a 2.1.4) y en lo que pide el Paso 4:
autenticación y usuarios, conexión con los dispositivos IoT, almacenamiento y consulta de datos, y seguridad.

## Antes de grabar

- Raspberry Pi encendida, con el agente corriendo y la temperatura sobre 28 °C (ventilador apagado unos 5 min).
- Dos celulares en el mismo Wi-Fi que la Pi: **Celular A** (admin) y **Celular B** (operador).
- Cuentas creadas: admin, un operador, un lector y una cuenta de prueba solo para el bloqueo.
- En el computador, dos terminales con SSH a la Pi:
  - Terminal 1: `journalctl -u monitor-iot-agente -f`
  - Terminal 2: conectado a MySQL de RDS
- GitHub abierto en la carpeta del proyecto.
- Grabar la pantalla de los celulares (scrcpy o grabador del celular) y el computador.

Comando para la terminal 2:

```bash
mysql --ssl-ca=/etc/ssl/rds/global-bundle.pem -h database-1.cujwinzrlr3q.us-east-1.rds.amazonaws.com -u RSPPI -p iot_app
```

---

## 1. Introducción · 0:00 – 0:40

**Se muestra:** pantalla de login en el Celular A.

> "Hola, soy [nombre]. Les presento Monitor IoT, una app Android para monitorear y controlar los dispositivos de una sala de equipos. Una Raspberry Pi lee temperatura, humedad y luz, y controla un ventilador, una luz y una alarma. Desde la app se pueden ver las lecturas y prender o apagar los actuadores desde varios celulares al mismo tiempo."

## 2. Herramientas y arquitectura (criterio 2.1.1) · 0:40 – 1:50

**Se muestra:** el diagrama de arquitectura del documento de diseño, o el README.

> "La app está hecha en Android Studio con Kotlin, siguiendo los componentes de Material Design. Uso Volley para las peticiones HTTP, MPAndroidChart para los gráficos y EncryptedSharedPreferences para guardar la sesión.
>
> La app no se conecta directo a la base de datos. Le habla por Wi-Fi y HTTPS a una API en PHP que corre en la Raspberry Pi. La Pi guarda todo en MySQL en AWS RDS.
>
> Elegí Wi-Fi y no Bluetooth porque tiene más alcance, permite varios celulares a la vez y la Pi está enchufada, así que el consumo no es problema. Para el login elegí la opción de API con MySQL, porque las cuentas quedan centralizadas, el administrador puede quitar accesos y la contraseña de la base nunca queda dentro del APK."

## 3. Autenticación y gestión de usuarios · 1:50 – 3:30

**Celular A:**
1. Escribir una contraseña incorrecta → aparece "Usuario o contraseña incorrectos".
   > "Si me equivoco, el mensaje no dice si falló el usuario o la contraseña, para no dar pistas."
2. (Cuenta de prueba, se puede cortar en edición) 5 intentos fallidos → "Cuenta bloqueada por demasiados intentos".
   > "A los 5 intentos fallidos la cuenta se bloquea 15 minutos, para evitar ataques de fuerza bruta."
3. Ingresar con el admin → Panel.
   > "Al ingresar, el servidor me entrega un token de sesión que dura 12 horas. La contraseña no se guarda en el celular."
4. Menú → Usuarios → botón + → crear un usuario con rol Lector.
   > "No hay registro abierto: las cuentas las crea el administrador. Hay tres roles: administrador, operador y lector. El formulario valida el correo y que la contraseña tenga 8 caracteres con letras y números, y el servidor lo vuelve a validar."

## 4. Conexión con los dispositivos IoT y dos celulares (criterios 2.1.2 y 2.1.4) · 3:30 – 5:30

**Se muestra:** Celular A (admin) y Celular B (operador) lado a lado, más la terminal 1.

1. Mostrar el panel: estado "Raspberry Pi en línea", sensores y actuadores.
   > "El panel muestra si la Raspberry Pi está en línea y el último valor de cada sensor. Se actualiza solo cada 3 segundos."
2. En el **Celular A** encender el **Ventilador**.
   > "Enciendo el ventilador desde este celular..."
3. Mostrar la **terminal 1**: aparece `VENT-01 ENCENDIDO` (y el LED o relé si hay hardware).
   > "...la Raspberry Pi recibe la orden en menos de un segundo y activa el pin GPIO 17."
4. Mostrar el **Celular B**: el interruptor cambió solo.
   > "Y en el otro celular el cambio aparece en unos 3 segundos, sin tocar nada. Así los dos celulares se comunican a través de la Raspberry Pi por Wi-Fi."
5. En el Celular B abrir el detalle del Ventilador → "Último cambio: [nombre del admin]".
   > "Además queda registrado quién hizo el cambio."

## 5. Almacenamiento y consulta de datos · 5:30 – 7:00

1. Abrir **Temperatura** → gráfico de 1 hora.
   > "Cada 10 segundos la Pi manda una lectura a la API, que la guarda en la tabla lecturas de MySQL. Aquí se ve el historial con las líneas rojas del rango permitido. Como encendimos el ventilador, la temperatura empieza a bajar."
2. Cambiar a 24 horas y mostrar el resumen (mínimo, promedio, máximo).
3. Volver al panel y mostrar el aviso rojo de "Fuera de rango" (si sigue sobre 28 °C) → Menú → **Actividad**.
   > "Cuando un sensor sale del rango se marca en rojo y se registra una alerta en la bitácora, junto con los inicios de sesión y quién prendió o apagó cada cosa."
4. **Terminal 2:**
   ```sql
   SELECT dispositivo_id, valor, registrado_en FROM lecturas ORDER BY id DESC LIMIT 5;
   ```
   > "Estos son los mismos datos guardados en AWS RDS."

## 6. Seguridad ISO/IEC 27400 (criterio 2.1.3) · 7:00 – 8:40

1. **Terminal 2:**
   ```sql
   SELECT usuario, password_hash, rol FROM usuarios;
   ```
   > "Las contraseñas no se guardan en texto plano, sino con hash bcrypt."
   ```sql
   SELECT usuario_id, token_hash, expira_en FROM sesiones;
   ```
   > "De los tokens también se guarda solo el hash, así que aunque alguien vea la tabla no puede usarlos."
2. **Cifrado y conexión segura:** mostrar en la terminal que la API rechaza HTTP y pide sesión por HTTPS (comandos abajo).
   > "La API solo responde por HTTPS. Por HTTP da 403, y sin token da 401. La app además solo acepta el certificado de la Raspberry Pi, configurado en network_security_config, y la conexión de la Pi a RDS también va cifrada con SSL."
3. **Control de acceso:** entrar con la cuenta **Lector** → los interruptores están deshabilitados y dice "Solo lectura".
   > "Los permisos se revisan en el servidor: si un lector intentara controlar algo, la API responde 403."
4. **Revocar acceso:** desde el admin desactivar al operador → el Celular B vuelve al login con "La sesión expiró".
   > "Si el administrador desactiva una cuenta, se cierran sus sesiones en todos los celulares."

## 7. Código y cierre · 8:40 – 9:30

**Se muestra:** el repositorio en GitHub.

> "El código está en GitHub. En la carpeta app está la aplicación, con una Activity por pantalla. En servidor están la API en PHP, el script SQL, el agente de la Raspberry Pi y el script de instalación. Los archivos con contraseñas están en .gitignore y no se suben.
>
> En resumen, la app permite iniciar sesión de forma segura, monitorear y controlar los dispositivos IoT desde varios celulares por Wi-Fi y guardar el historial en AWS. Gracias."
