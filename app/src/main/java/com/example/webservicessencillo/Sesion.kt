package com.example.webservicessencillo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

// Guarda la sesion del usuario en el telefono.
// Se usa EncryptedSharedPreferences para que el token quede cifrado (AES-256)
// con una clave del Android Keystore. La contraseña NO se guarda nunca.
object Sesion {

    private fun preferencias(context: Context): SharedPreferences {
        val clave = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "sesion_segura",
            clave,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // se llama despues del login con lo que responde login.php
    fun guardar(context: Context, token: String, usuario: JSONObject) {
        preferencias(context).edit()
            .putString("token", token)
            .putInt("id", usuario.getInt("id"))
            .putString("usuario", usuario.getString("usuario"))
            .putString("nombre", usuario.getString("nombre") + " " + usuario.getString("apellido"))
            .putString("rol", usuario.getString("rol"))
            .apply()
    }

    fun obtenerToken(context: Context): String? = preferencias(context).getString("token", null)

    fun obtenerId(context: Context): Int = preferencias(context).getInt("id", 0)

    fun obtenerNombre(context: Context): String = preferencias(context).getString("nombre", "") ?: ""

    fun obtenerRol(context: Context): String = preferencias(context).getString("rol", "lector") ?: "lector"

    fun esAdmin(context: Context): Boolean = obtenerRol(context) == "admin"

    // el rol lector solo puede mirar
    fun puedeControlar(context: Context): Boolean = obtenerRol(context) != "lector"

    fun cerrar(context: Context) {
        preferencias(context).edit()
            .remove("token").remove("id").remove("usuario").remove("nombre").remove("rol")
            .apply()
    }

    // opcion "Recordar usuario" del login (solo el nombre de usuario)
    fun guardarUsuarioRecordado(context: Context, usuario: String?) {
        preferencias(context).edit().putString("recordado", usuario).apply()
    }

    fun obtenerUsuarioRecordado(context: Context): String? = preferencias(context).getString("recordado", null)
}
