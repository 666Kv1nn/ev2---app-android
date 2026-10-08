<?php
// GET ?dispositivo_id=1&horas=24 -> historial de un sensor para el grafico
include 'funciones.php';

validarToken($c);

$id = (int)($_GET['dispositivo_id'] ?? 0);
$horas = (int)($_GET['horas'] ?? 24);
if ($horas != 1 && $horas != 24 && $horas != 168) {
    $horas = 24;
}

$q = mysqli_prepare($c, $SQL_DISPOSITIVOS . " WHERE d.id = ? AND d.tipo = 'sensor'");
mysqli_stmt_bind_param($q, 'i', $id);
mysqli_stmt_execute($q);
$sensor = mysqli_fetch_assoc(mysqli_stmt_get_result($q));
if (!$sensor) {
    responderError(404, 'Sensor no encontrado');
}

// Se agrupan las lecturas para que el grafico tenga maximo 120 puntos
// (ej: 24 h -> un promedio cada 12 minutos)
$segundos = intdiv($horas * 3600, 120);
$q = mysqli_prepare($c, "SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(registrado_en) / ?) * ?) AS t, AVG(valor) AS v
                         FROM lecturas
                         WHERE dispositivo_id = ? AND registrado_en > UTC_TIMESTAMP() - INTERVAL ? HOUR
                         GROUP BY t ORDER BY t");
mysqli_stmt_bind_param($q, 'iiii', $segundos, $segundos, $id, $horas);
mysqli_stmt_execute($q);
$resultado = mysqli_stmt_get_result($q);

$puntos = [];
while ($fila = mysqli_fetch_assoc($resultado)) {
    $puntos[] = ['t' => fechaIso($fila['t']), 'v' => round($fila['v'], 2)];
}

// minimo, maximo y promedio del periodo
$q = mysqli_prepare($c, "SELECT MIN(valor) AS min, MAX(valor) AS max, AVG(valor) AS prom, COUNT(*) AS total
                         FROM lecturas WHERE dispositivo_id = ? AND registrado_en > UTC_TIMESTAMP() - INTERVAL ? HOUR");
mysqli_stmt_bind_param($q, 'ii', $id, $horas);
mysqli_stmt_execute($q);
$r = mysqli_fetch_assoc(mysqli_stmt_get_result($q));

responder(200, [
    'dispositivo' => dispositivoJson($sensor),
    'horas'       => $horas,
    'puntos'      => $puntos,
    'resumen'     => [
        'min'   => $r['min'] === null ? null : (float)$r['min'],
        'max'   => $r['max'] === null ? null : (float)$r['max'],
        'prom'  => $r['prom'] === null ? null : round($r['prom'], 2),
        'total' => (int)$r['total'],
    ],
]);
