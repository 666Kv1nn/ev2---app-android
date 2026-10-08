package com.example.webservicessencillo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilidadesTest {

    @Test
    fun passwordDebeTenerLetrasYNumeros() {
        assertTrue(passwordValida("Clave2026"))
        assertFalse(passwordValida("12345678"))
        assertFalse(passwordValida("corta1"))
    }

    @Test
    fun usuarioSinEspaciosNiMayusculas() {
        assertTrue(usuarioValido("ana.rojas"))
        assertFalse(usuarioValido("Ana Rojas"))
        assertFalse(usuarioValido("ab"))
    }

    @Test
    fun formatoDeValores() {
        assertEquals("23,5", formatearValor(23.46, "°C"))
        assertEquals("612", formatearValor(611.8, "lux"))
        assertEquals("-", formatearValor(null, "%"))
    }

    @Test
    fun tiempoRelativo() {
        val ahora = leerFecha("2026-10-06T12:00:00Z")!!.time
        assertEquals("ahora", haceCuanto("2026-10-06T11:59:58Z", ahora))
        assertEquals("hace 30 s", haceCuanto("2026-10-06T11:59:30Z", ahora))
        assertEquals("hace 5 min", haceCuanto("2026-10-06T11:55:00Z", ahora))
        assertEquals("nunca", haceCuanto(null, ahora))
    }
}
