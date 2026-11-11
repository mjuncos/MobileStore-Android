package com.mobilestore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilestore.data.repository.CartRepository
import com.mobilestore.data.repository.ProductRepository
import com.mobilestore.ui.cart.CartScreen
import com.mobilestore.ui.cart.CartViewModel
import com.mobilestore.ui.catalog.CatalogScreen
import com.mobilestore.ui.catalog.CatalogViewModel
import com.mobilestore.ui.common.rememberSnackbarController
import com.mobilestore.ui.detail.DetailScreen
import com.mobilestore.ui.detail.DetailViewModel
import com.mobilestore.ui.form.ProductFormScreen
import com.mobilestore.ui.form.ProductFormViewModel
import kotlinx.coroutines.flow.map

/**
 * Las cuatro pantallas de la aplicacion sobre una unica Activity.
 */
@Composable
fun AppNavigation(
    productRepository: ProductRepository,
    cartRepository: CartRepository
) {
    val navController = rememberNavController()
    // Un unico controlador de Snackbar para toda la app (ver SnackbarController).
    val snackbar = rememberSnackbarController()

    // Contador del carrito que muestra la barra superior del catalogo.
    val cartCountFlow = remember(cartRepository) {
        cartRepository.observeLines().map { lines -> lines.sumOf { it.quantity } }
    }
    val cartItemCount by cartCountFlow.collectAsStateWithLifecycle(initialValue = 0)

    NavHost(
        navController = navController,
        startDestination = Routes.CATALOG
    ) {

        // ---------- Pantalla 1: listado ----------
        composable(Routes.CATALOG) {
            val viewModel: CatalogViewModel = viewModel(
                factory = CatalogViewModel.Factory(productRepository, cartRepository)
            )
            CatalogScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                cartItemCount = cartItemCount,
                onProductClick = { id -> navController.navigate(Routes.detail(id)) },
                onCreateProduct = { navController.navigate(Routes.form()) },
                onOpenCart = { navController.navigate(Routes.CART) }
            )
        }

        // ---------- Pantalla 2: detalle ----------
        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument(Routes.PRODUCT_ID_ARG) { type = NavType.LongType }
            )
        ) { entry ->
            val productId = entry.arguments?.getLong(Routes.PRODUCT_ID_ARG) ?: Routes.NO_ID
            val viewModel: DetailViewModel = viewModel(
                factory = DetailViewModel.Factory(productId, productRepository, cartRepository)
            )
            DetailScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.form(id)) }
            )
        }

        // ---------- Pantalla 3: alta / edicion ----------
        composable(
            route = Routes.FORM,
            arguments = listOf(
                navArgument(Routes.PRODUCT_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = Routes.NO_ID
                }
            )
        ) { entry ->
            val rawId = entry.arguments?.getLong(Routes.PRODUCT_ID_ARG) ?: Routes.NO_ID
            val productId = rawId.takeIf { it != Routes.NO_ID }
            val viewModel: ProductFormViewModel = viewModel(
                factory = ProductFormViewModel.Factory(productId, productRepository)
            )
            ProductFormScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                onSaved = { savedId ->
                    if (productId == null) {
                        // Tras un alta vamos directo al detalle del producto nuevo.
                        navController.navigate(Routes.detail(savedId)) {
                            popUpTo(Routes.CATALOG)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onClose = { navController.popBackStack() }
            )
        }

        // ---------- Pantalla 4: carrito ----------
        composable(Routes.CART) {
            val viewModel: CartViewModel = viewModel(
                factory = CartViewModel.Factory(cartRepository)
            )
            CartScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                onBack = { navController.popBackStack() },
                onBrowseCatalog = {
                    navController.popBackStack(Routes.CATALOG, inclusive = false)
                }
            )
        }
    }
}
