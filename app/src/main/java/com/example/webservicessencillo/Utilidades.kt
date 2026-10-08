package com.example.webservicessencillo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Funciones de ayuda que se usan en varias pantallas

// La API manda las fechas en UTC: "2026-10-06T14:32:05Z"
fun leerFecha(iso: String?): Date? {
    if (iso == null || iso.length < 19) return null
    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return try {
        formato.parse(iso.substring(0, 19))
    } catch (e: Exception) {
        null
    }
}

// "14:32"
fun formatearHora(iso: String?): String {
    val fecha = leerFecha(iso) ?: return "-"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(fecha)
}

// "06/10 14:32"
fun formatearFecha(iso: String?): String {
    val fecha = leerFecha(iso) ?: return "-"
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(fecha)
}

// "hace 5 s", "hace 3 min", "hace 2 h" o la fecha
fun haceCuanto(iso: String?, ahora: Long = System.currentTimeMillis()): String {
    val fecha = leerFecha(iso) ?: return "nunca"
    val segundos = (ahora - fecha.time) / 1000
    return when {
        segundos < 5 -> "ahora"
        segundos < 60 -> "hace $segundos s"
        segundos < 3600 -> "hace ${segundos / 60} min"
        segundos < 86400 -> "hace ${segundos / 3600} h"
        else -> formatearFecha(iso)
    }
}

// 23.46 -> "23,5"   (la luz va sin decimales)
fun formatearValor(valor: Double?, unidad: String): String {
    if (valor == null) return "-"
    return if (unidad == "lux") {
        String.format(Locale.forLanguageTag("es"), "%.0f", valor)
    } else {
        String.format(Locale.forLanguageTag("es"), "%.1f", valor)
    }
}

fun iconoDispositivo(magnitud: String, tipo: String): Int {
    return when (magnitud) {
        "temperatura" -> R.drawable.ic_temperatura
        "humedad" -> R.drawable.ic_humedad
        "luz" -> if (tipo == "sensor") R.drawable.ic_luz else R.drawable.ic_ampolleta
        "ventilacion" -> R.drawable.ic_ventilador
        "alarma" -> R.drawable.ic_alarma
        else -> R.drawable.ic_dispositivo
    }
}

fun nombreRol(rol: String): String {
    return when (rol) {
        "admin" -> "Administrador"
        "operador" -> "Operador"
        else -> "Lector"
    }
}

// mismas reglas que usuarios.php
fun usuarioValido(usuario: String): Boolean = Regex("^[a-z0-9._-]{3,30}$").matches(usuario)

fun passwordValida(password: String): Boolean {
    return password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}

fun correoValido(correo: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()
