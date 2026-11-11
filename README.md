# MobileStore — Aplicación Android Nativa

Trabajo práctico: *Desarrollo de Aplicación Android Nativa*.
Catálogo y venta para un comercio chico, en **Kotlin + Jetpack Compose**, con arquitectura **MVVM**, persistencia local en **Room/SQLite** y consumo de una **API REST** con Retrofit.

> **Sobre el alcance.** La idea de producto es más amplia que lo que se implementa acá (ver §2). Para este TP el alcance se acotó a **cuatro pantallas** — catálogo, detalle, formulario de alta/edición y carrito — que cubren el ciclo completo del negocio sin convertirlo en un proyecto final.

---

## 1. Descripción del caso de negocio

**Problema.** Un comercio chico que vende por redes sociales lleva su catálogo en planillas y capturas de pantalla. Consultar un precio en medio de una venta es lento, los datos se cargan a mano y sin ningún control (aparecen precios en cero, nombres repetidos y descripciones vacías), y el stock se descuenta de memoria.

**Solución.** MobileStore reemplaza esa planilla por un catálogo en el teléfono:

- consulta del catálogo con búsqueda por nombre o categoría;
- alta, edición y eliminación de productos, con validaciones por campo y confirmación antes de borrar;
- carrito para armar el pedido y **confirmar la venta, que descuenta el stock** de cada producto y emite una notificación del sistema;
- aviso visual en el listado cuando a un producto le quedan pocas unidades;
- ficha de producto compartible por WhatsApp, mail o cualquier otra app;
- foto del producto tomada de la galería del dispositivo.

**Usuarios objetivo.** El dueño o encargado del comercio (carga y actualiza el catálogo) y quien atiende el mostrador (consulta precios, arma pedidos y registra ventas).

### Historias de usuario principales

| # | Como… | quiero… | para… |
|---|---|---|---|
| 1 | vendedor | ver el listado de productos con precio y stock, y buscar por nombre o categoría | responder rápido cuando un cliente pregunta por un artículo |
| 2 | vendedor | dar de alta un producto con nombre, precio, categoría, descripción, stock e imagen, con validaciones | evitar cargar precios en cero, nombres repetidos o descripciones vacías |
| 3 | vendedor | abrir el detalle de un producto, editarlo o eliminarlo con confirmación previa | mantener el catálogo actualizado sin borrar algo por accidente |
| 4 | vendedor | armar un carrito y confirmar la venta | registrar lo vendido y que el stock quede descontado solo |
| 5 | vendedor | compartir la ficha de un producto por otra aplicación | pasársela a un cliente sin sacar captura de pantalla |

---

## 2. Alcance: visión completa vs. este TP

La aplicación completa incluiría clientes con cuenta corriente, reportes de ventas, lectura de códigos de barras, múltiples sucursales y sincronización en la nube.

**Para este TP se implementa el núcleo de catálogo y venta:**

| | Incluido en el TP | Fuera del TP (visión futura) |
|---|---|---|
| Pantallas | Catálogo, Detalle, Alta/Edición, Carrito | Clientes, Reportes, Configuración |
| Datos | Room local + lectura de API REST | Escritura remota, sincronización bidireccional, multi-sucursal |
| Ventas | Carrito, total y descuento de stock | Historial de ventas, cuenta corriente, facturación |
| Extras | Compartir, selección de fotos | Lector de códigos de barras, impresión de tickets |

---

## 3. Tecnologías y bibliotecas

| Área | Tecnología | Por qué |
|---|---|---|
| Build | AGP 8.4.0 · Gradle 8.7 · JDK 17 | Compatible con Android Studio Jellyfish |
| Lenguaje | Kotlin 1.9.24 | Lenguaje nativo de Android |
| Interfaz | Jetpack Compose + Material 3 | UI declarativa; estado y recomposición explícitos |
| Navegación | Navigation Compose 2.7.7 | Una sola `Activity`, argumentos tipados |
| Arquitectura | MVVM (`ViewModel` + `StateFlow`) | Separación interfaz / presentación / datos |
| Asincronía | Corrutinas + `Flow` | Operaciones de red y base fuera del hilo principal |
| Persistencia local | Room 2.6.1 (SQLite) | Alta, listado, consulta, edición y eliminación |
| Consumo remoto | Retrofit 2.11 + OkHttp + Moshi | Lectura del catálogo desde una API REST |
| Imágenes | Coil 2.7 | Carga asíncrona con caché |
| Tests | JUnit 4 | 24 casos sobre las validaciones del formulario |

API consumida: `https://fakestoreapi.com/products` (solo lectura).

---

## 4. Diagrama de arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI  (Compose)                          │
│   CatalogScreen · DetailScreen · ProductFormScreen · CartScreen │
│   AppNavigation (NavHost)  ·  SnackbarController compartido     │
└────────────────────────────┬────────────────────────────────────┘
                             │  eventos ▼        ▲ StateFlow<UiState>
