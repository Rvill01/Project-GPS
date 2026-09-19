# Project GPS 🗺️

Aplicación Android de navegación y gestión de rutas geográficas, desarrollada en Kotlin con MapLibre y Room Database.

[Español](#spanish) | [English](#english)

---

## 📋 Descripción General

**Project GPS** es una aplicación móvil para Android que permite a los usuarios crear, visualizar y gestionar rutas geográficas sobre un mapa interactivo. La app utiliza MapLibre para la renderización del mapa y la API de Geoapify como fuente de estilos cartográficos, junto con Room Database para el almacenamiento local persistente de rutas y puntos de interés.

### Características principales

- **Mapa interactivo** con MapLibre Android SDK y estilos basados en OSM (OpenStreetMap) a través de Geoapify
- **Creación de rutas** tocando puntos sobre el mapa en tiempo real
- **Almacenamiento local** con Room Database (SQLite) para rutas, puntos y sectores
- **Gestión de rutas**: ver, renombrar, eliminar y visualizar rutas guardadas sobre el mapa
- **Geolocalización** mediante Google Play Services (FusedLocationProviderClient)
- **Organización por sectores** para clasificar rutas
- **Interfaz con navegación lateral** (Navigation Drawer) y botones de acción flotantes (FAB)
- **Animaciones** en la interfaz de usuario para una experiencia fluida

---

## 🏗️ Arquitectura y Tecnologías

### Stack Tecnológico

| Tecnología | Versión | Descripción |
|---|---|---|
| **Kotlin** | 1.9.23 | Lenguaje de programación principal |
| **Android Gradle Plugin (AGP)** | 8.6.1 | Herramienta de construcción de Android |
| **Compile SDK** | 36 | SDK de compilación de Android |
| **Min SDK** | 28 | Versión mínima de Android soportada (Android 8.0) |
| **Target SDK** | 36 | Versión objetivo de Android |
| **MapLibre GL Android** | 12.0.1 | Motor de mapas vectoriales de código abierto |
| **Room Database** | 2.6.1 | Capa de abstracción de SQLite de AndroidX |
| **Google Play Services - Location** | 21.3.0 | Servicios de geolocalización |
| **Material Design** | 1.12.0 | Componentes de UI de Google |
| **AndroidX Core KTX** | 1.13.1 | Extensiones de Kotlin para AndroidX |
| **Data Binding** | Habilitado | Enlace de datos en layouts XML |
| **Coroutines (kotlinx)** | A través de lifecycleScope | Programación asíncrona |
| **Geoapify API** | — | Servicio de estilos de mapas y geocoding |

### Arquitectura

El proyecto sigue una arquitectura simplificada tipo **MVVM (Model-View-ViewModel)** con las siguientes capas:

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  ┌──────────────┐  ┌────────────────────┐   │
│  │ MainActivity │  │ RutaSelectorFragment│   │
│  │ (Mapa + NAV) │  │  (Selector Sectores) │   │
│  └──────────────┘  └────────────────────┘   │
├─────────────────────────────────────────────┤
│               Data Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐ │
│  │  Ruta    │  │  Punto   │  │  Sector   │ │
│  │  Entity  │  │  Entity  │  │  Entity   │ │
│  └──────────┘  └──────────┘  └───────────┘ │
│  ┌──────────────────────────────────────┐   │
│  │        AppDatabase (Room)            │   │
│  │     DatabaseBuilder (Singleton)      │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │          RutaDao                     │   │
│  │    (@Query, @Insert, @Update,       │   │
│  │     @Delete, @Transaction)           │   │
│  └──────────────────────────────────────┘   │
├─────────────────────────────────────────────┤
│         External Services                    │
│  ┌──────────────┐  ┌────────────────────┐   │
│  │ MapLibre SDK │  │ Geoapify API       │   │
│  │ FusedLocation│  │ (Estilos de mapa)  │   │
│  │ Provider     │  │                    │   │
│  └──────────────┘  └────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Modelo de Datos (Room)

- **`Ruta`** (`rutas_db`): Almacena las rutas con nombre y referencia a un sector
- **`Punto`**: Coordenadas geográficas (latitud/longitud) asociadas a una ruta con orden de secuencia
- **`Sector`**: Clasificación numérica para organizar rutas geográficas

### Flujo de Trabajo

1. **Inicio**: La app solicita permisos de ubicación y centra el mapa en la posición del usuario
2. **Crear ruta**: El usuario activa el modo creación mediante el FAB, toca el mapa para agregar puntos y guarda con un nombre
3. **Ver rutas**: El menú lateral permite listar todas las rutas guardadas y seleccionar una para visualizarla en el mapa
4. **Gestionar rutas**: Se puede renombrar o eliminar cualquier ruta, incluyendo todos sus puntos asociados

---

## 📦 Instrucciones de Clonación e Instalación

### Prerrequisitos

- **Android Studio** Arctic Fox (2022.1.1) o superior
- **JDK** 11 o superior
- **Android SDK** con API Level 36 (Android 14) instalado
- **Conexión a Internet** para descargar dependencias

### Pasos

1. **Clonar el repositorio:**

   ```bash
   git clone https://github.com/tu-usuario/project-gps.git
   cd project-gps
   ```

2. **Crear el archivo `secrets.properties` en la raíz del proyecto:**

   > ⚠️ **Este paso es obligatorio para que el proyecto compile correctamente.**

   Crea un archivo llamado `secrets.properties` en la raíz del proyecto (junto a `build.gradle.kts` y `settings.gradle.kts`) con el siguiente contenido:

   ```properties
   GEOAPIFY_API_KEY=tu_clave
   ```

   Reemplaza `tu_clave` por tu clave API real de [Geoapify](https://geoapify.com/). Esta clave se utiliza para cargar los estilos de mapa (OSM Bright) en la aplicación.

3. **Sincronizar Gradle:**

   Abre el proyecto en Android Studio y haz clic en **"Sync Now"** cuando aparezca la notificación, o ejecuta:

   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

4. **Ejecutar la aplicación:**

   Conecta un dispositivo Android (API 28+) o inicia un emulador y ejecuta la app desde Android Studio.

### Estructura de archivos clave

```
project-gps/
├── app/
│   ├── build.gradle.kts              # Configuración de dependencias del módulo app
│   └── src/main/
│       ├── java/sv/vill/project_gps/
│       │   ├── MainActivity.kt        # Actividad principal (mapa, UI, lógica)
│       │   ├── menus/RutaSelectorFragment.kt  # Fragmento selector de sectores
│       │   └── data/
│       │       ├── Ruta.kt            # Entity Room: ruta
│       │       ├── Punto.kt           # Entity Room: punto geográfico
│       │       ├── Sector.kt          # Entity Room: sector
│       │       ├── RutaDao.kt         # DAO con operaciones de base de datos
│       │       ├── AppDatabase.kt     # Configuración de la base de datos Room
│       │       └── DatabaseBuilder.kt # Singleton de la base de datos
│       └── res/                       # Recursos (layouts, drawable, valores)
├── build.gradle.kts                   # Configuración global de plugins
├── settings.gradle.kts                # Configuración de repositorios y módulos
├── gradle.properties                  # Propiedades de Gradle
├── gradle/libs.versions.toml          # Catálogo de versiones de dependencias
├── secrets.properties                 # ⚠️ API key de Geoapify (CREAR MANUALMENTE)
└── local.properties                   # Ruta del SDK de Android (auto-generado)
```

### Nota sobre `secrets.properties`

El archivo `secrets.properties` **está incluido en `.gitignore`** por seguridad. El archivo que existe en este repositorio es solo de referencia con un ejemplo. Cada desarrollador debe crear su propio archivo con su clave API personal.

---

## 📄 Licencia

Este proyecto es de código abierto y disponible bajo la licencia MIT.

---

## 🔗 Enlaces de interés

- [MapLibre GL Android](https://github.com/maplibre/maplibre-native/tree/main/platforms/android)
- [Geoapify Maps API](https://www.geoapify.com/)
- [Room Database (AndroidX)](https://developer.android.com/topic/libraries/architecture/room)
- [Google Play Services Location](https://developers.google.com/android/guides/location)

---

<p id="english"></p>

## 📖 English Description

An Android application for GPS route mapping and management, built with Kotlin, MapLibre, and Room Database.

**Features:** Interactive map with MapLibre, route creation by tapping, local storage with Room, GPS tracking, and sector-based organization.

**Requirements:** Android Studio, JDK 11+, and a `secrets.properties` file at the project root containing `GEOAPIFY_API_KEY=your_key`.

---
