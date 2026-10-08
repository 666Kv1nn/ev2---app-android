<?php
// Funciones que usan todos los archivos de la API
include 'conexion.php';

// responde en JSON y termina
function responder($codigo, $datos)
{
    http_response_code($codigo);
    echo json_encode($datos, JSON_UNESCAPED_UNICODE);
    exit;
}

function responderError($codigo, $mensaje)
{
    responder($codigo, ['error' => $mensaje]);
}

// la app manda los datos como JSON en el cuerpo del POST
function leerJson()
{
    $datos = json_decode(file_get_contents('php://input'), true);
    if (!is_array($datos)) {
        return [];
    }
    return $datos;
}

// "2026-10-06 14:32:05" -> "2026-10-06T14:32:05Z"
function fechaIso($fecha)
{
    if ($fecha == null) {
        return null;
    }
    return str_replace(' ', 'T', $fecha) . 'Z';
}

// saca el token de la cabecera "Authorization: Bearer ..."
function obtenerToken()
{
    $cabecera = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if ($cabecera == '' && function_exists('apache_request_headers')) {
        $cabeceras = apache_request_headers();
        $cabecera = $cabeceras['Authorization'] ?? ($cabeceras['authorization'] ?? '');
    }
    if (strpos($cabecera, 'Bearer ') !== 0) {
        return '';
    }
    return trim(substr($cabecera, 7));
}

// Revisa el token y devuelve el usuario.
// Si no es valido corta con 401. Si el rol no esta permitido corta con 403.
function validarToken($c, $rolesPermitidos = [])
{
    $token = obtenerToken();
    if ($token == '') {
        responderError(401, 'Sesión no iniciada');
    }

    // en la base se guarda el hash del token, no el token
    $hash = hash('sha256', $token);
    $q = mysqli_prepare($c, "SELECT u.* FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
                             WHERE s.token_hash = ? AND s.expira_en > UTC_TIMESTAMP() AND u.activo = 1");
    mysqli_stmt_bind_param($q, 's', $hash);
    mysqli_stmt_execute($q);
    $usuario = mysqli_fetch_assoc(mysqli_stmt_get_result($q));

    if (!$usuario) {
        responderError(401, 'La sesión expiró, vuelve a iniciar sesión');
    }
    if (count($rolesPermitidos) > 0 && !in_array($usuario['rol'], $rolesPermitidos)) {
        responderError(403, 'Tu rol no tiene permiso para esto');
    }
    return $usuario;
}

// lo que se manda a la app de cada usuario (sin el hash de la contraseña)
function usuarioJson($u)
{
    return [
        'id'            => (int)$u['id'],
        'usuario'       => $u['usuario'],
        'nombre'        => $u['nombre'],
        'apellido'      => $u['apellido'],
        'correo'        => $u['correo'],
        'telefono'      => $u['telefono'],
        'rol'           => $u['rol'],
        'activo'        => $u['activo'] == 1,
        'ultimo_acceso' => fechaIso($u['ultimo_acceso']),
    ];
}

// Consulta de dispositivos. Un dispositivo esta "en linea" si la Pi lo reporto hace menos de 30 s
$SQL_DISPOSITIVOS = "SELECT d.*,
        (d.ultima_conexion > UTC_TIMESTAMP() - INTERVAL 30 SECOND) AS en_linea,
        CONCAT(u.nombre, ' ', u.apellido) AS nombre_usuario
    FROM dispositivos d
    LEFT JOIN usuarios u ON u.id = d.actualizado_por";

function dispositivoJson($d)
{
    $valor = $d['ultimo_valor'] === null ? null : (float)$d['ultimo_valor'];
    $min = $d['umbral_min'] === null ? null : (float)$d['umbral_min'];
    $max = $d['umbral_max'] === null ? null : (float)$d['umbral_max'];

    // alerta si el valor esta fuera del rango permitido
    $alerta = false;
    if ($valor !== null) {
        if ($min !== null && $valor < $min) $alerta = true;
        if ($max !== null && $valor > $max) $alerta = true;
    }

    return [
        'id'              => (int)$d['id'],
        'codigo'          => $d['codigo'],
        'nombre'          => $d['nombre'],
        'tipo'            => $d['tipo'],
        'magnitud'        => $d['magnitud'],
        'unidad'          => $d['unidad'],
        'ubicacion'       => $d['ubicacion'],
        'umbral_min'      => $min,
        'umbral_max'      => $max,
        'estado'          => $d['estado'] == 1,
        'valor'           => $valor,
        'alerta'          => $alerta,
        'en_linea'        => $d['en_linea'] == 1,
        'ultima_conexion' => fechaIso($d['ultima_conexion']),
        'actualizado_por' => $d['nombre_usuario'],
        'actualizado_en'  => fechaIso($d['actualizado_en']),
    ];
}

// guarda en la bitacora quien hizo que
function registrarEvento($c, $usuarioId, $dispositivoId, $tipo, $detalle)
{
    $ip = $_SERVER['REMOTE_ADDR'] ?? '';
    $q = mysqli_prepare($c, "INSERT INTO eventos (usuario_id, dispositivo_id, tipo, detalle, ip) VALUES (?, ?, ?, ?, ?)");
    mysqli_stmt_bind_param($q, 'iisss', $usuarioId, $dispositivoId, $tipo, $detalle, $ip);
    mysqli_stmt_execute($q);
}
