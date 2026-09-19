package sv.vill.project_gps.data

import androidx.room.*

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update


@Dao
interface RutaDao {

    // 📌 Insertar una ruta y devolver su ID
    @Insert
    suspend fun insertRuta(ruta: Ruta): Long

    // 📌 Insertar una lista de puntos
    @Insert
    suspend fun insertPuntos(puntos: List<Punto>)

    // 📌 Insertar un sector
    @Insert
    suspend fun insertSector(sector: Sector): Long

    // 📌 Obtener todos los sectores
    @Query("SELECT * FROM Sector ORDER BY numero ASC")
    suspend fun getAllSectores(): List<Sector>

    // 📌 Obtener todos los puntos de una ruta
    @Query("SELECT * FROM Punto WHERE rutaId = :rutaId ORDER BY orden ASC")
    suspend fun getPuntosPorRuta(rutaId: Int): List<Punto>

    // 📌 Contar puntos totales en la base
    @Query("SELECT COUNT(*) FROM Punto")
    suspend fun countPuntos(): Int

    // 📌 Obtener todas las rutas
    @Query("SELECT * FROM Ruta ORDER BY id ASC")
    suspend fun getAllRutas(): List<Ruta>

    // 📌 Eliminar una ruta específica (opcional)
    @Delete
    suspend fun deleteRuta(ruta: Ruta)

    // 📌 Actualizar una ruta (por si quieres renombrarla después)
    @Update
    suspend fun updateRuta(ruta: Ruta)

    // 📌 Eliminar todos los puntos de una ruta
    @Query("DELETE FROM Punto WHERE rutaId = :rutaId")
    suspend fun deletePuntosPorRuta(rutaId: Int)

    // 📌 Eliminar todas las rutas y puntos (reset general)
    @Query("DELETE FROM Ruta")
    suspend fun deleteAllRutas()

    @Query("DELETE FROM Punto")
    suspend fun deleteAllPuntos()

    @Query("DELETE FROM Sector")
    suspend fun deleteAllSectores()

    @Transaction
    suspend fun eliminarRutaConPuntos(ruta: Ruta) {
        deletePuntosPorRuta(ruta.id)
        deleteRuta(ruta)
    }
    

}