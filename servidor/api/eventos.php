<?php
// GET ?limite=60 -> bitacora de actividad (lo mas nuevo primero)
include 'funciones.php';

validarToken($c);

$limite = (int)($_GET['limite'] ?? 50);
if ($limite < 1 || $limite > 200) {
    $limite = 50;
}

$q = mysqli_prepare($c, "SELECT e.tipo, e.detalle, e.fecha, CONCAT(u.nombre, ' ', u.apellido) AS usuario
                         FROM eventos e LEFT JOIN usuarios u ON u.id = e.usuario_id
                         ORDER BY e.id DESC LIMIT ?");
mysqli_stmt_bind_param($q, 'i', $limite);
mysqli_stmt_execute($q);
$resultado = mysqli_stmt_get_result($q);

$eventos = [];
while ($fila = mysqli_fetch_assoc($resultado)) {
    $eventos[] = [
        'tipo'    => $fila['tipo'],
        'detalle' => $fila['detalle'],
        'fecha'   => fechaIso($fila['fecha']),
        'usuario' => $fila['usuario'],
    ];
}

responder(200, ['eventos' => $eventos]);
