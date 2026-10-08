<?php
// POST {usuario, password, cliente}  ->  {token, expira_en, usuario}
include 'funciones.php';

if ($_SERVER['REQUEST_METHOD'] != 'POST') {
    responderError(405, 'Método no permitido');
}

$datos = leerJson();
$usuario = strtolower(trim($datos['usuario'] ?? ''));
$password = $datos['password'] ?? '';
$cliente = substr(trim($datos['cliente'] ?? ''), 0, 100);
$ip = $_SERVER['REMOTE_ADDR'] ?? '';

if ($usuario == '' || $password == '') {
    responderError(400, 'Ingresa usuario y contraseña');
}

// 1. Revisar si la cuenta esta bloqueada por muchos intentos fallidos
$q = mysqli_prepare($c, "SELECT COUNT(*) AS fallos FROM intentos_login
                         WHERE usuario = ? AND exito = 0 AND fecha > UTC_TIMESTAMP() - INTERVAL ? MINUTE");
mysqli_stmt_bind_param($q, 'si', $usuario, $config['minutos_bloqueo']);
mysqli_stmt_execute($q);
$fallos = mysqli_fetch_assoc(mysqli_stmt_get_result($q))['fallos'];

if ($fallos >= $config['max_intentos']) {
    responderError(423, 'Cuenta bloqueada por demasiados intentos. Intenta en ' . $config['minutos_bloqueo'] . ' minutos.');
}

// 2. Buscar el usuario y comparar la contraseña con el hash (bcrypt)
$q = mysqli_prepare($c, "SELECT * FROM usuarios WHERE usuario = ?");
mysqli_stmt_bind_param($q, 's', $usuario);
mysqli_stmt_execute($q);
$u = mysqli_fetch_assoc(mysqli_stmt_get_result($q));

$correcto = $u && password_verify($password, $u['password_hash']);

// se guarda el intento (correcto o no)
$exito = $correcto ? 1 : 0;
$q = mysqli_prepare($c, "INSERT INTO intentos_login (usuario, ip, exito) VALUES (?, ?, ?)");
mysqli_stmt_bind_param($q, 'ssi', $usuario, $ip, $exito);
mysqli_stmt_execute($q);

if (!$correcto) {
    // mismo mensaje si falla el usuario o la contraseña, asi no se sabe cual existe
    responderError(401, 'Usuario o contraseña incorrectos');
}
if ($u['activo'] != 1) {
    responderError(403, 'Tu cuenta está desactivada. Habla con el administrador.');
}

// 3. Login correcto: se borran los intentos fallidos y se crea el token
$q = mysqli_prepare($c, "DELETE FROM intentos_login WHERE usuario = ? AND exito = 0");
mysqli_stmt_bind_param($q, 's', $usuario);
mysqli_stmt_execute($q);

$token = bin2hex(random_bytes(32)); // 64 caracteres aleatorios
$hash = hash('sha256', $token);     // en la base solo se guarda el hash
$q = mysqli_prepare($c, "INSERT INTO sesiones (usuario_id, token_hash, cliente, ip, expira_en)
                         VALUES (?, ?, ?, ?, UTC_TIMESTAMP() + INTERVAL ? HOUR)");
mysqli_stmt_bind_param($q, 'isssi', $u['id'], $hash, $cliente, $ip, $config['horas_sesion']);
mysqli_stmt_execute($q);

$q = mysqli_prepare($c, "UPDATE usuarios SET ultimo_acceso = UTC_TIMESTAMP() WHERE id = ?");
mysqli_stmt_bind_param($q, 'i', $u['id']);
mysqli_stmt_execute($q);

// limpieza de sesiones vencidas
mysqli_query($c, "DELETE FROM sesiones WHERE expira_en < UTC_TIMESTAMP()");

registrarEvento($c, $u['id'], null, 'sesion', 'Inició sesión desde ' . $cliente);

responder(200, [
    'token'   => $token,
    'usuario' => usuarioJson($u),
]);
