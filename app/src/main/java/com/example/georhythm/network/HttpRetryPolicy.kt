package com.example.georhythm.network

data class HttpRetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 500L,
    val backoffMultiplier: Double = 2.0
) {
    fun delayForAttempt(attempt: Int): Long {
        if (attempt <= 1) return initialDelayMs
        var delay = initialDelayMs.toDouble()
        for (i in 2..attempt) {
            delay *= backoffMultiplier
        }
        return delay.toLong()
    }

    fun shouldRetry(statusCode: Int): Boolean = statusCode in 500..599 || statusCode == 429
}
