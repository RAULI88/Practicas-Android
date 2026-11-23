package com.example.practica14.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MarcadorEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun marcadorDao(): MarcadorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton: una única instancia del motor SQLite (evita fugas de memoria)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mapa_marcadores.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