┌────────────────────────────┴────────────────────────────────────┐
│                       PRESENTACIÓN (ViewModel)                  │
│  CatalogViewModel · DetailViewModel · ProductFormViewModel ·    │
│  CartViewModel                                                  │
│  · un único UiState inmutable por pantalla                      │
│  · sobreviven a la rotación (viewModelScope + stateIn)          │
└────────────────────────────┬────────────────────────────────────┘
                             │  suspend / Flow
┌────────────────────────────┴────────────────────────────────────┐
│                            DOMINIO                              │
│  Product · CartLine · CheckoutSummary                           │
│  ProductFormValidator (Kotlin puro, testeable con JUnit)        │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────────┐
│                             DATOS                               │
│   ProductRepository          CartRepository                     │
│         │                          │                            │
│         ├── local  ──▶ Room / SQLite  ◀── FUENTE ÚNICA DE VERDAD│
│         │              ProductDao · CartDao (JOIN)              │
│         └── remote ──▶ Retrofit → fakestoreapi.com (solo GET)   │
│                        alimenta a Room, nunca a la UI           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Decisiones técnicas relevantes

**Room como fuente única de verdad.** La interfaz nunca lee de la red: observa a Room, y la red solo escribe en Room. Esto evita que la pantalla quede en blanco cuando la API falla y mantiene las cuatro pantallas consistentes entre sí, porque todas observan las mismas tablas.

**El carrito guarda ids, no copias.** `cart_items` tiene solo `productId` y `quantity`; el nombre y el precio se leen del producto con un `JOIN` (`CartDao.observeLines()`). Así, si se edita el precio de un producto, el carrito lo refleja al instante en vez de quedar con una copia vieja. La clave foránea con `onDelete = CASCADE` hace que borrar un producto saque su línea del carrito automáticamente.

**El checkout es transaccional.** Confirmar la venta descuenta el stock de varios productos y vacía el carrito: son varias escrituras sobre dos tablas. Van dentro de `db.withTransaction { }` porque o se aplica toda la venta o no se aplica ninguna — si fallara a la mitad quedarían productos descontados y el carrito sin vaciar.

**La API es de solo lectura.** `fakestoreapi.com` es una API de prueba que acepta POST/PUT pero no persiste nada. En vez de simular una sincronización que no existe, el consumo remoto se limita a traer el catálogo y el alta/edición/eliminación son locales. Es una limitación del backend, no del diseño, y está documentada en `ProductApi.kt`.

**El validador no conoce Android.** `ProductFormValidator` es un `object` de Kotlin puro que devuelve un `ValidationError` del dominio, no un `String`. La traducción a `strings.xml` ocurre en `ui/form/ValidationMessages.kt`. Así se testea con JUnit sin emulador y no queda ni un texto hardcodeado.

**Cuándo se muestra un error de validación.** Cada campo se revalida en cada pulsación, pero el error solo se pinta cuando el campo perdió el foco (`touched`) o cuando ya hubo un intento de guardado (`submitAttempted`). Un formulario recién abierto no aparece todo en rojo. El botón *Guardar* queda habilitado a propósito: al tocarlo se revalida todo, se marcan los campos con problema y el foco salta al primero.

**`Snackbar` compartido a nivel `NavHost`.** Los mensajes de confirmación se disparan justo antes de navegar. Si el `SnackbarHostState` viviera en cada pantalla, la corrutina se cancelaría al destruirse el composable y el mensaje nunca se vería. Colgándolo del `NavHost` sobrevive al cambio de pantalla — por eso «producto eliminado» con su acción *Deshacer* se ve ya en el catálogo.

**La notificación la dispara la lógica de negocio.** Al confirmar la compra, `CartViewModel` emite un evento con los datos ya formateados y la pantalla llama a `NotificationHelper`. El ViewModel no conoce `NotificationCompat`. El permiso `POST_NOTIFICATIONS` se solicita en ese momento exacto, no al abrir la app, y la venta se registra igual si el usuario lo rechaza: la notificación es un extra, no un requisito para vender.

**No se puede vender más de lo que hay.** El botón de sumar unidades se deshabilita al llegar al stock disponible, y `CartRepository.add()` acota la cantidad. El aviso visual de stock bajo (`Product.isLowStock`) marca en color los productos con 3 unidades o menos.

**Imágenes copiadas al almacenamiento privado.** El Photo Picker entrega un permiso de lectura temporal que se pierde al reiniciar la app, así que `ImageStorage` copia el archivo elegido a `filesDir`. Usar el Photo Picker en vez de `READ_MEDIA_IMAGES` evita pedir un permiso de almacenamiento.

**Inyección de dependencias manual.** `ServiceLocator` en lugar de Hilt: con dos repositorios y cuatro ViewModels, Hilt agrega más configuración que valor.

**Stock derivado de `rating.count`.** La API pública no expone stock; se deriva de `rating.count` para poder ejercitar el campo en el formulario. Está documentado en `ProductDto.kt` para no confundirlo con un dato real del backend.

**Migración destructiva de la base.** `AppDatabase` está en `version = 2` con
`fallbackToDestructiveMigration()`. Si el esquema cambia, Room recrea la base en
lugar de migrarla: el catálogo se vuelve a descargar de la API y los productos
locales son datos de prueba, así que no hay nada que preservar. En una aplicación
real iría una `Migration` explícita de la versión 1 a la 2.

