package com.example.georhythm.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRetryTest {

    private val policy = HttpRetryPolicy(maxRetries = 3, initialDelayMs = 500L, backoffMultiplier = 2.0)

    @Test
    fun `los reintentos aplican backoff exponencial`() {
        assertEquals(500L, policy.delayForAttempt(1))
        assertEquals(1000L, policy.delayForAttempt(2))
        assertEquals(2000L, policy.delayForAttempt(3))
    }

    @Test
    fun `reintenta en errores de servidor y rate limit pero no en errores de cliente`() {
        assertTrue(policy.shouldRetry(500))
        assertTrue(policy.shouldRetry(503))
        assertTrue(policy.shouldRetry(429))
        assertFalse(policy.shouldRetry(400))
        assertFalse(policy.shouldRetry(401))
        assertFalse(policy.shouldRetry(404))
    }
}
