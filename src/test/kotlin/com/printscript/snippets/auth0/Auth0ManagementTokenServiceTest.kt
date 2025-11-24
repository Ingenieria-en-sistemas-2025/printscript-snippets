package com.printscript.snippets.auth0

import com.printscript.snippets.error.RunTimeError
import com.printscript.snippets.user.auth0.Auth0ManagementTokenService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.nio.charset.StandardCharsets
import java.time.Instant

class Auth0ManagementTokenServiceTest {


    private fun newTokenResponse(
        accessToken: String,
        expiresIn: Int,
        tokenType: String = "Bearer",
    ): Any {
        val clazz =
            Class.forName(
                "com.printscript.snippets.user.auth0.Auth0ManagementTokenService\$TokenResponse",
            )
        val ctor = clazz.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType, String::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(accessToken, expiresIn, tokenType)
    }


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
    fun `getAccessToken obtiene token cuando no hay y lo cachea mientras no expire`() {
        val (service, _, rest) = newServiceWithRest()

        val tokenResponse = newTokenResponse("m2m-token-123", 3600)

        // stub de TODA la cadena post().uri().contentType().body().retrieve().body()
        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenReturn(tokenResponse)

        val token1 = service.getAccessToken() // refresca
        val token2 = service.getAccessToken() // usa cache

        Assertions.assertEquals("m2m-token-123", token1)
        Assertions.assertEquals("m2m-token-123", token2)

        // Llamó solo una vez a Auth0
        Mockito.verify(rest, Mockito.times(1)).post()
    }

    @Test
    fun `getAccessToken renueva token si esta proximo a expirar`() {
        val (service, _, rest) = newServiceWithRest()

        val first = newTokenResponse("old-token", 10)   // expira enseguida
        val second = newTokenResponse("new-token", 3600)

        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        )
            .thenReturn(first)
            .thenReturn(second)

        val token1 = service.getAccessToken() // primer refresh → old-token
        val token2 = service.getAccessToken() // como expira pronto → vuelve a refrescar → new-token

        Assertions.assertEquals("old-token", token1)
        Assertions.assertEquals("new-token", token2)

        // Dos llamadas a Auth0
        Mockito.verify(rest, Mockito.times(2)).post()
    }


    @Test
    fun `refreshToken arma correctamente la URL cuando issuer termina con barra`() {
        val (service, _, rest) = newServiceWithRest(issuer = "https://tenant.auth0.com/")

        val tokenResponse = newTokenResponse("tok", 3600)

        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenReturn(tokenResponse)

        service.getAccessToken()

        val postSpec = rest.post()
        val uriCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(postSpec).uri(uriCaptor.capture())

        Assertions.assertEquals("https://tenant.auth0.com/oauth/token", uriCaptor.value)
    }

    @Test
    fun `refreshToken arma correctamente la URL cuando issuer no tiene barra final`() {
        val (service, _, rest) = newServiceWithRest(issuer = "https://tenant.auth0.com")

        val tokenResponse = newTokenResponse("tok", 3600)

        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenReturn(tokenResponse)

        service.getAccessToken()

        val postSpec = rest.post()
        val uriCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(postSpec).uri(uriCaptor.capture())

        Assertions.assertEquals("https://tenant.auth0.com/oauth/token", uriCaptor.value)
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
    fun `getAccessToken envuelve RestClientResponseException en RunTimeError`() {
        val (service, _, rest) = newServiceWithRest()

        val httpException =
            RestClientResponseException(
                "Bad request",
                400,
                "Bad Request",
                HttpHeaders(),
                ByteArray(0),
                StandardCharsets.UTF_8,
            )

        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenThrow(httpException)

        val ex =
            assertThrows<RunTimeError> {
                service.getAccessToken()
            }

        Assertions.assertTrue(ex.message!!.contains("Fallo al obtener token M2M de Auth0."))
    }


    @Test
    fun `getAccessToken envuelve ResourceAccessException en RunTimeError`() {
        val (service, _, rest) = newServiceWithRest()

        Mockito.`when`(
            rest.post()
                .uri(ArgumentMatchers.anyString())
                .contentType(ArgumentMatchers.any(MediaType::class.java))
                .body(ArgumentMatchers.any())
                .retrieve()
                .body(ArgumentMatchers.any<Class<*>>()),
        ).thenThrow(ResourceAccessException("timeout"))

        val ex =
            assertThrows<RunTimeError> {
                service.getAccessToken()
            }

        Assertions.assertTrue(ex.message!!.contains("Fallo al obtener token M2M de Auth0."))
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