package com.example.practica05.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("RegistroPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_MATRICULA = "key_matricula"
        const val KEY_NOMBRE = "key_nombre"
    }


    fun saveDatos(matricula: String, nombre: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_MATRICULA, matricula)
        editor.putString(KEY_NOMBRE, nombre)
        editor.apply()
    }

    fun getMatricula(): String {
        return sharedPreferences.getString(KEY_MATRICULA, "") ?: ""
    }

    fun getNombre(): String {
        return sharedPreferences.getString(KEY_NOMBRE, "") ?: ""
    }
}