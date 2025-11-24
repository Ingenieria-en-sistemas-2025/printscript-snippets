package com.printscript.snippets.auth0

import com.printscript.snippets.user.auth0.Auth0Client
import com.printscript.snippets.user.auth0.Auth0ManagementTokenService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class Auth0ClientTest {

    private val restTemplate = mock(RestTemplate::class.java)
    private val tokenService = mock(Auth0ManagementTokenService::class.java)
    private val client =
        Auth0Client(
            auth0Url = "https://auth0.test",
            restTemplate = restTemplate,
            auth0ManagementTokenService = tokenService,
        )

    // helper para capturar HTTP headers
    private fun captureHeaders(): HttpHeaders {
        val captor = ArgumentCaptor.forClass(HttpEntity::class.java)

        verify(restTemplate).exchange(
            anyString(),
            any(HttpMethod::class.java),
            captor.capture(),
            any<ParameterizedTypeReference<*>>(),
        )

        val entity = captor.value as HttpEntity<*>
        return entity.headers
    }

    // ============================================================
    // getAllUsers()
    // ============================================================

    @Test
    fun `getAllUsers devuelve lista mapeada`() {
        `when`(tokenService.getAccessToken()).thenReturn("token123")

        val dtoList =
            listOf(
                Auth0Client.Auth0UserDto(
                    userId = "u1",
                    name = "Catita",
                    email = "cati@test.com",
                ),
            )

        val response = ResponseEntity(dtoList, HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        `when`(
            restTemplate.exchange(
                eq("https://auth0.test/api/v2/users"),
                eq(HttpMethod.GET),
                any(HttpEntity::class.java),
                any<ParameterizedTypeReference<List<Auth0Client.Auth0UserDto>>>(),
            ),
        ).thenReturn(response as ResponseEntity<List<Auth0Client.Auth0UserDto>>)

        val result = client.getAllUsers()

        assertEquals(1, result.size)
        assertEquals("u1", result[0].userId)
        assertEquals("Catita", result[0].name)
        assertEquals("cati@test.com", result[0].email)

        // headers correctos
        val headers = captureHeaders()
        assertEquals(MediaType.APPLICATION_JSON, headers.contentType)
        assertEquals("Bearer token123", headers.getFirst("Authorization"))
    }

    @Test
    fun `getAllUsers devuelve lista vacia si body es null`() {
        `when`(tokenService.getAccessToken()).thenReturn("abc")

        val response = ResponseEntity<List<Auth0Client.Auth0UserDto>>(null, HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        `when`(
            restTemplate.exchange(
                eq("https://auth0.test/api/v2/users"),
                eq(HttpMethod.GET),
                any(HttpEntity::class.java),
                any<ParameterizedTypeReference<List<Auth0Client.Auth0UserDto>>>(),
            ),
        ).thenReturn(response)

        val result = client.getAllUsers()
        assertTrue(result.isEmpty())
    }

    // ============================================================
    // getUserById()
    // ============================================================

    @Test
    fun `getUserById OK`() {
        `when`(tokenService.getAccessToken()).thenReturn("xyz")

        val dto =
            Auth0Client.Auth0UserDto(
                userId = "u999",
                name = "Pepe",
                email = "pepe@test.com",
            )

        val response = ResponseEntity(dto, HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        `when`(
            restTemplate.exchange(
                eq("https://auth0.test/api/v2/users/u999"),
                eq(HttpMethod.GET),
                any(HttpEntity::class.java),
                any<ParameterizedTypeReference<Auth0Client.Auth0UserDto>>(),
            ),
        ).thenReturn(response)

        val result = client.getUserById("u999")

        assertEquals("u999", result.userId)
        assertEquals("Pepe", result.name)
        assertEquals("pepe@test.com", result.email)
    }

    @Test
    fun `getUserById lanza si status no es 2xx`() {
        `when`(tokenService.getAccessToken()).thenReturn("tkn")

        val response = ResponseEntity<Auth0Client.Auth0UserDto>(null, HttpStatus.NOT_FOUND)

        @Suppress("UNCHECKED_CAST")
        `when`(
            restTemplate.exchange(
                eq("https://auth0.test/api/v2/users/u1"),
                eq(HttpMethod.GET),
                any(HttpEntity::class.java),
                any<ParameterizedTypeReference<Auth0Client.Auth0UserDto>>(),
            ),
        ).thenReturn(response)

        assertThrows<NoSuchElementException> {
            client.getUserById("u1")
        }
    }

    @Test
    fun `getUserById lanza si body es null`() {
        `when`(tokenService.getAccessToken()).thenReturn("tkn")

        val response = ResponseEntity<Auth0Client.Auth0UserDto>(null, HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        `when`(
            restTemplate.exchange(
                eq("https://auth0.test/api/v2/users/u1"),
                eq(HttpMethod.GET),
                any(HttpEntity::class.java),
                any<ParameterizedTypeReference<Auth0Client.Auth0UserDto>>(),
            ),
        ).thenReturn(response)

        assertThrows<NoSuchElementException> {
            client.getUserById("u1")
        }
    }
}
