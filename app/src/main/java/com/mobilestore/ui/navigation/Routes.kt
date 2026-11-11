package com.mobilestore.ui.navigation

/**
 * Rutas de Navigation Compose. Los argumentos viajan tipados en la ruta
 * (punto 3.1: paso seguro de informacion entre pantallas) y se centralizan
 * aca para no armar strings a mano en el resto de la aplicacion.
 */
object Routes {

    const val CATALOG = "catalog"
    const val CART = "cart"

    const val PRODUCT_ID_ARG = "productId"

    const val DETAIL = "detail/{$PRODUCT_ID_ARG}"
    fun detail(productId: Long) = "detail/$productId"

    /** -1 significa "alta"; cualquier otro valor es una edicion. */
    const val FORM = "form?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"
    fun form(productId: Long? = null) = "form?$PRODUCT_ID_ARG=${productId ?: NO_ID}"

    const val NO_ID = -1L
}
