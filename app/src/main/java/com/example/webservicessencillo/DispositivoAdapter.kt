package com.example.webservicessencillo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class DispositivoAdapter(
    private var lista: List<Dispositivo>,
    private val puedeControlar: Boolean,
    private val alTocar: (Dispositivo) -> Unit,
    private val alCambiar: (Dispositivo, Boolean) -> Unit
) : RecyclerView.Adapter<DispositivoAdapter.ViewHolder>() {

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val fila: LinearLayout = vista.findViewById(R.id.fila)
        val imgIcono: ImageView = vista.findViewById(R.id.imgIcono)
        val txtNombre: TextView = vista.findViewById(R.id.txtNombre)
        val txtDetalle: TextView = vista.findViewById(R.id.txtDetalle)
        val txtValor: TextView = vista.findViewById(R.id.txtValor)
        val swEstado: SwitchMaterial = vista.findViewById(R.id.swEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_dispositivo, parent, false)
        return ViewHolder(vista)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = lista[position]
        val context = holder.itemView.context

        holder.txtNombre.text = d.nombre
        holder.imgIcono.setImageResource(iconoDispositivo(d.magnitud, d.tipo))
        holder.fila.setBackgroundResource(R.drawable.fondo_tarjeta)
        holder.txtDetalle.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario))
        holder.fila.setOnClickListener { alTocar(d) }

        if (d.tipo == "sensor") {
            holder.txtValor.visibility = View.VISIBLE
            holder.swEstado.visibility = View.GONE
            holder.txtValor.text = formatearValor(d.valor, d.unidad) + " " + d.unidad

            if (!d.enLinea) {
                holder.txtDetalle.text = "Sin conexión"
            } else if (d.alerta) {
                holder.txtDetalle.text = "FUERA DE RANGO"
                holder.txtDetalle.setTextColor(ContextCompat.getColor(context, R.color.rojo))
                holder.fila.setBackgroundResource(R.drawable.fondo_alerta)
            } else if (d.umbralMin != null && d.umbralMax != null) {
                holder.txtDetalle.text = "Normal (" + formatearValor(d.umbralMin, d.unidad) + " a " +
                        formatearValor(d.umbralMax, d.unidad) + " " + d.unidad + ")"
            } else {
                holder.txtDetalle.text = d.ubicacion
            }
        } else {
            holder.txtValor.visibility = View.GONE
            holder.swEstado.visibility = View.VISIBLE

            // se quita el listener antes de marcar el switch, si no se dispara solo
            holder.swEstado.setOnCheckedChangeListener(null)
            holder.swEstado.isChecked = d.encendido
            holder.swEstado.isEnabled = puedeControlar && d.enLinea
            holder.swEstado.setOnCheckedChangeListener { _, marcado -> alCambiar(d, marcado) }

            holder.txtDetalle.text = when {
                !d.enLinea -> "Sin conexión"
                d.encendido -> "Encendido"
                else -> "Apagado"
            }
        }
    }

    fun actualizar(nuevaLista: List<Dispositivo>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
