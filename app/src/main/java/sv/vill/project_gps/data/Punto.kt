package sv.vill.project_gps.data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Punto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var rutaId: Int,
    val latitud: Double,
    val longitud: Double,
    val orden: Int
)