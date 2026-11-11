# Etapa 1 — Propuesta de aplicación

**Trabajo práctico:** Desarrollo de Aplicación Android Nativa
**Aplicación:** MobileStore · **Plataforma:** Android nativo (Kotlin + Jetpack Compose)
**Objetivo de este documento:** validar el alcance antes de implementar.

> La versión entregable de este documento es `Propuesta_MobileStore.docx`, en esta misma carpeta.

---

## 1. La idea

Un comercio chico que vende por redes sociales lleva su catálogo en planillas y capturas de pantalla. Eso trae tres problemas concretos: consultar un precio en medio de una venta es lento, los datos se cargan a mano y sin ningún control (aparecen precios en cero, nombres repetidos y descripciones vacías), y el stock se descuenta de memoria.

MobileStore reemplaza esa planilla por un catálogo en el teléfono: se consulta con búsqueda, se carga con validaciones que impiden guardar datos inválidos, y permite armar un pedido y confirmar la venta, que descuenta el stock y emite una notificación del sistema.

**Usuario objetivo.** El dueño o encargado del comercio, que carga y actualiza el catálogo, y quien atiende el mostrador, que consulta precios y stock con el teléfono en la mano.

---

## 2. Alcance: la visión completa y lo que entra en este TP

| Módulo | Qué haría | ¿Entra en el TP? |
|---|---|---|
| Catálogo de productos | Listado, búsqueda, alta, edición y eliminación | **Sí** — es el núcleo del TP |
| Carrito y venta | Armar un pedido, calcular el total y confirmar la venta | **Sí** — con descuento de stock y notificación |
| Control de stock | Cantidad por producto y aviso de reposición | **Sí, parcial** — se descuenta al vender y se marca el stock bajo en la lista |
| Clientes | Ficha de cliente y cuenta corriente | No |
| Reportes | Ventas por período, productos más vendidos | No |
| Códigos de barras | Alta y búsqueda por escaneo con la cámara | No |
| Multi-sucursal | Varios locales sincronizados contra un backend propio | No |

El TP se reduce a **cuatro pantallas**: listado, detalle, formulario de alta/edición y carrito. Es el recorte que permite ejercitar todos los puntos técnicos del enunciado —navegación, lista, formulario con validaciones, persistencia local y remota, estados de interfaz, notificación e integraciones— cerrando el ciclo del negocio (consultar, cargar, vender) sin convertirse en un proyecto final.

---

## 3. Historias de usuario

**HU-1 · Consultar el catálogo.** Como vendedor quiero ver el listado de productos con precio y stock, y buscar por nombre o categoría, para responder en segundos cuando un cliente pregunta por un artículo.

- El listado muestra imagen, nombre, precio y stock.
- La búsqueda filtra por nombre o categoría mientras se escribe.
- Se distingue «catálogo vacío» de «la búsqueda no arrojó resultados».
- Mientras se descarga el catálogo se muestra un indicador de carga.

**HU-2 · Dar de alta o editar un producto con validaciones.** Como vendedor quiero cargar un producto indicando nombre, precio, categoría, descripción, stock e imagen, y que la aplicación no me deje guardar datos inválidos, para no ensuciar el catálogo.

- Nombre obligatorio, de 3 a 80 caracteres, sin repetir el de otro producto.
- Precio obligatorio, numérico, mayor a cero, con hasta dos decimales.
- Categoría de una lista; descripción de 10 a 500 caracteres con contador; stock entero no negativo.
- Los errores se muestran junto al campo y, al intentar guardar, el foco salta al primero con problema.

**HU-3 · Ver y eliminar un producto.** Como vendedor quiero abrir el detalle de un producto y poder eliminarlo con confirmación previa, para mantener el catálogo actualizado sin borrar algo por accidente.

**HU-4 · Armar un pedido y registrar la venta.** Como vendedor quiero agregar productos a un carrito, ajustar cantidades y confirmar la venta, para registrar lo vendido y que el stock quede descontado solo.

- Se agregan productos al carrito desde el listado y desde el detalle.
- El carrito permite sumar, restar y quitar unidades, y muestra el total; no deja vender más de lo que hay en stock.
- Al confirmar se descuenta el stock de cada producto y el sistema emite una notificación.

**HU-5 · Compartir un producto.** Como vendedor quiero enviar la ficha de un producto por otra aplicación, para pasársela a un cliente sin sacar capturas de pantalla.

---

## 4. Flujo de navegación

```
                    ┌───────────────────────────┐
                    │         CATÁLOGO          │ ← pantalla inicial
                    │  lista + búsqueda + 🛒(2) │
                    │  carga / vacío / error    │
                    └──┬─────────┬──────────┬───┘
       tocar un ítem   │ botón + │   ícono  │ del carrito
                       ▼         ▼          ▼
        ┌──────────────────┐ ┌──────────┐ ┌──────────────────┐
        │     DETALLE      │ │FORMULARIO│ │     CARRITO      │
        │  ficha completa  │ │  alta /  │ │  líneas + total  │
        │ [Agregar al 🛒]  │ │ edición  │ │  [Confirmar]     │
        │ [Compartir]      │ │          │ └────────┬─────────┘
        │ [Editar] ────────┼▶│[Guardar] │          ▼
        │ [Eliminar]       │ └──────────┘  descuenta stock
        └────────┬─────────┘               + notificación
                 ▼
          confirmación
          antes de borrar
```

Los wireframes de cada pantalla están en `Propuesta_MobileStore.docx`.

---

## 5. Cómo se cubre cada punto del enunciado

| Punto | Qué se construye |
|---|---|
| 3.1 · Navegación e interfaces | Cuatro pantallas con Navigation Compose sobre una única `Activity`. Listado con `LazyColumn`, selección de ítem y estado de lista vacía. Argumentos tipados (`NavType.LongType`). |
| 3.1 · Formulario con validaciones | Alta y edición con seis campos validados: nombre, precio, categoría, descripción, stock e imagen. |
| 3.1 · Eliminación con confirmación | Diálogo previo, con opción de deshacer. |
| 3.2 · Gestión de datos | Room (SQLite) como fuente única de verdad, con el carrito resuelto por `JOIN` y el checkout dentro de una transacción + Retrofit/Moshi para leer el catálogo de `fakestoreapi.com`. |
| 3.3 · Estados y feedback | Un `UiState` inmutable por pantalla: carga, contenido, lista vacía, sin resultados, error de validación, error de red y confirmación. |
| 3.4 · Arquitectura y asincronía | MVVM con `ViewModel` + `StateFlow` + corrutinas; el estado sobrevive a la rotación. |
| 3.5 · Notificación del sistema | `NotificationCompat`: compra confirmada al hacer el checkout. El permiso se pide en ese momento. |
| 3.5 · Extras (mínimo 2) | **(1)** Compartir con `Intent.ACTION_SEND`. **(2)** Selección de fotografías con el Photo Picker. |
| 3.6 · Consideraciones generales | Textos en `strings.xml`, tema Material 3 con paleta y espaciados propios, solo dos permisos declarados. |

---

## 6. Para validar

Este es el alcance que propongo. Si todavía es mucho, el carrito es lo primero que se puede sacar: quedarían el listado, el detalle y el formulario, y la notificación pasaría a ser un aviso de stock bajo al guardar. Si es poco, sumo lo que haga falta antes de empezar a programar.
