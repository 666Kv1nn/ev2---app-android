package com.example.webservicessencillo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import org.json.JSONObject

// Lista de usuarios (solo administradores). Permite activar/desactivar cuentas
// y crear usuarios nuevos con el boton +
class UsuariosActivity : AppCompatActivity() {

    lateinit var swipe: SwipeRefreshLayout
    lateinit var recycler: RecyclerView
    lateinit var txtVacio: TextView
    val usuarios = ArrayList<Usuario>()

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
        toolbar.title = "Usuarios"
        toolbar.setNavigationOnClickListener { finish() }

        swipe = findViewById(R.id.swipe)
        recycler = findViewById(R.id.recycler)
        txtVacio = findViewById(R.id.txtVacio)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.visibility = View.VISIBLE
        fab.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = UsuarioAdapter()
        swipe.setOnRefreshListener { cargarUsuarios() }
    }

    // se recarga al volver de RegistroActivity
    override fun onResume() {
        super.onResume()
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        Api.get(this, "usuarios.php", { respuesta ->
            swipe.isRefreshing = false
            usuarios.clear()
            val arreglo = respuesta.getJSONArray("usuarios")
            for (i in 0 until arreglo.length()) {
                val u = arreglo.getJSONObject(i)
                usuarios.add(
                    Usuario(
                        u.getInt("id"),
                        u.getString("usuario"),
                        u.getString("nombre") + " " + u.getString("apellido"),
                        u.getString("rol"),
                        u.getBoolean("activo"),
                        textoONull(u, "ultimo_acceso")
                    )
                )
            }
            recycler.adapter?.notifyDataSetChanged()
            txtVacio.visibility = View.GONE
        }, { error ->
            swipe.isRefreshing = false
            txtVacio.text = error
            txtVacio.visibility = View.VISIBLE
        })
    }

    private fun cambiarActivo(u: Usuario, activo: Boolean) {
        val datos = JSONObject()
        datos.put("accion", "estado")
        datos.put("id", u.id)
        datos.put("activo", activo)

        Api.post(this, "usuarios.php", datos, {
            val texto = if (activo) "activado" else "desactivado"
            Toast.makeText(this, u.usuario + " " + texto, Toast.LENGTH_SHORT).show()
            cargarUsuarios()
        }, { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            cargarUsuarios()
        })
    }

    inner class UsuarioAdapter : RecyclerView.Adapter<UsuarioAdapter.ViewHolder>() {

        inner class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
            val txtIniciales: TextView = vista.findViewById(R.id.txtIniciales)
            val txtNombre: TextView = vista.findViewById(R.id.txtNombre)
            val txtInfo: TextView = vista.findViewById(R.id.txtInfo)
            val swActivo: SwitchMaterial = vista.findViewById(R.id.swActivo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false))
        }

        override fun getItemCount(): Int = usuarios.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val u = usuarios[position]
            val esYo = u.id == Sesion.obtenerId(this@UsuariosActivity)

            holder.txtIniciales.text = u.nombre.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
            holder.txtNombre.text = if (esYo) u.nombre + " (tú)" else u.nombre
            holder.txtInfo.text = "@" + u.usuario + " · " + nombreRol(u.rol) + " · último acceso: " + haceCuanto(u.ultimoAcceso)
            holder.itemView.alpha = if (u.activo) 1f else 0.5f

            holder.swActivo.setOnCheckedChangeListener(null)
            holder.swActivo.isChecked = u.activo
            holder.swActivo.isEnabled = !esYo // no te puedes desactivar a ti mismo
            holder.swActivo.setOnCheckedChangeListener { _, marcado -> cambiarActivo(u, marcado) }
        }
    }
}
