<?php
// Crea un usuario desde la terminal de la Raspberry Pi (para el primer admin).
// Uso:  php crear_usuario.php admin admin "Ana" "Rojas" ana@ejemplo.com
// La contraseña se pide por teclado.
if (php_sapi_name() != 'cli') {
    exit; // desde el navegador no hace nada
}

include 'conexion.php';

if (count($argv) < 6) {
    echo "Uso: php crear_usuario.php <usuario> <admin|operador|lector> <nombre> <apellido> <correo>\n";
    exit(1);
}
$usuario = strtolower($argv[1]);
$rol = $argv[2];
$nombre = $argv[3];
$apellido = $argv[4];
$correo = strtolower($argv[5]);

if (!in_array($rol, ['admin', 'operador', 'lector'])) {
    echo "Rol no valido\n";
    exit(1);
}

echo "Contraseña (minimo 8, letras y numeros): ";
system('stty -echo');
$password = trim(fgets(STDIN));
system('stty echo');
echo "\n";

if (strlen($password) < 8 || !preg_match('/[A-Za-z]/', $password) || !preg_match('/[0-9]/', $password)) {
    echo "La contraseña es muy debil\n";
    exit(1);
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$q = mysqli_prepare($c, "INSERT INTO usuarios (usuario, password_hash, nombre, apellido, correo, rol) VALUES (?, ?, ?, ?, ?, ?)");
mysqli_stmt_bind_param($q, 'ssssss', $usuario, $hash, $nombre, $apellido, $correo, $rol);

if (mysqli_stmt_execute($q)) {
    echo "Usuario $usuario creado con rol $rol\n";
} else {
    echo "Error: " . mysqli_error($c) . "\n";
}
