package com.mobilestore.ui.cart

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mobilestore.R
import com.mobilestore.core.Formatters
import com.mobilestore.core.NotificationHelper
import com.mobilestore.domain.model.CartLine
import com.mobilestore.ui.common.EmptyState
import com.mobilestore.ui.common.LoadingState
import com.mobilestore.ui.common.SnackbarController
import com.mobilestore.ui.theme.Dimens

/**
 * Pantalla 4: carrito. Al confirmar la compra se descuenta el stock y se
 * emite la notificacion del sistema (punto 3.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    snackbar: SnackbarController,
    onBack: () -> Unit,
    onBrowseCatalog: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // El permiso de notificaciones se pide recien al confirmar la compra.
    // Se responda que si o que no, la venta se registra igual: la
    // notificacion es un extra, no un requisito para vender.
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkout()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.Message -> snackbar.show(event.text.asString(context))

                is CartEvent.CheckoutConfirmed -> {
                    NotificationHelper.showOrderConfirmed(
                        context = context,
                        itemCount = event.itemCount,
                        formattedTotal = event.formattedTotal
                    )
                    snackbar.show(
                        context.getString(R.string.cart_checkout_success, event.formattedTotal)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_cart)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    if (state.lines.isNotEmpty()) {
                        IconButton(onClick = viewModel::requestClear) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cart_clear)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.lines.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(Dimens.m)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.cart_total),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = state.formattedTotal,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                val needsPermission =
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationHelper.hasPermission(context)
                                if (needsPermission) {
                                    // La venta sigue en el callback del permiso.
                                    notificationPermission.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    viewModel.checkout()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimens.s)
                                .height(52.dp)
                        ) {
                            Icon(Icons.Outlined.ShoppingCartCheckout, contentDescription = null)
                            Text(
                                text = stringResource(R.string.cart_checkout),
                                modifier = Modifier.padding(start = Dimens.s)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar.hostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.isEmpty -> EmptyState(
                title = stringResource(R.string.cart_empty_title),
                message = stringResource(R.string.cart_empty_message),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.screen_catalog),
                onAction = onBrowseCatalog
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Dimens.m),
                verticalArrangement = Arrangement.spacedBy(Dimens.s)
            ) {
                items(items = state.lines, key = { it.product.id }) { line ->
                    CartLineItem(
                        line = line,
                        onIncrease = { viewModel.increase(line) },
                        onDecrease = { viewModel.decrease(line) },
                        onRemove = { viewModel.remove(line) }
                    )
                }
            }
        }

        if (state.showClearDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelClear,
                title = { Text(stringResource(R.string.cart_clear_dialog_title)) },
                text = { Text(stringResource(R.string.cart_clear_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmClear) {
                        Text(
                            text = stringResource(R.string.delete_dialog_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelClear) {
                        Text(stringResource(R.string.delete_dialog_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun CartLineItem(
    line: CartLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(Dimens.corner)) {
        Row(
            modifier = Modifier.padding(Dimens.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = line.product.imageUrl,
                contentDescription = stringResource(
                    R.string.cd_product_image,
                    line.product.title
                ),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(Dimens.s))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.s)
            ) {
                Text(
                    text = line.product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.money(line.subtotal),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cart_decrease)
                    )
                }
                Text(
                    text = line.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onIncrease,
                    // No se puede sumar mas alla del stock disponible.
                    enabled = line.quantity < line.product.stock
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cart_increase)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.cart_remove)
                    )
                }
            }
        }
    }
}
