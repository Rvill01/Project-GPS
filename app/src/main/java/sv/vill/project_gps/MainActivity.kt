package sv.vill.project_gps
import sv.vill.project_gps.BuildConfig
import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import sv.vill.project_gps.data.AppDatabase
import sv.vill.project_gps.data.DatabaseBuilder
import sv.vill.project_gps.data.Punto
import sv.vill.project_gps.data.Ruta
import sv.vill.project_gps.data.Sector

class MainActivity : AppCompatActivity() {
    private var mapClickListener: MapLibreMap.OnMapClickListener? = null
    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null
    private lateinit var db: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var creandoRuta = false
    private val puntosTemp = mutableListOf<Punto>()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(applicationContext)
        setContentView(R.layout.activity_main)

        db = DatabaseBuilder.getInstance(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val fabBuscar = findViewById<FloatingActionButton>(R.id.fabBuscar)
        val fabUbic = findViewById<FloatingActionButton>(R.id.fabUbic)
        val tvCount = findViewById<TextView>(R.id.tvPuntosCount)
        val layoutCreacion = findViewById<LinearLayout>(R.id.layoutCreacionRuta)

        // 📋 Menú lateral
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ver_rutas -> {
                    mostrarListaDeRutas()
                    drawerLayout.close()
                    true
                }
                R.id.nav_crear_ruta -> {
                    iniciarCreacionRuta(fabBuscar, tvCount, layoutCreacion)
                    drawerLayout.close()
                    true
                }
                else -> false
            }
        }

        // 🔍 FAB Buscar/Guardar
        fabBuscar.setOnClickListener {
            if (creandoRuta) {
                guardarRutaTemporal(fabBuscar, layoutCreacion)
            } else {
                mostrarListaDeRutas()
            }
        }

        // 📍 FAB Ubicación
        fabUbic.setOnClickListener {
            centrarEnUbicacionActual()
        }

        // 🗺️ Cargar mapa
        mapView.getMapAsync { map ->
            mapLibreMap = map
            val styleUrl = "https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            map.setStyle(Style.Builder().fromUri(styleUrl)) {
                verificarPermisosUbicacion()
            }
        }
    }

    // 📍 Verificación de permisos de ubicación
    private fun verificarPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            centrarEnUbicacionActual()
        }
    }

    // 🎯 Método centrarEnUbicacionActual modificado
    @SuppressLint("MissingPermission")
    private fun centrarEnUbicacionActual(coordenadaEspecifica: LatLng? = null) {
        val map = mapLibreMap ?: return

        // Si se pasa una coordenada directa, se mueve directamente la cámara a ese punto
        if (coordenadaEspecifica != null) {
            moverCamaraAMiUbicacion(map, coordenadaEspecifica)
            return
        }

        // Si no se pasaron coordenadas, se comprueban permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        @SuppressLint("MissingPermission")
        fun centrarEnUbicacionActual() {
            val map = mapLibreMap ?: return

            // 1. Limpiar líneas y marcadores de rutas en pantalla
            limpiarRutaDibujada()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val miPos = LatLng(location.latitude, location.longitude)

                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(miPos)
                                .zoom(16.0)
                                .build()
                        ), 1800
                    )
                    map.clear()
                    map.addMarker(MarkerOptions().position(miPos).title("Tu ubicación"))
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación GPS.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Intentar obtener la última ubicación conocida
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val miPos = LatLng(location.latitude, location.longitude)
                moverCamaraAMiUbicacion(map, miPos)
            } else {
                // Fallback: solicitar ubicación actual precisa si la última ubicación es nula
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { currentLocation ->
                    if (currentLocation != null) {
                        val miPos = LatLng(currentLocation.latitude, currentLocation.longitude)
                        moverCamaraAMiUbicacion(map, miPos)
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun moverCamaraAMiUbicacion(map: MapLibreMap, pos: LatLng) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(pos)
                    .zoom(16.0)
                    .build()
            ), 1800
        )
        map.clear()
        map.addMarker(MarkerOptions().position(pos).title("Tu ubicación"))
    }

    // 🧭 Ver rutas
    private fun mostrarListaDeRutas() {
        lifecycleScope.launch {
            val rutas = db.rutaDao().getAllRutas()
            if (rutas.isEmpty()) {
                Toast.makeText(this@MainActivity, "No hay rutas guardadas.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val nombres = rutas.map { it.nombre }.toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Selecciona una ruta")
                .setItems(nombres) { _, which ->
                    val rutaSeleccionada = rutas[which]
                    mostrarOpcionesDeRuta(rutaSeleccionada)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
    }

    private fun mostrarOpcionesDeRuta(ruta: Ruta) {
        val opciones = arrayOf("Ver en el mapa", "Renombrar", "Eliminar")

        AlertDialog.Builder(this)
            .setTitle(ruta.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        // Ver en el mapa
                        lifecycleScope.launch {
                            limpiarRutaDibujada()
                            mapLibreMap?.clear()
                            dibujarRutaEnMapa(ruta.id)
                        }
                    }
                    1 -> {
                        // Renombrar ruta
                        renombrarRutaDialog(ruta)
                    }
                    2 -> {
                        // Eliminar ruta
                        confirmarEliminacionRuta(ruta)
                    }
                }
            }
            .show()
    }

    private fun renombrarRutaDialog(ruta: Ruta) {
        val input = android.widget.EditText(this).apply {
            setText(ruta.nombre)
        }

        AlertDialog.Builder(this)
            .setTitle("Renombrar Ruta")
            .setView(input)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    lifecycleScope.launch {
                        val rutaActualizada = ruta.copy(nombre = nuevoNombre)
                        db.rutaDao().updateRuta(rutaActualizada)
                        Toast.makeText(this@MainActivity, "Ruta renombrada con éxito", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacionRuta(ruta: Ruta) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar ruta?")
            .setMessage("¿Estás seguro de que deseas eliminar \"${ruta.nombre}\"? Esta acción borrará la ruta y sus puntos.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    db.rutaDao().eliminarRutaConPuntos(ruta)
                    limpiarRutaDibujada()
                    mapLibreMap?.clear()
                    Toast.makeText(this@MainActivity, "Ruta eliminada.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ✏️ Dibujar ruta
    private suspend fun dibujarRutaEnMapa(rutaId: Int) {
        val puntos = db.rutaDao().getPuntosPorRuta(rutaId)
        if (puntos.isEmpty()) return

        val coords = puntos.map { LatLng(it.latitud, it.longitud) }
        val map = mapLibreMap ?: return

        map.getStyle { style ->
            val line = LineString.fromLngLats(coords.map { Point.fromLngLat(it.longitude, it.latitude) })
            val source = GeoJsonSource("ruta-source", FeatureCollection.fromFeature(Feature.fromGeometry(line)))
            if (style.getSource("ruta-source") == null) style.addSource(source)

            val layer = LineLayer("ruta-layer", "ruta-source").apply {
                setProperties(PropertyFactory.lineColor(Color.BLUE), PropertyFactory.lineWidth(5f))
            }
            if (style.getLayer("ruta-layer") == null) style.addLayer(layer)
        }

        val primera = coords.first()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(primera, 15.5))
    }

    // 🧹 Limpiar mapa
    private fun limpiarRutaDibujada() {
        mapLibreMap?.getStyle { style ->
            style.removeLayer("ruta-layer")
            style.removeSource("ruta-source")
        }
    }

    // 🚧 Iniciar creación
    private fun iniciarCreacionRuta(fabBuscar: FloatingActionButton, tvCount: TextView, layoutCreacion: LinearLayout) {
        val map = mapLibreMap ?: return
        creandoRuta = true
        puntosTemp.clear()
        limpiarRutaDibujada()
        mapLibreMap?.clear()

        animarFabCambio(fabBuscar, android.R.drawable.ic_menu_save)
        layoutCreacion.visibility = View.VISIBLE
        tvCount.text = "Puntos: 0"

        Toast.makeText(this, "Modo creación activado. Toca el mapa para agregar puntos.", Toast.LENGTH_LONG).show()
        mapClickListener = MapLibreMap.OnMapClickListener { latLng ->
            val punto = Punto(
                id = 0,
                rutaId = 0,
                latitud = latLng.latitude,
                longitud = latLng.longitude,
                orden = puntosTemp.size + 1
            )

            puntosTemp.add(punto)
            map.addMarker(MarkerOptions().position(latLng))
            tvCount.text = "Puntos: ${puntosTemp.size}"
            dibujarLineaTemporal(map)
            true
        }

        map.addOnMapClickListener(mapClickListener!!)
    }

    // 💾 Guardar ruta
    @androidx.annotation.RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
    )

    private fun guardarRutaTemporal(fabBuscar: FloatingActionButton, layoutCreacion: LinearLayout) {
        if (puntosTemp.isEmpty()) {
            Toast.makeText(this, "No hay puntos para guardar.", Toast.LENGTH_SHORT).show()
            return
        }

        // Campo de texto para ingresar el nombre
        val inputNombre = android.widget.EditText(this).apply {
            hint = "Ejemplo: Ruta Sector Norte"
        }

        AlertDialog.Builder(this)
            .setTitle("Nombre de la ruta")
            .setMessage("Ingresa un nombre para guardar esta ruta:")
            .setView(inputNombre)
            .setPositiveButton("Guardar") { _, _ ->
                val nombreIngresado = inputNombre.text.toString().trim()
                val nombreFinal = if (nombreIngresado.isNotEmpty()) nombreIngresado else "Ruta ${System.currentTimeMillis()}"

                // Guardar en base de datos dentro de corrutina
                lifecycleScope.launch {
                    val sectorId = db.rutaDao().insertSector(Sector(numero = 1)).toInt()
                    val rutaId = db.rutaDao().insertRuta(
                        Ruta(nombre = nombreFinal, sectorId = sectorId)
                    ).toInt()

                    puntosTemp.forEach { it.rutaId = rutaId }
                    db.rutaDao().insertPuntos(puntosTemp)

                    Toast.makeText(
                        this@MainActivity,
                        "Ruta \"$nombreFinal\" guardada con ${puntosTemp.size} puntos.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Restaurar interfaz del mapa
                    creandoRuta = false
                    puntosTemp.clear()
                    layoutCreacion.visibility = View.GONE
                    animarFabCambio(fabBuscar, android.R.drawable.ic_menu_search)
                    limpiarRutaDibujada()
                    mapLibreMap?.clear()

                    mapClickListener?.let { mapLibreMap?.removeOnMapClickListener(it) }
                    mapClickListener = null

                    mapLibreMap?.getStyle { style ->
                        style.removeLayer("temp-layer")
                        style.removeSource("temp-line")
                    }

                    centrarEnUbicacionActual()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centrarEnUbicacionActual()
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación para continuar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🟢 Dibujar línea temporal
    private fun dibujarLineaTemporal(map: MapLibreMap) {
        val style = map.style ?: return
        val coords = puntosTemp.map { Point.fromLngLat(it.longitud, it.latitud) }
        val line = LineString.fromLngLats(coords)

        val srcId = "temp-line"
        val source = style.getSourceAs<GeoJsonSource>(srcId)
        if (source != null) source.setGeoJson(line)
        else {
            val newSrc = GeoJsonSource(srcId, line)
            style.addSource(newSrc)
            style.addLayer(LineLayer("temp-layer", srcId).withProperties(
                PropertyFactory.lineColor(Color.GREEN),
                PropertyFactory.lineWidth(4f)
            ))
        }
    }

    // ✨ Animación del FAB
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    private fun animarFabCambio(fab: FloatingActionButton, icono: Int) {
        val animOut = ObjectAnimator.ofFloat(fab, "rotation", 0f, 180f)
        animOut.duration = 200
        animOut.doOnEnd {
            fab.setImageResource(icono)
            val animIn = ObjectAnimator.ofFloat(fab, "rotation", 180f, 360f)
            animIn.duration = 200
            animIn.start()
        }
        animOut.start()
    }

    // 🔁 Ciclo de vida
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
}