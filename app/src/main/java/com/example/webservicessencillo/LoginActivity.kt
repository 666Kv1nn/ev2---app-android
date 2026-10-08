package com.example.webservicessencillo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    lateinit var etUsuario: EditText
    lateinit var etPassword: EditText
    lateinit var cbRecordar: CheckBox
    lateinit var btnIngresar: Button
    lateinit var progreso: ProgressBar
    lateinit var txtError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // si ya hay una sesion guardada se entra directo al panel
        if (Sesion.obtenerToken(this) != null) {
            irAlPanel()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        relacionarVistas()

        val recordado = Sesion.obtenerUsuarioRecordado(this)
        if (recordado != null) {
            etUsuario.setText(recordado)
            cbRecordar.isChecked = true
        }

        // mensaje cuando se vuelve al login porque la sesion expiro
        val mensaje = intent.getStringExtra("mensaje")
        if (mensaje != null) {
            mostrarError(mensaje)
        }

        btnIngresar.setOnClickListener { ingresar() }
        etPassword.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                ingresar()
            }
            false
        }
    }

    private fun relacionarVistas() {
        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        cbRecordar = findViewById(R.id.cbRecordar)
        btnIngresar = findViewById(R.id.btnIngresar)
        progreso = findViewById(R.id.progreso)
        txtError = findViewById(R.id.txtError)
    }

    private fun ingresar() {
        val usuario = etUsuario.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError("Completa usuario y contraseña")
            return
        }

        cargando(true)
        txtError.visibility = View.GONE

        // las credenciales van en el cuerpo del POST, nunca en la URL
        val datos = JSONObject()
        datos.put("usuario", usuario)
        datos.put("password", password)
        datos.put("cliente", Build.MANUFACTURER + " " + Build.MODEL)

        Api.post(this, "login.php", datos, { respuesta ->
            Sesion.guardar(this, respuesta.getString("token"), respuesta.getJSONObject("usuario"))
            if (cbRecordar.isChecked) {
                Sesion.guardarUsuarioRecordado(this, usuario)
            } else {
                Sesion.guardarUsuarioRecordado(this, null)
            }
            irAlPanel()
        }, { error ->
            cargando(false)
            etPassword.setText("")
            mostrarError(error)
        })
    }

    private fun cargando(si: Boolean) {
        progreso.visibility = if (si) View.VISIBLE else View.GONE
        btnIngresar.isEnabled = !si
        btnIngresar.text = if (si) "VERIFICANDO..." else "INGRESAR"
    }

    private fun mostrarError(mensaje: String) {
        txtError.text = mensaje
        txtError.visibility = View.VISIBLE
    }

    private fun irAlPanel() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
