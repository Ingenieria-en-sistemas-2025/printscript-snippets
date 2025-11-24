package com.printscript.snippets.auth0

import com.printscript.snippets.error.RunTimeError
import com.printscript.snippets.user.auth0.Auth0ManagementTokenService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.time.Instant

class Auth0ManagementTokenServiceTest {

    private fun newServiceWithRest(
        issuer: String = "https://auth0.example.com/",
    ): Triple<Auth0ManagementTokenService, RestClient.Builder, RestClient> {
        val builder = Mockito.mock(RestClient.Builder::class.java)
        val rest = Mockito.mock(RestClient::class.java, Mockito.RETURNS_DEEP_STUBS)

        Mockito.`when`(builder.build()).thenReturn(rest)

        val service =
            Auth0ManagementTokenService(
                issuer = issuer,
                clientId = "client-id",
                clientSecret = "client-secret",
                restBuilder = builder,
            )

        return Triple(service, builder, rest)
    }

    @Test
    fun `getAccessToken lanza RunTimeError si Auth0 devuelve body nulo`() {
        val (service, _, rest) = newServiceWithRest()

        // El último body(TokenResponse::class.java) devuelve null
        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenReturn(null)

        val ex =
            assertThrows<RunTimeError> {
                service.getAccessToken()
            }

        Assertions.assertTrue(ex.message!!.contains("Respuesta nula del endpoint de token de Auth0"))
    }

    @Test
    fun `getAccessToken no llama a Auth0 si ya hay token valido y lejos de expirar`() {
        val (service, _, rest) = newServiceWithRest()

        // seteo manualmente el estado interno para simular token ya presente y válido
        val clazz = Auth0ManagementTokenService::class.java
        val tokenField = clazz.getDeclaredField("accessToken").apply { isAccessible = true }
        val expiresAtField = clazz.getDeclaredField("expiresAt").apply { isAccessible = true }

        tokenField.set(service, "cached-token")
        expiresAtField.set(service, Instant.now().plusSeconds(3600)) // lejos de expirar

        val token = service.getAccessToken()

        Assertions.assertEquals("cached-token", token)
        // no se debería haber contactado a Auth0
        Mockito.verify(rest, Mockito.times(0)).post()
    }
}
