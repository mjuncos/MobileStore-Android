package com.mobilestore.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mobilestore.R
import com.mobilestore.core.Formatters
import com.mobilestore.domain.model.Product
import com.mobilestore.ui.theme.Dimens
import androidx.compose.material.icons.Icons

/**
 * Item del listado. Toda la fila es clickeable: es la seleccion de item que
 * pide el punto 3.1 y lleva al detalle.
 */
@Composable
fun ProductListItem(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.corner),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(Dimens.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = stringResource(R.string.cd_product_image, product.title),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(Dimens.thumb)
                    .clip(RoundedCornerShape(Dimens.s))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.m, end = Dimens.s),
                verticalArrangement = Arrangement.spacedBy(Dimens.xs)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.money(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        !product.isAvailable -> stringResource(R.string.catalog_out_of_stock)
                        product.isLowStock -> stringResource(R.string.catalog_low_stock, product.stock)
                        else -> stringResource(R.string.catalog_stock_label, product.stock)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        !product.isAvailable -> MaterialTheme.colorScheme.error
                        product.isLowStock -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Accion secundaria: sumar al carrito sin salir del listado.
            IconButton(
                onClick = onAddToCart,
                enabled = product.isAvailable
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddShoppingCart,
                    contentDescription = stringResource(R.string.detail_add_to_cart)
                )
            }
        }
    }
}