**Un refresco sin resultados es un error, no un éxito.** Si la API responde pero no
devuelve ningún producto utilizable, `refreshFromRemote()` devuelve
`AppError.EmptyRemote` en vez de `Resource.Success(0)`. La diferencia importa: con
`Success(0)` la pantalla mostraba «todavía no hay productos» y ocultaba que la
llamada había fallado.

---

## 6. Cobertura de los requerimientos del enunciado

| Requisito | Dónde está |
|---|---|
| 3.1 · Tres o más pantallas navegables | Catálogo, Detalle, Formulario, Carrito (`AppNavigation.kt`) |
| 3.1 · Listado con `LazyColumn`, selección y estado vacío | `CatalogScreen.kt`, `ProductListItem.kt` |
| 3.1 · Formulario con validaciones | `ProductFormScreen.kt`, `ProductFormViewModel.kt`, `ProductFormValidator.kt` |
| 3.1 · Detalle y edición | `DetailScreen.kt` → `ProductFormScreen.kt` en modo edición |
| 3.1 · Eliminación con confirmación | `AlertDialog` en `DetailScreen.kt`, con opción de deshacer |
| 3.1 · Paso seguro de argumentos | `Routes.kt` con `NavType.LongType` |
| 3.2 · Persistencia local (SQLite/Room) | `AppDatabase`, `ProductDao`, `CartDao`, transacción en el checkout |
| 3.2 · Consumo remoto (API REST) | `ProductApi`, `RetrofitClient`, Moshi |
| 3.3 · Carga, contenido, lista vacía, error de validación, error de red, confirmación | `StateViews.kt`, `CatalogUiState`, `CartUiState`, `SnackbarController` |
| 3.4 · MVVM, `ViewModel`, `StateFlow`, corrutinas, rotación | `ui/*/...ViewModel.kt` con `stateIn` + `viewModelScope` |
| 3.5 · Notificación con `NotificationCompat` | `NotificationHelper.kt` — compra confirmada, disparada desde el checkout |
| 3.5 · Extras (mínimo 2) | **(1)** compartir con `Intent.ACTION_SEND` (`ShareHelper.kt`); **(2)** selección de fotografías con Photo Picker (`ImageStorage.kt`) |
| 3.6 · Textos en recursos | `res/values/strings.xml` — 96 strings, ningún literal en la interfaz |
| 3.6 · Estilos, colores y dimensiones | `ui/theme/Theme.kt`, `ui/theme/Dimens.kt`, `res/values/` |
| 3.6 · Permisos solo cuando son necesarios | Solo `INTERNET` y `POST_NOTIFICATIONS`; este último se pide al confirmar una compra |

---

## 7. Cómo ejecutarlo

Requisitos: **Android Studio Jellyfish (2023.3.1) o posterior**, JDK 17, SDK de Android 34.

```bash
git clone <url-del-repo>
cd MobileStore
./gradlew assembleDebug          # compilar
./gradlew test                   # 24 tests de validación del formulario
./gradlew installDebug           # instalar en un dispositivo o emulador
```

O abrir la carpeta desde Android Studio (*Open*) y ejecutar la configuración `app`.

Al primer arranque, si hay conexión, la app descarga el catálogo de `fakestoreapi.com` y lo guarda en SQLite. Sin conexión arranca con lo que ya tenga guardado y se pueden cargar productos a mano.

**Para probar la notificación:** agregá productos al carrito, abrí el carrito y tocá *Confirmar compra*. En Android 13+ pide el permiso de notificaciones en ese momento. Al volver al catálogo vas a ver el stock descontado.

---

## 8. Estructura del proyecto

```
app/src/main/java/com/mobilestore/
├── MainActivity.kt              Única Activity; monta el NavHost
├── MobileStoreApp.kt            Application; crea el canal de notificación
├── core/                        Utilidades transversales
│   ├── Resource.kt              Éxito / error tipado del dominio
│   ├── UiText.kt                Texto diferido a strings.xml
│   ├── ErrorMapper.kt           AppError → texto de recursos
│   ├── Formatters.kt            Formato de moneda
│   ├── NotificationHelper.kt    NotificationCompat (compra confirmada)
│   ├── ShareHelper.kt           Intent ACTION_SEND
│   └── ImageStorage.kt          Copia local de la imagen elegida
├── data/
│   ├── local/                   Room: entidades, DAOs, base
│   ├── remote/                  Retrofit: ProductApi, ProductDto, cliente
│   ├── repository/              ProductRepository · CartRepository
│   └── ServiceLocator.kt        Inyección de dependencias manual
├── domain/
│   ├── model/                   Product · CartLine · CheckoutSummary
│   └── validation/              ProductFormValidator (Kotlin puro)
└── ui/
    ├── navigation/              NavHost y rutas
    ├── theme/                   Colores, tipografía, dimensiones
    ├── common/                  Estados de carga/vacío/error, Snackbar
    ├── catalog/  detail/  form/  cart/     Pantalla + ViewModel

app/src/test/java/com/mobilestore/domain/validation/
└── ProductFormValidatorTest.kt  24 casos de validación
```

