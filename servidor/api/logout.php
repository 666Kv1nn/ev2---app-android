<?php
// POST -> borra el token actual
include 'funciones.php';

$u = validarToken($c);

$hash = hash('sha256', obtenerToken());
$q = mysqli_prepare($c, "DELETE FROM sesiones WHERE token_hash = ?");
mysqli_stmt_bind_param($q, 's', $hash);
mysqli_stmt_execute($q);

registrarEvento($c, $u['id'], null, 'sesion', 'Cerró sesión');

responder(200, ['ok' => true]);
