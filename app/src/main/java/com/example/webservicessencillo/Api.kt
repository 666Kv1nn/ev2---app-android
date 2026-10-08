package com.example.webservicessencillo

import android.content.Context
import android.content.Intent
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

// Peticiones a la API de la Raspberry Pi con Volley.
// Todas las peticiones llevan el token en la cabecera Authorization.
object Api {

    private var cola: RequestQueue? = null

    private fun obtenerCola(context: Context): RequestQueue {
        if (cola == null) {
            cola = Volley.newRequestQueue(context.applicationContext)
        }
        return cola!!
    }

    fun get(context: Context, ruta: String, ok: (JSONObject) -> Unit, error: (String) -> Unit) {
        enviar(context, Request.Method.GET, ruta, null, ok, error)
    }

    fun post(context: Context, ruta: String, datos: JSONObject, ok: (JSONObject) -> Unit, error: (String) -> Unit) {
        enviar(context, Request.Method.POST, ruta, datos, ok, error)
    }

    private fun enviar(
        context: Context,
        metodo: Int,
        ruta: String,
        datos: JSONObject?,
        ok: (JSONObject) -> Unit,
        error: (String) -> Unit
    ) {
        val peticion = object : JsonObjectRequest(
            metodo, Conexion.URL_WEB_SERVICES + ruta, datos,
            Response.Listener { respuesta -> ok(respuesta) },
            Response.ErrorListener { e -> manejarError(context, e, error) }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val cabeceras = HashMap<String, String>()
                val token = Sesion.obtenerToken(context)
                if (token != null) {
                    cabeceras["Authorization"] = "Bearer $token"
                }
                return cabeceras
            }
        }
        // 8 segundos de espera y sin reintentos (para no mandar dos veces una orden)
        peticion.retryPolicy = DefaultRetryPolicy(8000, 0, 1f)
        obtenerCola(context).add(peticion)
    }

    private fun manejarError(context: Context, e: VolleyError, error: (String) -> Unit) {
        val respuesta = e.networkResponse
        if (respuesta == null) {
            // sin red, la Pi apagada o el certificado no coincide
            error("No se pudo conectar con la Raspberry Pi. Revisa la conexión Wi-Fi.")
            return
        }

        // la API siempre responde {"error": "..."} cuando algo falla
        var mensaje = "Error del servidor (" + respuesta.statusCode + ")"
        try {
            mensaje = JSONObject(String(respuesta.data)).getString("error")
        } catch (ex: Exception) {
        }

        // 401 = token vencido o cuenta desactivada -> volver al login
        if (respuesta.statusCode == 401 && Sesion.obtenerToken(context) != null) {
            Sesion.cerrar(context)
            val i = Intent(context, LoginActivity::class.java)
            i.putExtra("mensaje", mensaje)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(i)
            return
        }
        error(mensaje)
    }
}
