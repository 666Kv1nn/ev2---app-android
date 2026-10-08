-- =====================================================================
-- Monitor IoT - Esquema de base de datos (MySQL 8 en AWS RDS)
--
-- Uso (desde la Raspberry Pi, lo hace instalar.sh):
--   mysql -h <endpoint-rds> -u RSPPI -p --ssl-ca=/etc/ssl/rds/global-bundle.pem < esquema.sql
--
-- Se puede ejecutar varias veces: no borra ni duplica datos.
-- Los usuarios NO se crean aqui (para no dejar contraseñas en el repositorio):
-- se crean con `php crear_usuario.php` o desde la app (rol admin).
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `iot_app`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `iot_app`;

-- ---------------------------------------------------------------------
-- Usuarios de la app. La contraseña se guarda solo como hash bcrypt
-- (password_hash de PHP), nunca en texto plano.
-- Roles: admin (todo), operador (monitorea y controla), lector (solo ve).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `usuarios` (
  `id`             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `usuario`        VARCHAR(30)  NOT NULL,
  `password_hash`  VARCHAR(255) NOT NULL,
  `nombre`         VARCHAR(50)  NOT NULL,
  `apellido`       VARCHAR(50)  NOT NULL,
  `correo`         VARCHAR(100) NOT NULL,
  `telefono`       VARCHAR(20)  NOT NULL DEFAULT '',
  `rol`            ENUM('admin','operador','lector') NOT NULL DEFAULT 'lector',
  `activo`         TINYINT(1)   NOT NULL DEFAULT 1,
  `creado_en`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultimo_acceso`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuarios_usuario` (`usuario`),
  UNIQUE KEY `uk_usuarios_correo` (`correo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Sesiones: el token que recibe la app es aleatorio (256 bits) y aquí
-- solo se guarda su SHA-256. Si la tabla se filtra, los tokens no sirven.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sesiones` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `usuario_id`  INT UNSIGNED NOT NULL,
  `token_hash`  CHAR(64)     NOT NULL,
  `cliente`     VARCHAR(100) NOT NULL DEFAULT '',
  `ip`          VARCHAR(45)  NOT NULL DEFAULT '',
  `creado_en`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expira_en`   DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sesiones_token` (`token_hash`),
  KEY `ix_sesiones_usuario` (`usuario_id`),
  CONSTRAINT `fk_sesiones_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Intentos de login: sirve para bloquear fuerza bruta (5 fallos en
-- 15 minutos bloquean la cuenta 15 minutos) y como registro de auditoría.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `intentos_login` (
  `id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `usuario`  VARCHAR(30)  NOT NULL,
  `ip`       VARCHAR(45)  NOT NULL DEFAULT '',
  `exito`    TINYINT(1)   NOT NULL,
  `fecha`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_intentos_usuario_fecha` (`usuario`, `fecha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Dispositivos IoT conectados a la Raspberry Pi.
--   sensor   -> publica lecturas (tabla lecturas)
--   actuador -> tiene un estado 0/1 que la app cambia y el agente aplica
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dispositivos` (
  `id`              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `codigo`          VARCHAR(20)  NOT NULL,
  `nombre`          VARCHAR(50)  NOT NULL,
  `tipo`            ENUM('sensor','actuador') NOT NULL,
  `magnitud`        VARCHAR(30)  NOT NULL DEFAULT '',
  `unidad`          VARCHAR(10)  NOT NULL DEFAULT '',
  `ubicacion`       VARCHAR(50)  NOT NULL DEFAULT '',
  `umbral_min`      DECIMAL(8,2) NULL,
  `umbral_max`      DECIMAL(8,2) NULL,
  `estado`          TINYINT(1)   NOT NULL DEFAULT 0,
  `ultimo_valor`    DECIMAL(8,2) NULL,
  `ultima_conexion` DATETIME     NULL,
  `actualizado_por` INT UNSIGNED NULL,
  `actualizado_en`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispositivos_codigo` (`codigo`),
  CONSTRAINT `fk_dispositivos_usuario` FOREIGN KEY (`actualizado_por`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Lecturas de los sensores (serie de tiempo).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lecturas` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dispositivo_id`  INT UNSIGNED NOT NULL,
  `valor`           DECIMAL(8,2) NOT NULL,
  `registrado_en`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_lecturas_dispositivo_fecha` (`dispositivo_id`, `registrado_en`),
  CONSTRAINT `fk_lecturas_dispositivo` FOREIGN KEY (`dispositivo_id`) REFERENCES `dispositivos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Bitácora de eventos (trazabilidad, ISO/IEC 27400): quién hizo qué y cuándo.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `eventos` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `usuario_id`      INT UNSIGNED NULL,
  `dispositivo_id`  INT UNSIGNED NULL,
  `tipo`            VARCHAR(20)  NOT NULL,
  `detalle`         VARCHAR(200) NOT NULL,
  `ip`              VARCHAR(45)  NOT NULL DEFAULT '',
  `fecha`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_eventos_fecha` (`fecha`),
  CONSTRAINT `fk_eventos_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_eventos_dispositivo` FOREIGN KEY (`dispositivo_id`) REFERENCES `dispositivos` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Dispositivos de la Raspberry Pi. El agente (agente/agente_iot.py) usa
-- estos mismos códigos. Si no hay sensores físicos, el agente simula valores.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `dispositivos` (`codigo`, `nombre`, `tipo`, `magnitud`, `unidad`, `ubicacion`, `umbral_min`, `umbral_max`) VALUES
  ('TEMP-01', 'Temperatura',  'sensor',   'temperatura', '°C',  'Sala de equipos', 18.00, 28.00),
  ('HUM-01',  'Humedad',      'sensor',   'humedad',     '%',   'Sala de equipos', 30.00, 70.00),
  ('LUZ-01',  'Luminosidad',  'sensor',   'luz',         'lux', 'Sala de equipos', NULL,  NULL),
  ('VENT-01', 'Ventilador',   'actuador', 'ventilacion', '',    'Sala de equipos', NULL,  NULL),
  ('LED-01',  'Iluminación',  'actuador', 'luz',         '',    'Sala de equipos', NULL,  NULL),
  ('ALM-01',  'Alarma',       'actuador', 'alarma',      '',    'Acceso principal', NULL, NULL);
