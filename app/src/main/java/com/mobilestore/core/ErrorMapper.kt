package com.mobilestore.core

import com.mobilestore.R

/** Traduce un [AppError] del dominio a un texto de strings.xml. */
fun AppError.toUiText(): UiText = when (this) {
    AppError.EmptyRemote -> UiText.of(R.string.error_empty_remote)
    AppError.Network -> UiText.of(R.string.error_network)
    is AppError.Server -> UiText.of(R.string.error_server, code)
    AppError.Database -> UiText.of(R.string.error_database)
    is AppError.Unknown -> UiText.of(R.string.error_unknown)
}
