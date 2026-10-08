<?php
// GET -> lista de dispositivos con su ultimo valor/estado y el estado de la Raspberry Pi
include 'funciones.php';

validarToken($c);

$resultado = mysqli_query($c, $SQL_DISPOSITIVOS . " ORDER BY d.tipo = 'actuador', d.id");
$dispositivos = [];
while ($fila = mysqli_fetch_assoc($resultado)) {
    $dispositivos[] = dispositivoJson($fila);
}

// la Pi esta en linea si algun dispositivo reporto hace menos de 30 s
$fila = mysqli_fetch_assoc(mysqli_query($c, "SELECT MAX(ultima_conexion) AS ultima,
    MAX(ultima_conexion) > UTC_TIMESTAMP() - INTERVAL 30 SECOND AS en_linea FROM dispositivos"));

responder(200, [
    'gateway' => [
        'en_linea'        => $fila['en_linea'] == 1,
        'ultima_conexion' => fechaIso($fila['ultima']),
    ],
    'dispositivos' => $dispositivos,
]);
