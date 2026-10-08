package com.example.webservicessencillo

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONObject

// Detalle de un dispositivo:
//  - sensor: valor actual + grafico del historial (1 h, 24 h o 7 dias)
//  - actuador: estado + boton para encender/apagar
class DetalleActivity : AppCompatActivity() {

    lateinit var toolbar: MaterialToolbar
    lateinit var imgIcono: ImageView
    lateinit var txtValor: TextView
    lateinit var txtEstado: TextView
    lateinit var btnCambiar: Button
    lateinit var txtUltimoCambio: TextView
    lateinit var cajaHistorial: View
    lateinit var rgRango: RadioGroup
    lateinit var grafico: LineChart
    lateinit var txtResumen: TextView
    lateinit var txtInfo: TextView

    var idDispositivo = 0
    var tipo = ""
    var horas = 1
    var dispositivo: Dispositivo? = null

    val handler = Handler(Looper.getMainLooper())
    val actualizar = object : Runnable {
        override fun run() {
            cargar()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_detalle)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        relacionarVistas()

        idDispositivo = intent.getIntExtra("id", 0)
        tipo = intent.getStringExtra("tipo") ?: "sensor"

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        if (tipo == "sensor") {
            cajaHistorial.visibility = View.VISIBLE
            configurarGrafico()
            rgRango.setOnCheckedChangeListener { _, id ->
                horas = when (id) {
                    R.id.rb24h -> 24
                    R.id.rb7d -> 168
                    else -> 1
                }
                cargar()
            }
        } else {
            btnCambiar.visibility = View.VISIBLE
            txtUltimoCambio.visibility = View.VISIBLE
            btnCambiar.setOnClickListener { cambiarEstado() }
        }
    }

    private fun relacionarVistas() {
        toolbar = findViewById(R.id.toolbar)
        imgIcono = findViewById(R.id.imgIcono)
        txtValor = findViewById(R.id.txtValor)
        txtEstado = findViewById(R.id.txtEstado)
        btnCambiar = findViewById(R.id.btnCambiar)
        txtUltimoCambio = findViewById(R.id.txtUltimoCambio)
        cajaHistorial = findViewById(R.id.cajaHistorial)
        rgRango = findViewById(R.id.rgRango)
        grafico = findViewById(R.id.grafico)
        txtResumen = findViewById(R.id.txtResumen)
        txtInfo = findViewById(R.id.txtInfo)
    }

    override fun onResume() {
        super.onResume()
        handler.post(actualizar)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(actualizar)
    }

    private fun cargar() {
        if (tipo == "sensor") {
            // lecturas.php devuelve el sensor y su historial
            Api.get(this, "lecturas.php?dispositivo_id=$idDispositivo&horas=$horas", { respuesta ->
                mostrarDispositivo(leerDispositivo(respuesta.getJSONObject("dispositivo")))
                mostrarGrafico(respuesta)
            }, { error -> txtEstado.text = error })
        } else {
            // para los actuadores se busca en la lista completa
            Api.get(this, "dispositivos.php", { respuesta ->
                val arreglo = respuesta.getJSONArray("dispositivos")
                for (i in 0 until arreglo.length()) {
                    val d = leerDispositivo(arreglo.getJSONObject(i))
                    if (d.id == idDispositivo) {
                        mostrarDispositivo(d)
                    }
                }
            }, { error -> txtEstado.text = error })
        }
    }

    private fun mostrarDispositivo(d: Dispositivo) {
        dispositivo = d
        supportActionBar?.title = d.nombre
        supportActionBar?.subtitle = d.codigo + " · " + d.tipo
        imgIcono.setImageResource(iconoDispositivo(d.magnitud, d.tipo))

        val conexion = if (d.enLinea) "En línea" else "Sin conexión"

        if (d.tipo == "sensor") {
            txtValor.text = formatearValor(d.valor, d.unidad) + " " + d.unidad
            if (d.alerta) {
                txtValor.setTextColor(ContextCompat.getColor(this, R.color.rojo))
                txtEstado.text = "FUERA DE RANGO · " + conexion
            } else {
                txtValor.setTextColor(ContextCompat.getColor(this, R.color.texto))
                txtEstado.text = "Normal · " + conexion
            }
        } else {
            txtValor.text = if (d.encendido) "ENCENDIDO" else "APAGADO"
            txtValor.setTextColor(ContextCompat.getColor(this, if (d.encendido) R.color.verde else R.color.texto_secundario))
            txtEstado.text = conexion
            btnCambiar.text = if (d.encendido) "APAGAR" else "ENCENDER"

            if (!Sesion.puedeControlar(this)) {
                btnCambiar.isEnabled = false
                btnCambiar.text = "SOLO LECTURA"
            }
            if (d.actualizadoPor != null) {
                txtUltimoCambio.text = "Último cambio: " + d.actualizadoPor + " (" + haceCuanto(d.actualizadoEn) + ")"
            } else {
                txtUltimoCambio.text = "Sin cambios registrados"
            }
        }

        var info = "Código: " + d.codigo + "\nUbicación: " + d.ubicacion
        if (d.umbralMin != null && d.umbralMax != null) {
            info += "\nRango permitido: " + formatearValor(d.umbralMin, d.unidad) + " a " +
                    formatearValor(d.umbralMax, d.unidad) + " " + d.unidad
        }
        info += "\nÚltimo reporte: " + haceCuanto(d.ultimaConexion)
        txtInfo.text = info
    }

