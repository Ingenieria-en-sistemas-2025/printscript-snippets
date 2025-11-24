package com.printscript.snippets

import com.printscript.snippets.auth.Auth0TokenService
import com.printscript.snippets.config.HttpClientConfig
import com.printscript.snippets.logs.CorrelationIdFilter.Companion.CORRELATION_ID_HEADER
import com.printscript.snippets.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse

class HttpClientConfigTest {

    @Test
    fun `m2mRestTemplate agrega bearer token y correlation id cuando corresponde`() {
        // mock del servicio de tokens
        val tokenService = Mockito.mock(Auth0TokenService::class.java)
        Mockito.`when`(tokenService.getAccessToken()).thenReturn("tkn-123")

        val config = HttpClientConfig()
        val restTemplate = config.m2mRestTemplate(tokenService)

        // tiene exactamente 2 interceptores (auth + correlation)
        assertEquals(2, restTemplate.interceptors.size)

        val authInterceptor = restTemplate.interceptors[0]
        val corrInterceptor = restTemplate.interceptors[1]

        // -------- auth interceptor --------
        val req1 = MockClientHttpRequest(HttpMethod.GET, java.net.URI("https://example.com/auth"))
        val exec1 = ClientHttpRequestExecution { request, body ->
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        authInterceptor.intercept(req1, ByteArray(0), exec1)

        assertEquals("Bearer tkn-123", req1.headers.getFirst(HttpHeaders.AUTHORIZATION))
        Mockito.verify(tokenService, Mockito.times(1)).getAccessToken()

        // -------- correlation interceptor con MDC presente --------
        MDC.put(CORRELATION_ID_KEY, "corr-123")
        val req2 = MockClientHttpRequest(HttpMethod.GET, java.net.URI("https://example.com/corr1"))
        val exec2 = ClientHttpRequestExecution { request, body ->
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        corrInterceptor.intercept(req2, ByteArray(0), exec2)
        assertEquals("corr-123", req2.headers.getFirst(CORRELATION_ID_HEADER))

        // -------- correlation interceptor sin MDC (no debe agregar header) --------
        MDC.remove(CORRELATION_ID_KEY)
        val req3 = MockClientHttpRequest(HttpMethod.GET, java.net.URI("https://example.com/corr2"))
        val exec3 = ClientHttpRequestExecution { request, body ->
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }

        corrInterceptor.intercept(req3, ByteArray(0), exec3)
        assertNull(req3.headers.getFirst(CORRELATION_ID_HEADER))
    }
}
