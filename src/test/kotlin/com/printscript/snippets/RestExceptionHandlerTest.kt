package com.printscript.snippets

import com.printscript.snippets.error.RestExceptionHandler
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals

class RestExceptionHandlerTest {

    private val handler = RestExceptionHandler()

    @Test
    fun `badRequest devuelve 400 y BAD_REQUEST con mensaje original`() {
        val ex = IllegalArgumentException("mensaje de error")

        val res = handler.badRequest(ex)

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals("BAD_REQUEST", res.body!!.code)
        assertEquals("mensaje de error", res.body!!.message)
    }

    @Test
    fun `constraint devuelve 400 y CONSTRAINT_VIOLATION`() {
        val ex = ConstraintViolationException("msg", emptySet())

        val res = handler.constraint(ex)

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals("CONSTRAINT_VIOLATION", res.body!!.code)
        assertEquals("Request validation failed", res.body!!.message)
    }

    @Test
    fun `forbidden devuelve 403 y FORBIDDEN`() {
        val ex = org.springframework.security.access.AccessDeniedException("nope")

        val res = handler.forbidden(ex)

        assertEquals(HttpStatus.FORBIDDEN, res.statusCode)
        assertEquals("FORBIDDEN", res.body!!.code)
        assertEquals("Access denied", res.body!!.message)
    }

    @Test
    fun `conflict devuelve 409 y DATA_INTEGRITY`() {
        val ex = DataIntegrityViolationException("constraint")

        val res = handler.conflict(ex)

        assertEquals(HttpStatus.CONFLICT, res.statusCode)
        assertEquals("DATA_INTEGRITY", res.body!!.code)
        assertEquals("Integrity constraint violation", res.body!!.message)
    }

    @Test
    fun `status usa el status y reason de ResponseStatusException`() {
        val ex = ResponseStatusException(HttpStatus.NOT_FOUND, "no existe")

        val res = handler.status(ex)

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
        assertEquals("404", res.body!!.code)
        assertEquals("no existe", res.body!!.message)
    }

    @Test
    fun `upstream mapea 404 a NOT_FOUND y setea UPSTREAM_ERROR`() {
        val ex = restClientExceptionWithStatus(404)

        val res = handler.upstream(ex)

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
        assertEquals("UPSTREAM_ERROR", res.body!!.code)
        assertEquals("Servicio externo respondió 404", res.body!!.message)
    }

    @Test
    fun `upstream mapea otros codigos segun mapUpstreamStatus`() {
        val cases = listOf(
            401 to HttpStatus.UNAUTHORIZED,
            403 to HttpStatus.FORBIDDEN,
            409 to HttpStatus.CONFLICT,
            413 to HttpStatus.PAYLOAD_TOO_LARGE,
            500 to HttpStatus.BAD_GATEWAY, // 5xx
            418 to HttpStatus.BAD_GATEWAY, // default
        )

        cases.forEach { (code, expectedStatus) ->
            val ex = restClientExceptionWithStatus(code)
            val res = handler.upstream(ex)

            assertEquals(expectedStatus, res.statusCode)
            assertEquals("UPSTREAM_ERROR", res.body!!.code)
            assertEquals("Servicio externo respondió $code", res.body!!.message)
        }
    }

    @Test
    fun `timeout devuelve 504 y UPSTREAM_TIMEOUT`() {
        val ex = SocketTimeoutException("timeout")

        val res = handler.timeout(ex)

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, res.statusCode)
        assertEquals("UPSTREAM_TIMEOUT", res.body!!.code)
        assertEquals("Timeout al llamar servicio externo", res.body!!.message)
    }

    @Test
    fun `boom captura Exception generica y devuelve 500 INTERNAL_ERROR`() {
        val ex = RuntimeException("algo explotó")

        val res = handler.boom(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.statusCode)
        assertEquals("INTERNAL_ERROR", res.body!!.code)
        assertEquals("algo explotó", res.body!!.message)
    }

    private fun restClientExceptionWithStatus(statusCode: Int): RestClientResponseException {
        return RestClientResponseException(
            "msg",
            statusCode,
            "status-text",
            HttpHeaders.EMPTY,
            ByteArray(0),
            StandardCharsets.UTF_8,
        )
    }
}
