package com.example.georhythm.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class ConnectionError(val cause: Throwable) : NetworkResult<Nothing>()
    data class ParseError(val message: String) : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is HttpError -> this
        is ConnectionError -> this
        is ParseError -> this
    }

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (String) -> Unit): NetworkResult<T> {
        when (this) {
            is HttpError -> action("HTTP $code: $message")
            is ConnectionError -> action("Conexión: ${cause.message}")
            is ParseError -> action("Parseo: $message")
            is Success -> Unit
        }
        return this
    }
}
