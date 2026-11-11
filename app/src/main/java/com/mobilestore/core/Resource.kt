package com.mobilestore.core

/**
 * Resultado de una operacion que puede fallar. Permite que el ViewModel
 * distinga error de red, error de servidor y error de persistencia sin
 * conocer Retrofit ni Room.
 */
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Failure(val error: AppError) : Resource<Nothing>
}

sealed interface AppError {
    /** El servidor respondio, pero no trajo ningun producto utilizable. */
    data object EmptyRemote : AppError

    /** Sin conexion o el host no responde. */
    data object Network : AppError

    /** El servidor respondio con un codigo HTTP de error. */
    data class Server(val code: Int) : AppError

    /** Fallo la escritura o lectura en SQLite. */
    data object Database : AppError

    /** Cualquier otra cosa. */
    data class Unknown(val message: String?) : AppError
}
