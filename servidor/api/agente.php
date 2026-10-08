<?php
// Lo usa el agente de la Raspberry Pi (no la app).
// Cabecera X-Device-Key con la clave de config.php
// POST {"lecturas": {"TEMP-01": 23.4, "HUM-01": 51.0, ...}}
// Responde el estado que deben tener los actuadores: {"actuadores": {"VENT-01": true, ...}}
include 'funciones.php';

$clave = $_SERVER['HTTP_X_DEVICE_KEY'] ?? '';
if (!hash_equals($config['clave_dispositivo'], $clave)) {
    responderError(401, 'Dispositivo no autorizado');
}

$datos = leerJson();
$lecturas = $datos['lecturas'] ?? [];

foreach ($lecturas as $codigo => $valor) {
    if (!is_numeric($valor)) {
        continue;
    }

    $q = mysqli_prepare($c, "SELECT * FROM dispositivos WHERE codigo = ? AND tipo = 'sensor'");
    mysqli_stmt_bind_param($q, 's', $codigo);
    mysqli_stmt_execute($q);
    $sensor = mysqli_fetch_assoc(mysqli_stmt_get_result($q));
    if (!$sensor) {
        continue;
    }

    $valor = round($valor, 2);
    $id = $sensor['id'];

    $q = mysqli_prepare($c, "INSERT INTO lecturas (dispositivo_id, valor) VALUES (?, ?)");
    mysqli_stmt_bind_param($q, 'id', $id, $valor);
    mysqli_stmt_execute($q);

    $q = mysqli_prepare($c, "UPDATE dispositivos SET ultimo_valor = ?, ultima_conexion = UTC_TIMESTAMP() WHERE id = ?");
    mysqli_stmt_bind_param($q, 'di', $valor, $id);
    mysqli_stmt_execute($q);

    // alerta en la bitacora solo cuando el valor recien sale del rango
    $antes = $sensor['ultimo_valor'];
    $min = $sensor['umbral_min'];
    $max = $sensor['umbral_max'];
    $fueraAhora = ($min !== null && $valor < $min) || ($max !== null && $valor > $max);
    $fueraAntes = $antes !== null && (($min !== null && $antes < $min) || ($max !== null && $antes > $max));
    if ($fueraAhora && !$fueraAntes) {
        registrarEvento($c, null, $id, 'alerta', $sensor['nombre'] . ' fuera de rango: ' . $valor . ' ' . $sensor['unidad']);
    }
}

// los actuadores estan conectados a la misma Pi, si el agente responde estan en linea
mysqli_query($c, "UPDATE dispositivos SET ultima_conexion = UTC_TIMESTAMP() WHERE tipo = 'actuador'");

// de vez en cuando se borran las lecturas de mas de 30 dias
if (rand(1, 500) == 1) {
    mysqli_query($c, "DELETE FROM lecturas WHERE registrado_en < UTC_TIMESTAMP() - INTERVAL 30 DAY");
}

$actuadores = [];
$resultado = mysqli_query($c, "SELECT codigo, estado FROM dispositivos WHERE tipo = 'actuador'");
while ($fila = mysqli_fetch_assoc($resultado)) {
    $actuadores[$fila['codigo']] = $fila['estado'] == 1;
}

responder(200, ['actuadores' => $actuadores]);
