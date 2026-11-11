package com.mobilestore.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilestore.R
import com.mobilestore.ui.common.EmptyState
import com.mobilestore.ui.common.ErrorState
import com.mobilestore.ui.common.LoadingState
import com.mobilestore.ui.common.SnackbarController
import com.mobilestore.ui.theme.Dimens

/**
 * Pantalla 1: listado del catalogo con LazyColumn, busqueda y seleccion de
 * item (punto 3.1). Contempla los estados de carga, contenido, lista vacia,
 * busqueda sin resultados y error (punto 3.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    snackbar: SnackbarController,
    cartItemCount: Int,
    onProductClick: (Long) -> Unit,
    onCreateProduct: () -> Unit,
    onOpenCart: () -> Unit
) {
    // collectAsStateWithLifecycle: deja de recolectar cuando la pantalla no
    // esta visible y conserva el estado ante la rotacion.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.show(message.asString(context))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_catalog)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.catalog_refresh)
                        )
                    }
                    IconButton(onClick = onOpenCart) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) Badge { Text(cartItemCount.toString()) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = stringResource(R.string.screen_cart)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProduct) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.catalog_add_product)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar.hostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                label = { Text(stringResource(R.string.catalog_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.m, vertical = Dimens.s)
            )

            when {
                // Estado: carga de informacion
                state.isLoading -> LoadingState()

                // Estado: error de red o persistencia (sin datos que mostrar)
                state.errorMessage != null && state.products.isEmpty() -> ErrorState(
                    message = state.errorMessage!!.asString(context),
                    onRetry = {
                        viewModel.dismissError()
                        viewModel.refresh()
                    }
                )

                // Estado: la busqueda no arrojo resultados
                state.isEmptyBecauseOfSearch -> EmptyState(
                    title = stringResource(R.string.catalog_empty_search_title),
                    message = stringResource(R.string.catalog_empty_search_message, state.query),
                    actionLabel = stringResource(R.string.cd_clear_search),
                    onAction = viewModel::clearQuery
                )

                // Estado: lista vacia
                state.isEmpty -> EmptyState(
                    title = stringResource(R.string.catalog_empty_title),
                    message = stringResource(R.string.catalog_empty_message),
                    actionLabel = stringResource(R.string.catalog_refresh),
                    onAction = { viewModel.refresh() }
                )

                // Estado: contenido disponible
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.m,
                        end = Dimens.m,
                        top = Dimens.s,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.s)
                ) {
                    items(
                        items = state.products,
                        key = { it.id }
                    ) { product ->
                        ProductListItem(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }
    }
}
