package com.example.webservicessencillo

import org.json.JSONObject

data class Dispositivo(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val tipo: String,          // "sensor" o "actuador"
    val magnitud: String,      // temperatura, humedad, luz, ventilacion, alarma
    val unidad: String,
    val ubicacion: String,
    val umbralMin: Double?,
    val umbralMax: Double?,
    var encendido: Boolean,
    val valor: Double?,
    val alerta: Boolean,
    val enLinea: Boolean,
    val ultimaConexion: String?,
    val actualizadoPor: String?,
    val actualizadoEn: String?
)

data class Evento(
    val tipo: String,
    val detalle: String,
    val fecha: String,
    val usuario: String?
)

data class Usuario(
    val id: Int,
    val usuario: String,
    val nombre: String,
    val rol: String,
    var activo: Boolean,
    val ultimoAcceso: String?
)

// org.json devuelve "null" como texto si el campo es null, por eso se revisa con isNull
fun textoONull(json: JSONObject, campo: String): String? {
    return if (json.isNull(campo)) null else json.getString(campo)
}

fun numeroONull(json: JSONObject, campo: String): Double? {
    return if (json.isNull(campo)) null else json.getDouble(campo)
}

fun leerDispositivo(json: JSONObject): Dispositivo {
    return Dispositivo(
        id = json.getInt("id"),
        codigo = json.getString("codigo"),
        nombre = json.getString("nombre"),
        tipo = json.getString("tipo"),
        magnitud = json.getString("magnitud"),
        unidad = json.getString("unidad"),
        ubicacion = json.getString("ubicacion"),
        umbralMin = numeroONull(json, "umbral_min"),
        umbralMax = numeroONull(json, "umbral_max"),
        encendido = json.getBoolean("estado"),
        valor = numeroONull(json, "valor"),
        alerta = json.getBoolean("alerta"),
        enLinea = json.getBoolean("en_linea"),
        ultimaConexion = textoONull(json, "ultima_conexion"),
        actualizadoPor = textoONull(json, "actualizado_por"),
        actualizadoEn = textoONull(json, "actualizado_en")
    )
}
