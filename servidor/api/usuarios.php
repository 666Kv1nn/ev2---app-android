<?php
// Gestion de usuarios (solo admin)
//   GET                                -> lista de usuarios
//   POST {nombre, apellido, ...}       -> crea un usuario
//   POST {accion: "estado", id, activo} -> activa o desactiva un usuario
include 'funciones.php';

$admin = validarToken($c, ['admin']);

// ---------- Listar ----------
if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $resultado = mysqli_query($c, "SELECT * FROM usuarios ORDER BY activo DESC, nombre");
    $usuarios = [];
    while ($fila = mysqli_fetch_assoc($resultado)) {
        $usuarios[] = usuarioJson($fila);
    }
    responder(200, ['usuarios' => $usuarios]);
}

$datos = leerJson();

// ---------- Activar / desactivar ----------
if (($datos['accion'] ?? '') == 'estado') {
    $id = (int)($datos['id'] ?? 0);
    $activo = !empty($datos['activo']) ? 1 : 0;

    if ($id == $admin['id']) {
        responderError(400, 'No puedes desactivar tu propia cuenta');
    }

    $q = mysqli_prepare($c, "UPDATE usuarios SET activo = ? WHERE id = ?");
    mysqli_stmt_bind_param($q, 'ii', $activo, $id);
    mysqli_stmt_execute($q);

    if ($activo == 0) {
        // se cierran sus sesiones en todos los telefonos
        $q = mysqli_prepare($c, "DELETE FROM sesiones WHERE usuario_id = ?");
        mysqli_stmt_bind_param($q, 'i', $id);
        mysqli_stmt_execute($q);
    }

    registrarEvento($c, $admin['id'], null, 'usuarios', ($activo ? 'Activó' : 'Desactivó') . ' al usuario con id ' . $id);
    responder(200, ['ok' => true]);
}

// ---------- Crear usuario ----------
$usuario = strtolower(trim($datos['usuario'] ?? ''));
$password = $datos['password'] ?? '';
$nombre = trim($datos['nombre'] ?? '');
$apellido = trim($datos['apellido'] ?? '');
$correo = strtolower(trim($datos['correo'] ?? ''));
$telefono = trim($datos['telefono'] ?? '');
$rol = $datos['rol'] ?? '';

// validaciones (las mismas que hace la app)
if (!preg_match('/^[a-z0-9._-]{3,30}$/', $usuario)) {
    responderError(422, 'Usuario no válido (3 a 30 caracteres: letras, números, punto o guion)');
}
if (strlen($password) < 8 || !preg_match('/[A-Za-z]/', $password) || !preg_match('/[0-9]/', $password)) {
    responderError(422, 'La contraseña debe tener mínimo 8 caracteres, con letras y números');
}
if ($nombre == '' || $apellido == '') {
    responderError(422, 'Falta el nombre o el apellido');
}
if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) {
    responderError(422, 'Correo no válido');
}
if (!in_array($rol, ['admin', 'operador', 'lector'])) {
    responderError(422, 'Rol no válido');
}

// que no se repita el usuario ni el correo
$q = mysqli_prepare($c, "SELECT id FROM usuarios WHERE usuario = ? OR correo = ?");
mysqli_stmt_bind_param($q, 'ss', $usuario, $correo);
mysqli_stmt_execute($q);
if (mysqli_fetch_assoc(mysqli_stmt_get_result($q))) {
    responderError(409, 'Ese usuario o correo ya está registrado');
}

// la contraseña se guarda con hash bcrypt, nunca en texto plano
$hash = password_hash($password, PASSWORD_DEFAULT);
$q = mysqli_prepare($c, "INSERT INTO usuarios (usuario, password_hash, nombre, apellido, correo, telefono, rol) VALUES (?, ?, ?, ?, ?, ?, ?)");
mysqli_stmt_bind_param($q, 'sssssss', $usuario, $hash, $nombre, $apellido, $correo, $telefono, $rol);

if (!mysqli_stmt_execute($q)) {
    responderError(500, 'No se pudo crear el usuario');
}

registrarEvento($c, $admin['id'], null, 'usuarios', 'Creó al usuario ' . $usuario . ' (' . $rol . ')');
responder(201, ['ok' => true, 'id' => mysqli_insert_id($c)]);
