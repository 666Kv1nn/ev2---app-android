<?php
// Conexion a MySQL en AWS RDS usando SSL/TLS
include 'config.php';

mysqli_report(MYSQLI_REPORT_OFF);
header('Content-Type: application/json; charset=utf-8');

$c = mysqli_init();
mysqli_options($c, MYSQLI_OPT_CONNECT_TIMEOUT, 5);
mysqli_ssl_set($c, null, null, $config['db_ssl_ca'], null, null);
mysqli_options($c, MYSQLI_OPT_SSL_VERIFY_SERVER_CERT, true);

$ok = mysqli_real_connect(
    $c,
    $config['db_host'],
    $config['db_usuario'],
    $config['db_password'],
    $config['db_nombre'],
    $config['db_puerto'],
    null,
    MYSQLI_CLIENT_SSL
);

if (!$ok) {
    error_log('Error de conexion a RDS: ' . mysqli_connect_error());
    http_response_code(500);
    echo json_encode(['error' => 'No se pudo conectar a la base de datos']);
    exit;
}

mysqli_set_charset($c, 'utf8mb4');
// todas las fechas se guardan en UTC
mysqli_query($c, "SET time_zone = '+00:00'");
