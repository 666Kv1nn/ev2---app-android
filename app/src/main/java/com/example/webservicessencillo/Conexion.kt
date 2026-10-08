package com.example.webservicessencillo

object Conexion {
    // API en la Raspberry Pi (HTTPS). Si cambia la IP hay que cambiarla tambien en
    // res/xml/network_security_config.xml y generar otro certificado (ver README)
    const val URL_WEB_SERVICES = "https://10.16.1.28/iot/"
}
