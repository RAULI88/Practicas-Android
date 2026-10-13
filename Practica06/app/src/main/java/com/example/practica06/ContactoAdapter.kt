package com.example.practica06

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactoAdapter(
    private val contactos: List<Contacto>,
    private val onItemSelected: (Contacto) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    // Patrón ViewHolder implementado
    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvTelefono: TextView = view.findViewById(R.id.tvTelefono)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.tvNombre.text = contacto.nombre
        holder.tvTelefono.text = "Tel: ${contacto.telefono}"

        // Captura de eventos de clic sobre el elemento de la lista
        holder.itemView.setOnClickListener {
            onItemSelected(contacto)
        }
    }

    override fun getItemCount(): Int = contactos.size
}