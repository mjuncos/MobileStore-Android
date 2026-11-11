package com.mobilestore.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mobilestore.R
import com.mobilestore.core.Formatters
import com.mobilestore.core.ShareHelper
import com.mobilestore.ui.common.ErrorState
import com.mobilestore.ui.common.LoadingState
import com.mobilestore.ui.common.SnackbarController
import com.mobilestore.ui.theme.Dimens

/**
 * Pantalla 2: detalle del producto seleccionado, desde donde se edita, se
 * comparte y se elimina con confirmacion previa (punto 3.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    snackbar: SnackbarController,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.Message -> snackbar.show(event.text.asString(context))

                is DetailEvent.Deleted -> {
                    // El mensaje lo muestra el controlador compartido, asi que
                    // sobrevive a la vuelta al catalogo.
                    snackbar.showWithAction(
                        message = event.text.asString(context),
                        actionLabel = undoLabel,
                        onAction = viewModel::undoDelete
                    )
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_detail)) },
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
                    val product = state.product
                    if (product != null) {
                        // Extra 1: compartir mediante otras aplicaciones.
                        IconButton(onClick = { ShareHelper.shareProduct(context, product) }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.detail_share)
                            )
                        }
                        IconButton(onClick = viewModel::requestDelete) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.detail_delete)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar.hostState) }
    ) { padding ->
        val product = state.product
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            product == null -> ErrorState(
                message = stringResource(R.string.detail_not_found),
                modifier = Modifier.padding(padding),
                onRetry = onBack
            )

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.m),
                    verticalArrangement = Arrangement.spacedBy(Dimens.m)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = stringResource(
                            R.string.cd_product_image,
                            product.title
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.heroImage)
                            .clip(RoundedCornerShape(Dimens.corner))
                    )

                    Text(text = product.title, style = MaterialTheme.typography.headlineSmall)

                    Text(
                        text = Formatters.money(product.price),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(onClick = {}, label = { Text(product.category) })
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    when {
                                        !product.isAvailable ->
                                            stringResource(R.string.catalog_out_of_stock)
                                        product.isLowStock ->
                                            stringResource(R.string.catalog_low_stock, product.stock)
                                        else ->
                                            stringResource(R.string.catalog_stock_label, product.stock)
                                    }
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = when {
                                    !product.isAvailable -> MaterialTheme.colorScheme.error
                                    product.isLowStock -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                    }

                    Text(
                        text = stringResource(R.string.detail_description),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = viewModel::addToCart,
                        enabled = product.isAvailable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                        Text(
                            text = stringResource(R.string.detail_add_to_cart),
                            modifier = Modifier.padding(start = Dimens.s)
                        )
                    }

                    OutlinedButton(
                        onClick = { onEdit(product.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Text(
                            text = stringResource(R.string.detail_edit),
                            modifier = Modifier.padding(start = Dimens.s)
                        )
                    }

                    OutlinedButton(
                        onClick = { ShareHelper.shareProduct(context, product) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text(
                            text = stringResource(R.string.detail_share),
                            modifier = Modifier.padding(start = Dimens.s)
                        )
                    }

                    Spacer(Modifier.height(Dimens.l))
                }

                // Eliminacion con confirmacion previa (punto 3.1).
                if (state.showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = viewModel::cancelDelete,
                        title = { Text(stringResource(R.string.delete_dialog_title)) },
                        text = {
                            Text(stringResource(R.string.delete_dialog_message, product.title))
                        },
                        confirmButton = {
                            TextButton(onClick = viewModel::confirmDelete) {
                                Text(
                                    text = stringResource(R.string.delete_dialog_confirm),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::cancelDelete) {
                                Text(stringResource(R.string.delete_dialog_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}
