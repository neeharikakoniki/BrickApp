package com.brick.earthquaketracker.core.common

/**
 * A typed result wrapper that makes failure handling explicit and exhaustive.
 * Exceptions never cross layer boundaries — they are caught at the data layer
 * and mapped to [DataError], so ViewModels can handle them in a `when` with no `else`.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DataError) : AppResult<Nothing>
}

sealed interface DataError {
    data object NoConnectivity : DataError
    data object Timeout : DataError
    data class Server(val code: Int) : DataError
    data object Serialization : DataError
    data class Unknown(val throwable: Throwable) : DataError
}
