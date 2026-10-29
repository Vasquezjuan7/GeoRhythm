package com.example.georhythm.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `success devuelve los datos correctamente y se mapea`() {
        val res = NetworkResult.Success("120 bpm")
        assertTrue(res.isSuccess)
        assertEquals("120 bpm", res.getOrNull())
        val mapped = res.map { it.length }
        assertEquals(7, mapped.getOrNull())
    }

    @Test
    fun `errores HTTP y de conexion no son exitosos y devuelven null`() {
        val err = NetworkResult.HttpError(404, "Not Found")
        assertFalse(err.isSuccess)
        assertNull(err.getOrNull())
        val connErr = NetworkResult.ConnectionError(RuntimeException("Timeout"))
        assertFalse(connErr.isSuccess)
        assertNull(connErr.getOrNull())
    }

    @Test
    fun `callbacks onSuccess y onFailure se ejecutan segun el estado`() {
        var calledSuccess = false
        var calledFailure = false
        NetworkResult.Success(42).onSuccess { calledSuccess = true }
        NetworkResult.HttpError(500, "Server Error").onFailure { calledFailure = true }
        assertTrue(calledSuccess)
        assertTrue(calledFailure)
    }
}
