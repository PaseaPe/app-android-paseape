# 🐾 PaseaPe Móvil — Guía de Arquitectura y Desarrollo

¡Bienvenido al proyecto **PaseaPe Móvil**! Esta aplicación Android nativa ha sido construida para conectar a dueños de mascotas con paseadores calificados en Lima, Perú. 

Este documento fue diseñado especialmente para estudiantes y desarrolladores que dan sus primeros pasos en el ecosistema Android moderno. Aquí aprenderás **qué hace cada carpeta**, **por qué separamos el código en capas**, **cómo viaja la información desde los servidores hasta la pantalla** y **cómo crear nuevas pantallas o consumir nuevos endpoints** sin romper nada en el intento.

---

## 📑 Tabla de Contenidos

1. [La Gran Analogía: ¿Cómo funciona esta app por dentro?](#1-la-gran-analogía-cómo-funciona-esta-app-por-dentro)
2. [Stack Tecnológico Oficial](#2-stack-tecnológico-oficial)
3. [Las 3 Capas de Clean Architecture](#3-las-3-capas-de-clean-architecture)
4. [Mapa del Tesoro: Estructura de Carpetas](#4-mapa-del-tesoro-estructura-de-carpetas)
5. [El Viaje de los Datos: Del Servidor a la Pantalla (UDF)](#5-el-viaje-de-los-datos-del-servidor-a-la-pantalla-udf)
6. [Conceptos Clave Explicados Fácil](#6-conceptos-clave-explicados-fácil)
   - [¿Qué es un DTO vs un Modelo de Dominio?](#qué-es-un-dto-vs-un-modelo-de-dominio)
   - [¿Qué es un Mapper y por qué lo usamos?](#qué-es-un-mapper-y-por-qué-lo-usamos)
   - [Estrategia de Catálogos (Zero Lookup Overhead)](#estrategia-de-catálogos-zero-lookup-overhead)
   - [Navegación Type-Safe con Compose](#navegación-type-safe-con-compose)
   - [Inyección de Dependencias con Hilt](#inyección-de-dependencias-con-hilt)
7. [Receta de Cocina: ¿Cómo añadir una nueva pantalla y endpoint?](#7-receta-de-cocina-cómo-añadir-una-nueva-pantalla-y-endpoint)
8. [Configuración Local y Primeros Pasos](#8-configuración-local-y-primeros-pasos)
9. [Reglas de Oro del Proyecto](#9-reglas-de-oro-del-proyecto)
10. [Glosario Rápido para Principiantes](#10-glosario-rápido-para-principiantes)

---

## 1. La Gran Analogía: ¿Cómo funciona esta app por dentro?

Imagina que nuestra app es un **restaurante de alta cocina**:

```
+-------------------------------------------------------------+
|                 COMEDOR (Presentation)                      |
|  - El cliente mira la carta bonita (Screen / UI).           |
|  - El mozo toma la orden y trae los platos (ViewModel).    |
+------------------------------|------------------------------+
                               | Pide platos / Entrega comida
+------------------------------v------------------------------+
|                 EL CHEF Y LA CARTA (Domain)                 |
|  - Las recetas sagradas del restaurante (Use Cases).         |
|  - Los nombres de los platos y sus ingredientes (Modelos).  |
|  - No le importa qué camión trajo la verdura, solo la regla.|
+------------------------------|------------------------------+
                               | Solicita insumos frescos
+------------------------------v------------------------------+
|                 LA DESPENSA Y COMPRAS (Data)                |
|  - El camión repartidor del mercado (Retrofit / API).       |
|  - Las cajas selladas de fábrica en snake_case (DTOs).      |
|  - El ayudante que lava y pica la verdura (Mappers).        |
|  - La alacena donde guardamos las llaves (DataStore).       |
+-------------------------------------------------------------+
```

Si el proveedor de carne cambia (Data), el cliente en el comedor (UI) no tiene por qué enterarse ni cambiar su mesa. De eso se trata **Clean Architecture**: que cada parte haga su trabajo sin estorbar a las demás.

---

## 2. Stack Tecnológico Oficial

En PaseaPe no usamos tecnologías obsoletas (nada de archivos XML para pantallas ni librerías viejas). Todo está en la vanguardia de Android:

- **Kotlin 2.x:** Lenguaje moderno, conciso y seguro contra nulos (`Null Safety`).
- **Jetpack Compose + Material Design 3 (M3):** La interfaz se dibuja enteramente con funciones en código Kotlin (`@Composable`), adoptando las guías visuales Material You.
- **Dagger-Hilt:** Inyector de dependencias. Se encarga de fabricar y entregar objetos automáticamente a quien los necesite sin hacer `new Objeto()` manualmente.
- **Coroutines & Kotlin Flow (`StateFlow`):** Tareas en segundo plano (peticiones a internet) y transmisión reactiva de datos en tiempo real siguiendo el patrón **UDF (Unidirectional Data Flow)**.
- **Retrofit 2 + OkHttp 4:** Cliente HTTP para hacer llamadas a nuestra `api-paseape`.
- **Kotlinx Serialization:** Conversor súper rápido de JSON a objetos Kotlin (mapeando `snake_case` de la base de datos a `camelCase` de Kotlin).
- **Jetpack DataStore Preferences:** Guarda datos ligeros y la sesión del usuario (Token JWT, correo, rol, UUID) de forma cifrada y reactiva.
- **Navigation Compose (Type-Safe):** Cambio de pantallas sin cadenas de texto propensas a errores tipográficos, usando clases y objetos Kotlin `@Serializable`.
- **Google Identity (Credential Manager API):** Inicio de sesión moderno con cuentas de Google mediante credenciales oficiales de Android.

---

## 3. Las 3 Capas de Clean Architecture

El proyecto divide su lógica en tres grandes paquetes ubicados en `com.example.paseape`:

### Capa 1: `domain` (El Corazón del Negocio)
* **Regla suprema:** Es código 100% Kotlin puro. **No debe importar nada de Android**, ni Retrofit, ni Compose.
* Si el día de mañana cambiamos Retrofit por Ktor o cambiamos Compose por otra tecnología, la capa de dominio **no se toca**.
* Contiene:
  - `model`: Modelos puros que representan cosas del mundo real (`User`, `Paseador`, `CatalogItem`).
  - `repository`: Interfaces (contratos) que dicen **qué** datos se necesitan, pero no **cómo** se obtienen (`AuthRepository`, `PaseadorRepository`).
  - `usecase`: Operaciones específicas y atómicas que puede hacer un usuario (`LoginUseCase`, `GetPaseadoresUseCase`, `RegisterGoogleClientUseCase`).

### Capa 2: `data` (Los Proveedores y el Almacenamiento)
* Se encarga de buscar los datos donde estén: en el servidor web (`remote`) o en la memoria del teléfono (`local`).
* Contiene:
  - `remote/api`: Interfaces de Retrofit con anotaciones HTTP como `@GET`, `@POST`, `@PUT`.
  - `remote/dto`: Objetos de transferencia de datos (**Data Transfer Objects**). Reflejan exactamente el formato JSON que envía o recibe el backend.
  - `remote/mapper`: Funciones que convierten un DTO (crudo, con nombres en `snake_case`) a un Modelo de Dominio limpio y seguro.
  - `local/session`: El archivo `SessionManager.kt` que usa DataStore para recordar si el usuario ya inició sesión.
  - `repository`: Implementaciones reales de las interfaces de dominio (`AuthRepositoryImpl`, `PaseadorRepositoryImpl`).

### Capa 3: `presentation` (Lo que el Usuario Ve y Toca)
* Aquí vive la interfaz gráfica construida con **Jetpack Compose**.
* Sigue el patrón **MVVM (Model-View-ViewModel)** con flujo unidireccional (UDF):
  - **Screen (Vista):** Función Composable pura que solo dibuja botones, textos e imágenes basados en un estado (`UiState`). Cuando el usuario hace click, avisa mediante un evento (`UiEvent`).
  - **ViewModel:** El cerebro de la pantalla. Recibe los eventos del usuario, llama a los Casos de Uso (`UseCases`) y emite un nuevo estado inmutable a través de un `StateFlow<UiState>`.
  - **theme:** Colores oficiales de PaseaPe (`PrimaryOrange`, `PrimaryDark`, etc.), tipografías y formas.
  - **components:** Botones (`AppButton`), cajas de texto (`AppTextField`) y menús desplegables (`AppDropdownMenu`) reutilizables.
  - **navigation:** Define la máquina de navegación (`AppNavHost.kt`) y las rutas seguras (`ScreenRoute.kt`).

---

## 4. Mapa del Tesoro: Estructura de Carpetas

A continuación se muestra el árbol de directorios real del proyecto con la explicación de qué va en cada lugar:

```text
app/src/main/
├── AndroidManifest.xml                  <- Permisos (INTERNET) y actividad principal
├── res/                                 <- Recursos visuales y textos estáticos
│   ├── drawable/                        <- Imágenes (perritos, logo, íconos vectoriales)
│   ├── mipmap-*/                        <- Ícono de la app en varias densidades
│   └── values/
│       ├── strings.xml                  <- ¡OBLIGATORIO! Todos los textos de la app van aquí
│       ├── colors.xml                   <- Colores base del sistema Android
│       └── themes.xml                   <- Tema XML puente para la actividad
└── java/com/example/paseape/
    ├── PaseaPeApp.kt                    <- Clase Application con @HiltAndroidApp
    │
    ├── core/                            <- Utilidades transversales y configuración global
    │   ├── auth/                        <- Gestores de autenticación externa
    │   │   └── GoogleAuthManager.kt     <- Manejo de Credential Manager para Google Sign-In
    │   ├── di/                          <- Módulos de Inyección de Dependencias (Dagger Hilt)
    │   │   ├── NetworkModule.kt         <- Provee OkHttpClient, Retrofit, ApiService y Json
    │   │   ├── RepositoryModule.kt      <- Enlaza las interfaces de Repository con sus Impl
    │   │   └── DataStoreModule.kt       <- Provee DataStore y SessionManager como Singleton
    │   ├── network/                     <- Herramientas de red
    │   │   ├── ApiResponse.kt           <- Sealed interface de estados de red (Success, Error, Loading)
    │   │   └── AuthInterceptor.kt       <- Interceptor que inyecta automáticamente el token JWT
    │   └── util/                        <- Constantes y validaciones
    │       ├── AppConfig.kt             <- Acceso a BASE_URL y GOOGLE_WEB_CLIENT_ID
    │       ├── AppConstants.kt          <- Roles, IDs constantes, endpoints públicos
    │       └── ValidationUtils.kt       <- Validadores de correo, contraseña, etc.
    │
    ├── domain/                          <- CAPA 1: Negocio puro (Sin Android)
    │   ├── model/                       <- Clases de datos limpias
    │   │   ├── User.kt                  <- Representa a un usuario autenticado
    │   │   ├── Paseador.kt              <- Representa la ficha de un paseador
    │   │   ├── CatalogItem.kt           <- Par id-descripcion para combos (distritos, razas)
    │   │   ├── ApplicationParameters.kt <- Todos los catálogos juntos
    │   │   └── Register*Params.kt       <- Parámetros limpios para registrar usuarios/mascotas
    │   ├── repository/                  <- Interfaces (Contratos)
    │   │   ├── AuthRepository.kt
    │   │   ├── ParameterRepository.kt
    │   │   └── PaseadorRepository.kt
    │   └── usecase/                     <- Casos de uso atómicos
    │       ├── auth/                    <- LoginUseCase, RegisterGoogleClientUseCase, etc.
    │       ├── parameter/               <- GetApplicationParametersUseCase, GetRazasByTipoUseCase
    │       └── paseador/                <- GetPaseadoresUseCase
    │
    ├── data/                            <- CAPA 2: Acceso a Datos y Red
    │   ├── local/
    │   │   └── session/
    │   │       └── SessionManager.kt    <- Guarda el token, rol y correo en DataStore
    │   ├── remote/
    │   │   ├── api/
    │   │   │   └── ApiService.kt        <- Contratos Retrofit (@GET, @POST, @PUT)
    │   │   ├── dto/                     <- Objetos idénticos al JSON del backend
    │   │   │   ├── common/              <- ResponseDto<T> y HeaderDto
    │   │   │   ├── auth/                <- LoginReqDto, LoginResDto, UsuarioReqDto, etc.
    │   │   │   ├── parameter/           <- ParameterItemDto, ApplicationParametersResDto
    │   │   │   ├── paseador/            <- PaseadorDto, CoberturaReqDto
    │   │   │   └── mascota/             <- MascotaReqDto
    │   │   └── mapper/                  <- Traductores DTO <-> Domain
    │   │       └── UsuarioMapper.kt     <- Convierte RegisterParams en UsuarioReqDto/MascotaReqDto
    │   └── repository/                  <- Implementaciones concretas de los repositorios
    │       ├── AuthRepositoryImpl.kt
    │       ├── ParameterRepositoryImpl.kt
    │       └── PaseadorRepositoryImpl.kt
    │
    └── presentation/                    <- CAPA 3: Interfaz de Usuario (Compose)
        ├── MainActivity.kt              <- Actividad única (@AndroidEntryPoint)
        ├── theme/                       <- Sistema de diseño Material 3
        │   ├── Color.kt                 <- Paleta de colores oficial
        │   ├── Theme.kt                 <- PaseaPETheme con soporte claro/oscuro
        │   └── Type.kt                  <- Configuración tipográfica
        ├── components/                  <- Componentes reusables
        │   ├── AppButton.kt             <- Botón estilizado con indicador de carga
        │   ├── AppTextField.kt          <- Campo de texto con borde y mensaje de error
        │   └── AppDropdownMenu.kt       <- Selector desplegable de catálogos
        ├── navigation/                  <- Rutas y Grafo de Pantallas
        │   ├── ScreenRoute.kt           <- Rutas fuertemente tipadas (@Serializable)
        │   └── AppNavHost.kt            <- Grafo de navegación con NavHost
        └── screens/                     <- Módulos visuales por funcionalidad
            ├── splash/                  <- Pantalla de bienvenida / carga inicial
            ├── account/                 <- Pantalla de bienvenida para elegir login o registro
            ├── auth/                    <- LoginScreen, LoginViewModel, LoginUiState, LoginUiEvent
            │   └── google/              <- Flujo dinámico de registro con Google
            ├── dashboard/               <- Pantalla principal con categorías y recomendaciones
            └── paseadores/              <- Listado y búsqueda en tiempo real de paseadores
```

---

## 5. El Viaje de los Datos: Del Servidor a la Pantalla (UDF)

¿Cómo hace un botón de la pantalla para traer datos del servidor y pintarlos? En Android moderno todo sigue el patrón **UDF (Unidirectional Data Flow)**: los datos viajan en un solo sentido circular.

```text
 [1. Usuario toca "Iniciar Sesión"]
                 │
                 ▼
 [2. Screen emite un LoginUiEvent.SubmitLogin]
                 │
                 ▼
 [3. ViewModel recibe el evento y valida campos]
                 │
                 ▼
 [4. ViewModel ejecuta LoginUseCase(correo, pass)]
                 │
                 ▼
 [5. AuthRepositoryImpl llama a ApiService.login(reqDto)]
                 │
                 ▼
 [6. OkHttp + AuthInterceptor añaden cabeceras y disparan HTTP al Backend]
                 │
                 ▼
 [7. Backend responde JSON envuelto en ResponseDto<LoginResDto>]
                 │
                 ▼
 [8. Kotlinx Serialization parsea el JSON a LoginResDto]
                 │
                 ▼
 [9. Mapper convierte LoginResDto -> User (Modelo de Dominio limpio)]
                 │
                 ▼
 [10. Repository guarda Token en SessionManager (DataStore) y retorna Result.success(User)]
                 │
                 ▼
 [11. ViewModel actualiza _uiState.update { it.copy(isLoading = false, isSuccess = true) }]
                 │
                 ▼
 [12. Compose detecta el cambio de StateFlow y re-dibuja la pantalla (Recomposición)]
```

---

## 6. Conceptos Clave Explicados Fácil

### ¿Qué es un DTO vs un Modelo de Dominio?

- **DTO (Data Transfer Object):** Es el paquete que llega por encomienda desde el servidor. Si el servidor usa nombres en inglés o en formato `snake_case` (como `tarifa_hora` o `id_usuario`), el DTO debe tenerlos exactamente igual con `@SerialName`. Los DTOs pueden tener campos nulos o formatos crudos.
  
  ```kotlin
  // data/remote/dto/paseador/PaseadorDto.kt
  @Serializable
  data class PaseadorDto(
      @SerialName("idpaseador") val idPaseador: Int? = null,
      @SerialName("tarifa_hora") val tarifaHora: Double = 0.0,
      @SerialName("nombre") val nombre: String = ""
  )
  ```

- **Modelo de Dominio (`domain/model`):** Es la clase limpia que usará tu aplicación. Aquí los nombres son amigables para Kotlin y los datos son seguros (evitamos nulos innecesarios):
  
  ```kotlin
  // domain/model/Paseador.kt
  data class Paseador(
      val idPaseador: Int,
      val tarifaHora: Double,
      val nombre: String
  )
  ```

### ¿Qué es un Mapper y por qué lo usamos?

Un **Mapper** es un traductor. Toma un DTO sucio que vino de la red y lo transforma en un objeto de dominio listo para usar:

```kotlin
// data/repository/PaseadorRepositoryImpl.kt
private fun PaseadorDto.toDomain(): Paseador {
    return Paseador(
        idPaseador = this.idPaseador ?: 0,
        tarifaHora = this.tarifaHora,
        nombre = this.nombre
    )
}
```

Si el backend decide cambiar `"idpaseador"` por `"paseador_id"`, **solo cambias el DTO y el Mapper**. ¡Ninguna de tus 10 pantallas que usan `Paseador` se enterará del cambio!

---

### Estrategia de Catálogos (Zero Lookup Overhead)

En una app tradicional mal diseñada, para llenar un formulario de registro se hace una petición para obtener distritos, otra para tipos de documento, otra para géneros, etc. Eso satura la red móvil.

En PaseaPe aplicamos la regla de **Cero Overhead de Búsqueda**:

1. **Un solo viaje:** Al entrar a la app, consultamos `GET /application-parameters`.
2. El backend responde una sola bolsa con todo:
   - `tipos_usuario`, `tipos_documento`, `distritos`, `tipos_mascota`, `generos_mascota`, `tamanos_mascota`, `niveles_energia`.
3. **Regla de oro para la UI:** El componente `AppDropdownMenu` muestra en pantalla el texto legible (`descripcion`), pero cuando el usuario hace clic, captura únicamente el identificador numérico (`id`).
4. **Al enviar datos al backend:** El payload que viaja en el JSON contiene únicamente los IDs numéricos (por ejemplo `distrito_id = 5`). Jamás enviamos texto libre para campos de catálogo.
5. **Filtrado dinámico de razas:** Cuando el usuario elige el tipo de mascota (ej. Perro = 1), llamamos a `GET /application-parameters/razas/1` para cargar en tiempo real las razas correspondientes a esa especie.

---

### Navegación Type-Safe con Compose

En versiones antiguas de Jetpack Navigation se usaban rutas de texto como `"detalle_paseador/" + id`. Si te equivocabas en una letra, la app se caía en tiempo de ejecución.

En PaseaPe usamos **Navigation Type-Safe**:

```kotlin
// presentation/navigation/ScreenRoute.kt
sealed interface ScreenRoute {
    @Serializable data object SplashRoute : ScreenRoute
    @Serializable data object LoginRoute : ScreenRoute
    @Serializable data object DashboardRoute : ScreenRoute
    @Serializable data object PaseadoresRoute : ScreenRoute
    @Serializable data object GoogleRegisterRoute : ScreenRoute
}
```

Para navegar a otra pantalla simplemente ejecutas:
```kotlin
navController.navigate(ScreenRoute.PaseadoresRoute)
```
El compilador de Kotlin verifica que la ruta exista. ¡Cero sorpresas!

---

### Inyección de Dependencias con Hilt

¿Alguna vez viste código donde una clase crea manualmente otra, y esa otra crea tres más?
`val repo = AuthRepository(ApiService(OkHttpClient(LoggingInterceptor())))` ❌ Esto es un dolor de cabeza para mantener y testear.

Con **Hilt**, le decimos a Android cómo fabricar cada cosa en módulos (`core/di/NetworkModule.kt`, `RepositoryModule.kt`, `DataStoreModule.kt`), y cuando una clase lo necesita, solo pone `@Inject`:

```kotlin
@HiltViewModel
class PaseadoresViewModel @Inject constructor(
    private val getPaseadoresUseCase: GetPaseadoresUseCase
) : ViewModel()
```

Hilt se encarga de crear el Caso de Uso, el Repositorio, el Retrofit y la red automáticamente tras bambalinas.

---

## 7. Receta de Cocina: ¿Cómo añadir una nueva pantalla y endpoint?

Imagina que el backend agrega un nuevo endpoint para listar las **reseñas de un paseador**:  
`GET /paseadores/{id}/resenas`

Sigue estos 10 pasos en orden y tu funcionalidad quedará perfectamente integrada:

### Paso 1: Crear el DTO (`data/remote/dto/`)
Crea el archivo `ResenaDto.kt` dentro de `data/remote/dto/paseador/`:
```kotlin
package com.example.paseape.data.remote.dto.paseador

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResenaDto(
    @SerialName("id_resena") val idResena: Int? = null,
    @SerialName("calificacion") val calificacion: Double = 5.0,
    @SerialName("comentario") val comentario: String? = null,
    @SerialName("autor_nombre") val autorNombre: String = ""
)
```

### Paso 2: Registrar el Endpoint en `ApiService.kt` (`data/remote/api/`)
Añade la función suspendida al contrato Retrofit:
```kotlin
@GET("paseadores/{id}/resenas")
suspend fun getResenasPaseador(
    @Path("id") paseadorId: Int
): ResponseDto<List<ResenaDto>>
```

### Paso 3: Crear el Modelo de Dominio (`domain/model/`)
Crea el archivo `Resena.kt` en `domain/model/`:
```kotlin
package com.example.paseape.domain.model

data class Resena(
    val id: Int,
    val calificacion: Double,
    val comentario: String,
    val autor: String
)
```

### Paso 4: Crear el Mapper (`data/remote/mapper/`)
Crea la extensión para transformar el DTO a Dominio:
```kotlin
fun ResenaDto.toDomain(): Resena = Resena(
    id = this.idResena ?: 0,
    calificacion = this.calificacion,
    comentario = this.comentario.orEmpty(),
    autor = this.autorNombre
)
```

### Paso 5: Declarar el método en la Interfaz del Repositorio (`domain/repository/`)
En `PaseadorRepository.kt`:
```kotlin
interface PaseadorRepository {
    suspend fun getPaseadores(): Result<List<Paseador>>
    suspend fun getResenas(paseadorId: Int): Result<List<Resena>> // <-- NUEVO
}
```

### Paso 6: Implementar el Repositorio (`data/repository/`)
En `PaseadorRepositoryImpl.kt`:
```kotlin
override suspend fun getResenas(paseadorId: Int): Result<List<Resena>> {
    return runCatching {
        val response = apiService.getResenasPaseador(paseadorId)
        val dtoList = response.response.orEmpty()
        dtoList.map { it.toDomain() }
    }
}
```

### Paso 7: Crear el Caso de Uso (`domain/usecase/`)
Crea `GetResenasUseCase.kt` en `domain/usecase/paseador/`:
```kotlin
package com.example.paseape.domain.usecase.paseador

import com.example.paseape.domain.model.Resena
import com.example.paseape.domain.repository.PaseadorRepository
import javax.inject.Inject

class GetResenasUseCase @Inject constructor(
    private val paseadorRepository: PaseadorRepository
) {
    suspend operator fun invoke(paseadorId: Int): Result<List<Resena>> {
        return paseadorRepository.getResenas(paseadorId)
    }
}
```

### Paso 8: Crear el Paquete de la Pantalla en Presentation (`presentation/screens/resenas/`)
Crea 4 archivos en `presentation/screens/resenas/`:

1. **`ResenasUiState.kt`**:
   ```kotlin
   data class ResenasUiState(
       val resenas: List<Resena> = emptyList(),
       val isLoading: Boolean = false,
       val errorMessage: String? = null
   )
   ```

2. **`ResenasUiEvent.kt`**:
   ```kotlin
   sealed interface ResenasUiEvent {
       data object Refresh : ResenasUiEvent
   }
   ```

3. **`ResenasViewModel.kt`**:
   ```kotlin
   @HiltViewModel
   class ResenasViewModel @Inject constructor(
       private val getResenasUseCase: GetResenasUseCase
   ) : ViewModel() {
       private val _uiState = MutableStateFlow(ResenasUiState())
       val uiState: StateFlow<ResenasUiState> = _uiState.asStateFlow()

       fun loadResenas(paseadorId: Int) {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true) }
               getResenasUseCase(paseadorId)
                   .onSuccess { list -> _uiState.update { it.copy(resenas = list, isLoading = false) } }
                   .onFailure { err -> _uiState.update { it.copy(errorMessage = err.message, isLoading = false) } }
           }
       }
   }
   ```

4. **`ResenasScreen.kt`**:
   Composición pura con `collectAsStateWithLifecycle()`:
   ```kotlin
   @Composable
   fun ResenasScreen(
       paseadorId: Int,
       viewModel: ResenasViewModel = hiltViewModel()
   ) {
       val uiState by viewModel.uiState.collectAsStateWithLifecycle()
       // Dibuja la lista o el spinner de carga
   }
   ```

### Paso 9: Registrar la Ruta y Añadirla al `NavHost`
En `presentation/navigation/ScreenRoute.kt`:
```kotlin
@Serializable
data class ResenasRoute(val paseadorId: Int) : ScreenRoute
```

Y en `presentation/navigation/AppNavHost.kt`:
```kotlin
composable<ScreenRoute.ResenasRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<ScreenRoute.ResenasRoute>()
    ResenasScreen(paseadorId = route.paseadorId)
}
```

### Paso 10: Añadir los Textos a `strings.xml`
¡No escribas texto directo en el código! Agrega tus etiquetas a `res/values/strings.xml`:
```xml
<string name="reviews_title">Reseñas del Paseador</string>
<string name="reviews_empty">Aún no hay reseñas registradas.</string>
```

---

## 8. Configuración Local y Primeros Pasos

### 1. Requisitos Previos
- **Android Studio:** Ladybug (2024.2+) o Koala.
- **JDK:** Java 17 o superior configurado en Gradle.
- **Dispositivo o Emulador:** Android 8.0 (API 26) o superior.

### 2. Archivo `local.properties`
El proyecto utiliza variables inyectadas mediante `local.properties` para no quemar URLs privadas en el control de versiones. 

Crea un archivo llamado `local.properties` en la raíz del proyecto (basándote en `local.properties.example`):

```properties
## Ruta a tu SDK de Android (generado automáticamente por Android Studio)
sdk.dir=/Users/tu-usuario/Library/Android/sdk

## URL base del Backend api-paseape
# Si usas el emulador oficial de Android (10.0.2.2 apunta a tu PC localhost):
PASEAPE_BASE_URL="http://10.0.2.2:7001/api-paseape/"

# Si pruebas en un celular físico conectado a tu misma red Wi-Fi:
# PASEAPE_BASE_URL="http://192.168.1.15:7001/api-paseape/"

## Credencial Web Client ID para Google Identity Services
GOOGLE_WEB_CLIENT_ID="tu_cliente_id.apps.googleusercontent.com"
```

### 3. Compilación y Ejecución
1. Abre el proyecto en Android Studio.
2. Espera a que Gradle termine la sincronización (`Sync Project with Gradle Files`).
3. Elige un dispositivo emulador o conecta tu celular físico con depuración USB.
4. Presiona el botón verde **Run 'app'** (`Shift + F10`).

---

## 9. Reglas de Oro del Proyecto

Para mantener el código impecable y profesional, todos los desarrolladores del equipo respetamos estas 5 reglas:

1. **CERO Hardcoded Strings:** Jamás escribas `"Bienvenido"` directo en un Composable. Usa siempre `stringResource(id = R.string.welcome)`.
2. **UDF Estricto:** La pantalla solo emite eventos (`UiEvent`) y lee estados (`UiState`). La UI nunca llama a un repositorio directamente.
3. **Manejo de los 4 Estados de Pantalla:** Toda pantalla que cargue datos remotos debe contemplar visualmente:
   - **Carga:** Un `CircularProgressIndicator`.
   - **Éxito:** Los datos mostrados en tarjetas (`LazyColumn`, `Card`).
   - **Vacío:** Mensaje claro si la lista vino sin elementos.
   - **Error:** Un mensaje descriptivo con botón de reintento.
4. **Respetar la envoltura `ResponseDto<T>`:** Todas las llamadas al backend devuelven `status_code`, `code`, `message`, `header` y `response`. Verifica que `code == "00"` para confirmar que la operación fue exitosa.
5. **No romper la barrera de capas:** La capa `domain` nunca debe conocer a Retrofit, Room ni Compose.

---

## 10. Glosario Rápido para Principiantes

- **Composable (`@Composable`):** Función de Kotlin que dibuja una parte de la interfaz gráfica.
- **StateFlow:** Un contenedor reactivo que guarda un dato y notifica a las pantallas cada vez que ese dato cambia.
- **UDF (Unidirectional Data Flow):** Patrón donde el estado baja del ViewModel a la Vista, y los eventos suben de la Vista al ViewModel.
- **Interceptor (OkHttp):** Un policía de aduanas que revisa cada petición HTTP antes de salir del teléfono para pegarle el Token de autenticación (`Authorization: Bearer ...`).
- **DataStore:** El reemplazo moderno de `SharedPreferences`. Permite guardar datos clave-valor de forma asíncrona y segura.
- **Hilt:** La herramienta de Google que entrega las piezas del rompecabezas a cada clase sin que tengas que armarlas a mano.
- **Type-Safe Navigation:** Sistema que garantiza que solo navegues a pantallas que existen y con los parámetros del tipo correcto.

---

*Desarrollado con dedicación para el equipo de **PaseaPe Móvil**. ¡Feliz codificación! 🐕🚶*