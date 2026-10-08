<?php
// Copiar este archivo como config.php y poner los datos reales.
$config = [
    'db_host'     => 'TU-INSTANCIA.xxxxxxxx.us-east-1.rds.amazonaws.com',
    'db_puerto'   => 3306,
    'db_usuario'  => 'USUARIO',
    'db_password' => 'CONTRASEÑA',
    'db_nombre'   => 'iot_app',
    'db_ssl_ca'   => '/etc/ssl/rds/global-bundle.pem',

    // generar con: python3 -c "import secrets; print(secrets.token_hex(32))"
    'clave_dispositivo' => 'CAMBIAR',

    'horas_sesion'    => 12,
    'max_intentos'    => 5,
    'minutos_bloqueo' => 15,
];
