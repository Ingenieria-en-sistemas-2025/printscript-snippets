package com.printscript.snippets.auth0

import com.printscript.snippets.auth.Auth0TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.client.RestClient
import java.time.Instant

class Auth0TokenServiceTest {

    private val builder: RestClient.Builder = Mockito.mock(RestClient.Builder::class.java)

    private val restClient: RestClient =
        Mockito.mock(RestClient::class.java, Mockito.RETURNS_DEEP_STUBS)

    private fun newService(): Auth0TokenService {
        Mockito.`when`(builder.build()).thenReturn(restClient)

        return Auth0TokenService(
            issuer = "https://tenant/",
            clientId = "id",
            clientSecret = "secret",
            audience = "aud",
            restBuilder = builder,
        )
    }

    @Test
    fun `getAccessToken no refresca si token aun valido`() {
        val service = newService()

        // Seteamos token y expiración directamente en los campos privados
        val accessTokenField = Auth0TokenService::class.java.getDeclaredField("accessToken")
        accessTokenField.isAccessible = true
        accessTokenField.set(service, "cached-token")

        val expiresAtField = Auth0TokenService::class.java.getDeclaredField("expiresAt")
        expiresAtField.isAccessible = true
        expiresAtField.set(service, Instant.now().plusSeconds(3600))

        val token = service.getAccessToken()
        assertEquals("cached-token", token)

        // El restClient nunca se usa para postear a Auth0
        Mockito.verify(restClient, Mockito.never()).post()
    }
}
