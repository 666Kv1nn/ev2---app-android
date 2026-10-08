<?php
// POST {dispositivo_id, estado} -> enciende o apaga un actuador (solo admin y operador)
// La Raspberry Pi lo aplica en su siguiente consulta (1 segundo aprox.)
include 'funciones.php';

$u = validarToken($c, ['admin', 'operador']);
$datos = leerJson();

$id = (int)($datos['dispositivo_id'] ?? 0);
if (!isset($datos['estado'])) {
    responderError(400, 'Falta el estado');
}
$estado = $datos['estado'] ? 1 : 0;

$q = mysqli_prepare($c, "SELECT * FROM dispositivos WHERE id = ? AND tipo = 'actuador'");
mysqli_stmt_bind_param($q, 'i', $id);
mysqli_stmt_execute($q);
$d = mysqli_fetch_assoc(mysqli_stmt_get_result($q));
if (!$d) {
    responderError(404, 'Actuador no encontrado');
}

$q = mysqli_prepare($c, "UPDATE dispositivos SET estado = ?, actualizado_por = ?, actualizado_en = UTC_TIMESTAMP() WHERE id = ?");
mysqli_stmt_bind_param($q, 'iii', $estado, $u['id'], $id);
mysqli_stmt_execute($q);

$accion = $estado == 1 ? 'Encendió ' : 'Apagó ';
registrarEvento($c, $u['id'], $id, 'control', $accion . $d['nombre']);

// se devuelve el dispositivo actualizado
$q = mysqli_prepare($c, $SQL_DISPOSITIVOS . " WHERE d.id = ?");
mysqli_stmt_bind_param($q, 'i', $id);
mysqli_stmt_execute($q);
responder(200, ['dispositivo' => dispositivoJson(mysqli_fetch_assoc(mysqli_stmt_get_result($q)))]);
