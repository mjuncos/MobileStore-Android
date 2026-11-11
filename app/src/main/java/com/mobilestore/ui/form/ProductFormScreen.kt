package com.mobilestore.ui.form

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.mobilestore.core.ImageStorage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mobilestore.R
import com.mobilestore.domain.validation.FormField
import com.mobilestore.domain.validation.ProductFormValidator
import com.mobilestore.ui.common.LoadingState
import com.mobilestore.ui.common.SnackbarController
import com.mobilestore.ui.theme.Dimens

/**
 * Pantalla de alta / edicion (requisito 3.1).
 *
 * Cada campo:
 *  - se revalida en vivo mientras se escribe;
 *  - muestra el error recien cuando pierde el foco o al intentar guardar;
 *  - expone `isError` + `supportingText` para que el mensaje quede asociado
 *    al campo y sea leido por accesibilidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    viewModel: ProductFormViewModel,
    snackbar: SnackbarController,
    onSaved: (Long) -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val titleFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }
    val stockFocus = remember { FocusRequester() }

    // Extra: seleccion de fotografias con el Photo Picker del sistema.
    // No requiere permiso de almacenamiento en ninguna version de Android.
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // El permiso sobre la Uri del picker es temporal, asi que copiamos
            // la imagen al almacenamiento privado de la app.
            scope.launch {
                ImageStorage.persist(context, uri)?.let(viewModel::onImageChange)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FormEvent.Saved -> {
                    snackbar.show(event.message.asString(context))
                    onSaved(event.productId)
                }
                is FormEvent.Message -> snackbar.show(event.text.asString(context))
                is FormEvent.FocusField -> when (event.field) {
                    FormField.TITLE -> titleFocus.requestFocus()
                    FormField.PRICE -> priceFocus.requestFocus()
                    FormField.DESCRIPTION -> descriptionFocus.requestFocus()
                    FormField.STOCK -> stockFocus.requestFocus()
                    else -> Unit
                }
                FormEvent.Close -> onClose()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.screen_edit_product
                            else R.string.screen_new_product
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::onBackRequested) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar.hostState) }
    ) { padding ->

        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.m)
        ) {

            // ---------------- Nombre ----------------
            val titleError = state.visibleError(FormField.TITLE)
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.form_title_label)) },
                singleLine = true,
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(titleError.message())
                    } else {
                        Text(
                            stringResource(
                                R.string.form_char_counter,
                                state.title.length,
                                ProductFormValidator.TITLE_MAX
                            )
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused) viewModel.onFieldTouched(FormField.TITLE)
                    }
            )

            // ---------------- Precio y stock ----------------
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
                val priceError = state.visibleError(FormField.PRICE)
                OutlinedTextField(
                    value = state.price,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text(stringResource(R.string.form_price_label)) },
                    singleLine = true,
                    isError = priceError != null,
                    prefix = { Text(stringResource(R.string.form_price_prefix)) },
                    supportingText = { if (priceError != null) Text(priceError.message()) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(priceFocus)
                        .onFocusChanged { focus ->
                            if (!focus.isFocused) viewModel.onFieldTouched(FormField.PRICE)
                        }
                )

                val stockError = state.visibleError(FormField.STOCK)
                OutlinedTextField(
                    value = state.stock,
                    onValueChange = viewModel::onStockChange,
                    label = { Text(stringResource(R.string.form_stock_label)) },
                    singleLine = true,
                    isError = stockError != null,
                    supportingText = { if (stockError != null) Text(stockError.message()) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(stockFocus)
                        .onFocusChanged { focus ->
                            if (!focus.isFocused) viewModel.onFieldTouched(FormField.STOCK)
                        }
                )
            }

            // ---------------- Categoria ----------------
            var expanded by remember { mutableStateOf(false) }
            val categoryError = state.visibleError(FormField.CATEGORY)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.form_category_label)) },
                    placeholder = { Text(stringResource(R.string.form_category_select)) },
                    isError = categoryError != null,
                    supportingText = { if (categoryError != null) Text(categoryError.message()) },
                    trailingIcon = {
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // ---------------- Descripcion ----------------
            val descriptionError = state.visibleError(FormField.DESCRIPTION)
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.form_description_label)) },
                minLines = 4,
                isError = descriptionError != null,
                supportingText = {
                    if (descriptionError != null) {
                        Text(descriptionError.message())
                    } else {
                        Text(
                            stringResource(
                                R.string.form_char_counter,
                                state.description.length,
                                ProductFormValidator.DESCRIPTION_MAX
                            )
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocus)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused) viewModel.onFieldTouched(FormField.DESCRIPTION)
                    }
            )

            // ---------------- Imagen ----------------
            Text(
                text = stringResource(R.string.form_image_label),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.m)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.thumb)
                        .clip(RoundedCornerShape(Dimens.s)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imageUrl.isBlank()) {
                        Text(
                            text = stringResource(R.string.form_image_none),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        AsyncImage(
                            model = state.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.s)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Text(
                            text = stringResource(R.string.form_image_pick),
                            modifier = Modifier.padding(start = Dimens.s)
                        )
                    }
                    if (state.imageUrl.isNotBlank()) {
                        TextButton(
                            onClick = viewModel::clearImage,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.form_image_clear))
                        }
                    }
                }
            }

            val imageError = state.visibleError(FormField.IMAGE)
            if (imageError != null) {
                Text(
                    text = imageError.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ---------------- Acciones ----------------
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.form_save))
                }
            }

            OutlinedButton(
                onClick = viewModel::onBackRequested,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.form_cancel))
            }
        }

        // Confirmacion al salir con cambios sin guardar.
        if (state.showDiscardDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDiscard,
                title = { Text(stringResource(R.string.form_discard_title)) },
                text = { Text(stringResource(R.string.form_discard_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDiscard) {
                        Text(stringResource(R.string.form_discard_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDiscard) {
                        Text(stringResource(R.string.form_discard_cancel))
                    }
                }
            )
        }
    }
}
