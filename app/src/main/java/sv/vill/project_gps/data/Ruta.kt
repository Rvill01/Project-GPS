package sv.vill.project_gps.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Ruta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val sectorId: Int
)