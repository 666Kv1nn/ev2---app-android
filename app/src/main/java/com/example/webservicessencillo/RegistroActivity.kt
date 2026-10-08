package com.example.webservicessencillo

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

// Registro de usuarios nuevos (solo admin). Antes era la pantalla principal de la app.
class RegistroActivity : AppCompatActivity() {

    lateinit var etNombre: EditText
    lateinit var etApellido: EditText
    lateinit var etCorreo: EditText
    lateinit var etTelefono: EditText
    lateinit var etUsuario: EditText
    lateinit var etPassword: EditText
    lateinit var tilNombre: TextInputLayout
    lateinit var tilApellido: TextInputLayout
    lateinit var tilCorreo: TextInputLayout
    lateinit var tilUsuario: TextInputLayout
    lateinit var tilPassword: TextInputLayout
    lateinit var rgRol: RadioGroup
    lateinit var btnGuardar: Button
    lateinit var btnLimpiar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_registro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        relacionarVistas()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        btnGuardar.setOnClickListener { guardar() }
        btnLimpiar.setOnClickListener { limpiar() }
    }

    private fun relacionarVistas() {
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreo = findViewById(R.id.etCorreo)
        etTelefono = findViewById(R.id.etTelefono)
        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        tilNombre = findViewById(R.id.tilNombre)
        tilApellido = findViewById(R.id.tilApellido)
        tilCorreo = findViewById(R.id.tilCorreo)
        tilUsuario = findViewById(R.id.tilUsuario)
        tilPassword = findViewById(R.id.tilPassword)
        rgRol = findViewById(R.id.rgRol)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnLimpiar = findViewById(R.id.btnLimpiar)
    }

    // revisa el formulario antes de enviarlo (el servidor lo vuelve a revisar)
    private fun validar(): Boolean {
        var ok = true
        tilNombre.error = null
        tilApellido.error = null
        tilCorreo.error = null
        tilUsuario.error = null
        tilPassword.error = null

        if (etNombre.text.toString().trim().isEmpty()) {
            tilNombre.error = "Obligatorio"
            ok = false
        }
        if (etApellido.text.toString().trim().isEmpty()) {
            tilApellido.error = "Obligatorio"
            ok = false
        }
        if (!correoValido(etCorreo.text.toString().trim())) {
            tilCorreo.error = "Correo no válido"
            ok = false
        }
        if (!usuarioValido(etUsuario.text.toString().trim().lowercase())) {
            tilUsuario.error = "De 3 a 30 caracteres: letras, números, punto o guion"
            ok = false
        }
        if (!passwordValida(etPassword.text.toString())) {
            tilPassword.error = "Mínimo 8 caracteres, con letras y números"
            ok = false
        }
        return ok
    }

    private fun guardar() {
        if (!validar()) return

        val rol = when (rgRol.checkedRadioButtonId) {
            R.id.rbAdmin -> "admin"
            R.id.rbLector -> "lector"
            else -> "operador"
        }

        val datos = JSONObject()
        datos.put("nombre", etNombre.text.toString().trim())
        datos.put("apellido", etApellido.text.toString().trim())
        datos.put("correo", etCorreo.text.toString().trim().lowercase())
        datos.put("telefono", etTelefono.text.toString().trim())
        datos.put("usuario", etUsuario.text.toString().trim().lowercase())
        datos.put("password", etPassword.text.toString())
        datos.put("rol", rol)

        btnGuardar.isEnabled = false
        Api.post(this, "usuarios.php", datos, {
            Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show()
            finish()
        }, { error ->
            btnGuardar.isEnabled = true
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        })
    }

    private fun limpiar() {
        etNombre.setText("")
        etApellido.setText("")
        etCorreo.setText("")
        etTelefono.setText("")
        etUsuario.setText("")
        etPassword.setText("")
        rgRol.check(R.id.rbOperador)
    }
}
