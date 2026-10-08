package com.example.webservicessencillo

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar

// Bitacora: quien encendio/apago que, alertas, inicios de sesion, etc.
class ActividadActivity : AppCompatActivity() {

    lateinit var swipe: SwipeRefreshLayout
    lateinit var recycler: RecyclerView
    lateinit var txtVacio: TextView
    val eventos = ArrayList<Evento>()

    val handler = Handler(Looper.getMainLooper())
    val actualizar = object : Runnable {
        override fun run() {
            cargarEventos()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_lista)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Actividad"
        toolbar.subtitle = "Últimos 60 eventos"
        toolbar.setNavigationOnClickListener { finish() }

        swipe = findViewById(R.id.swipe)
        recycler = findViewById(R.id.recycler)
        txtVacio = findViewById(R.id.txtVacio)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = EventoAdapter()
        swipe.setOnRefreshListener { cargarEventos() }
    }

    override fun onResume() {
        super.onResume()
        handler.post(actualizar)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(actualizar)
    }

    private fun cargarEventos() {
        Api.get(this, "eventos.php?limite=60", { respuesta ->
            swipe.isRefreshing = false
            eventos.clear()
            val arreglo = respuesta.getJSONArray("eventos")
            for (i in 0 until arreglo.length()) {
                val e = arreglo.getJSONObject(i)
                eventos.add(Evento(e.getString("tipo"), e.getString("detalle"), e.getString("fecha"), textoONull(e, "usuario")))
            }
            recycler.adapter?.notifyDataSetChanged()
            txtVacio.text = "Todavía no hay actividad"
            txtVacio.visibility = if (eventos.isEmpty()) View.VISIBLE else View.GONE
        }, { error ->
            swipe.isRefreshing = false
            txtVacio.text = error
            txtVacio.visibility = View.VISIBLE
        })
    }

    inner class EventoAdapter : RecyclerView.Adapter<EventoAdapter.ViewHolder>() {

        inner class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
            val imgIcono: ImageView = vista.findViewById(R.id.imgIcono)
            val txtDetalle: TextView = vista.findViewById(R.id.txtDetalle)
            val txtInfo: TextView = vista.findViewById(R.id.txtInfo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false))
        }

        override fun getItemCount(): Int = eventos.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val e = eventos[position]
            holder.txtDetalle.text = e.detalle

            // las alertas las genera la Raspberry Pi, no un usuario
            val quien = e.usuario ?: if (e.tipo == "alerta") "Raspberry Pi" else "Sistema"
            holder.txtInfo.text = quien + " · " + haceCuanto(e.fecha)

            val icono = when (e.tipo) {
                "control" -> R.drawable.ic_dispositivo
                "alerta" -> R.drawable.ic_alarma
                "usuarios" -> R.drawable.ic_usuarios
                else -> R.drawable.ic_usuario
            }
            holder.imgIcono.setImageResource(icono)
            val color = if (e.tipo == "alerta") R.color.rojo else R.color.naranjo
            holder.imgIcono.setColorFilter(ContextCompat.getColor(holder.itemView.context, color))
        }
    }
}
