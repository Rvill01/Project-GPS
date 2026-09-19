package sv.vill.project_gps.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Sector::class, Ruta::class, Punto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rutaDao(): RutaDao
}