    private fun cambiarEstado() {
        val d = dispositivo ?: return
        btnCambiar.isEnabled = false

        val datos = JSONObject()
        datos.put("dispositivo_id", d.id)
        datos.put("estado", !d.encendido)

        Api.post(this, "control.php", datos, { respuesta ->
            btnCambiar.isEnabled = true
            mostrarDispositivo(leerDispositivo(respuesta.getJSONObject("dispositivo")))
        }, { error ->
            btnCambiar.isEnabled = true
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        })
    }

    private fun configurarGrafico() {
        grafico.description.isEnabled = false
        grafico.legend.isEnabled = false
        grafico.axisRight.isEnabled = false
        grafico.setNoDataText("Sin lecturas en este periodo")
        grafico.xAxis.position = XAxis.XAxisPosition.BOTTOM
        grafico.xAxis.setDrawGridLines(false)
        grafico.xAxis.granularity = 1f
        grafico.xAxis.setLabelCount(4, false)
    }

    private fun mostrarGrafico(respuesta: JSONObject) {
        val d = dispositivo ?: return
        val puntos = respuesta.getJSONArray("puntos")

        val entradas = ArrayList<Entry>()
        val horasEje = ArrayList<String>()
        var minimo = Float.MAX_VALUE
        var maximo = -Float.MAX_VALUE
        for (i in 0 until puntos.length()) {
            val p = puntos.getJSONObject(i)
            val v = p.getDouble("v").toFloat()
            entradas.add(Entry(i.toFloat(), v))
            horasEje.add(if (horas > 24) formatearFecha(p.getString("t")) else formatearHora(p.getString("t")))
            if (v < minimo) minimo = v
            if (v > maximo) maximo = v
        }

        if (entradas.isEmpty()) {
            grafico.clear()
            txtResumen.text = "Sin lecturas en este periodo"
            return
        }

        val naranjo = ContextCompat.getColor(this, R.color.naranjo)
        val linea = LineDataSet(entradas, d.nombre)
        linea.color = naranjo
        linea.lineWidth = 2f
        linea.setDrawCircles(false)
        linea.setDrawValues(false)
        linea.setDrawFilled(true)
        linea.fillColor = naranjo
        linea.fillAlpha = 40

        grafico.data = LineData(linea)
        grafico.xAxis.valueFormatter = IndexAxisValueFormatter(horasEje)

        // lineas rojas con el rango permitido
        val eje = grafico.axisLeft
        eje.removeAllLimitLines()
        val rojo = ContextCompat.getColor(this, R.color.rojo)
        if (d.umbralMin != null) {
            val l = LimitLine(d.umbralMin.toFloat(), "Mín")
            l.lineColor = rojo
            l.enableDashedLine(10f, 10f, 0f)
            eje.addLimitLine(l)
            if (d.umbralMin.toFloat() < minimo) minimo = d.umbralMin.toFloat()
        }
        if (d.umbralMax != null) {
            val l = LimitLine(d.umbralMax.toFloat(), "Máx")
            l.lineColor = rojo
            l.enableDashedLine(10f, 10f, 0f)
            eje.addLimitLine(l)
            if (d.umbralMax.toFloat() > maximo) maximo = d.umbralMax.toFloat()
        }
        // para que se vean las lineas del rango aunque los datos esten lejos
        eje.axisMinimum = minimo - 1
        eje.axisMaximum = maximo + 1
        grafico.invalidate()

        val resumen = respuesta.getJSONObject("resumen")
        txtResumen.text = "Mínimo: " + formatearValor(numeroONull(resumen, "min"), d.unidad) + " " + d.unidad +
                "\nPromedio: " + formatearValor(numeroONull(resumen, "prom"), d.unidad) + " " + d.unidad +
                "\nMáximo: " + formatearValor(numeroONull(resumen, "max"), d.unidad) + " " + d.unidad +
                "\nLecturas: " + resumen.getInt("total")
    }
}
