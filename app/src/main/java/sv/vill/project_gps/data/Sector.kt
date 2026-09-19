package sv.vill.project_gps.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sector(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numero: Int
)