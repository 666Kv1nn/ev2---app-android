<?php
// GET -> datos del usuario conectado
include 'funciones.php';

$u = validarToken($c);
responder(200, ['usuario' => usuarioJson($u)]);
