package com.example.webservicessencillo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONObject

// Panel principal: estado de la Raspberry Pi, sensores y actuadores
class MainActivity : AppCompatActivity() {

    lateinit var toolbar: MaterialToolbar
    lateinit var swipe: SwipeRefreshLayout
    lateinit var imgEstadoPi: ImageView
    lateinit var txtEstadoPi: TextView
    lateinit var txtActualizado: TextView
    lateinit var txtAviso: TextView
    lateinit var txtSoloLectura: TextView
    lateinit var recyclerSensores: RecyclerView
    lateinit var recyclerActuadores: RecyclerView

    lateinit var adapterSensores: DispositivoAdapter
    lateinit var adapterActuadores: DispositivoAdapter

    // para no pisar el switch mientras se esta enviando una orden
    var enviando = false

    // se consulta la API cada 3 segundos mientras la pantalla esta abierta
    val handler = Handler(Looper.getMainLooper())
    val actualizar = object : Runnable {
        override fun run() {
            cargarDispositivos()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        relacionarVistas()

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Monitor IoT"
        supportActionBar?.subtitle = Sesion.obtenerNombre(this) + " · " + nombreRol(Sesion.obtenerRol(this))

        val puedeControlar = Sesion.puedeControlar(this)
        if (!puedeControlar) {
            txtSoloLectura.visibility = View.VISIBLE
        }

        adapterSensores = DispositivoAdapter(listOf(), puedeControlar, { abrirDetalle(it) }, { _, _ -> })
        adapterActuadores = DispositivoAdapter(listOf(), puedeControlar, { abrirDetalle(it) }, { d, encender ->
            cambiarEstado(d, encender)
        })
        recyclerSensores.layoutManager = LinearLayoutManager(this)
        recyclerSensores.adapter = adapterSensores
        recyclerActuadores.layoutManager = LinearLayoutManager(this)
        recyclerActuadores.adapter = adapterActuadores

        swipe.setColorSchemeColors(ContextCompat.getColor(this, R.color.naranjo))
        swipe.setOnRefreshListener { cargarDispositivos() }
    }

    private fun relacionarVistas() {
        toolbar = findViewById(R.id.toolbar)
        swipe = findViewById(R.id.swipe)
        imgEstadoPi = findViewById(R.id.imgEstadoPi)
        txtEstadoPi = findViewById(R.id.txtEstadoPi)
        txtActualizado = findViewById(R.id.txtActualizado)
        txtAviso = findViewById(R.id.txtAviso)
        txtSoloLectura = findViewById(R.id.txtSoloLectura)
        recyclerSensores = findViewById(R.id.recyclerSensores)
        recyclerActuadores = findViewById(R.id.recyclerActuadores)
    }

    override fun onResume() {
        super.onResume()
        handler.post(actualizar)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(actualizar)
    }

    private fun cargarDispositivos() {
        Api.get(this, "dispositivos.php", { respuesta ->
            swipe.isRefreshing = false
            if (enviando) return@get

            val sensores = ArrayList<Dispositivo>()
            val actuadores = ArrayList<Dispositivo>()
            val alertas = ArrayList<String>()

            val arreglo = respuesta.getJSONArray("dispositivos")
            for (i in 0 until arreglo.length()) {
                val d = leerDispositivo(arreglo.getJSONObject(i))
                if (d.tipo == "sensor") {
                    sensores.add(d)
                    if (d.alerta) {
                        alertas.add(d.nombre + ": " + formatearValor(d.valor, d.unidad) + " " + d.unidad)
                    }
                } else {
                    actuadores.add(d)
                }
            }
            adapterSensores.actualizar(sensores)
            adapterActuadores.actualizar(actuadores)

            // estado de la Raspberry Pi
            val gateway = respuesta.getJSONObject("gateway")
            val ultima = textoONull(gateway, "ultima_conexion")
            if (gateway.getBoolean("en_linea")) {
                txtEstadoPi.text = "Raspberry Pi en línea"
                imgEstadoPi.setColorFilter(ContextCompat.getColor(this, R.color.verde))
            } else {
                txtEstadoPi.text = "Raspberry Pi sin conexión"
                imgEstadoPi.setColorFilter(ContextCompat.getColor(this, R.color.rojo))
            }
            txtActualizado.text = "Último reporte: " + haceCuanto(ultima) + " · 10.16.1.28"

            if (alertas.isEmpty()) {
                txtAviso.visibility = View.GONE
            } else {
                txtAviso.text = "⚠ Fuera de rango: " + alertas.joinToString(", ")
                txtAviso.visibility = View.VISIBLE
            }
        }, { error ->
            swipe.isRefreshing = false
            txtAviso.text = error
            txtAviso.visibility = View.VISIBLE
            txtEstadoPi.text = "Sin conexión con el servidor"
            imgEstadoPi.setColorFilter(ContextCompat.getColor(this, R.color.rojo))
        })
    }

    // encender o apagar un actuador
    private fun cambiarEstado(d: Dispositivo, encender: Boolean) {
        enviando = true
        val datos = JSONObject()
        datos.put("dispositivo_id", d.id)
        datos.put("estado", encender)

        Api.post(this, "control.php", datos, {
            enviando = false
            val texto = if (encender) "encendido" else "apagado"
            Toast.makeText(this, d.nombre + " " + texto, Toast.LENGTH_SHORT).show()
            cargarDispositivos()
        }, { error ->
            enviando = false
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            cargarDispositivos() // vuelve el switch a como estaba
        })
    }

    private fun abrirDetalle(d: Dispositivo) {
        val i = Intent(this, DetalleActivity::class.java)
        i.putExtra("id", d.id)
        i.putExtra("tipo", d.tipo)
        startActivity(i)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // solo el administrador gestiona usuarios
        menu.findItem(R.id.menuUsuarios).isVisible = Sesion.esAdmin(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuActividad -> startActivity(Intent(this, ActividadActivity::class.java))
            R.id.menuUsuarios -> startActivity(Intent(this, UsuariosActivity::class.java))
            R.id.menuSalir -> confirmarSalir()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmarSalir() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        // se borra el token en el servidor y en el telefono
        Api.post(this, "logout.php", JSONObject(), { irAlLogin() }, { irAlLogin() })
    }

    private fun irAlLogin() {
        Sesion.cerrar(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
