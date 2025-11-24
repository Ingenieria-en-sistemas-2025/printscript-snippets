 package com.printscript.snippets

 import com.printscript.snippets.auth.Auth0TokenService
 import com.printscript.snippets.auth.Auth0TokenService.TokenResponse
 import com.printscript.snippets.error.RunTimeError
 import org.junit.jupiter.api.Assertions.assertEquals
 import org.junit.jupiter.api.Test
 import org.junit.jupiter.api.assertThrows
 import org.mockito.ArgumentMatchers.any
 import org.mockito.ArgumentMatchers.anyString
 import org.mockito.Mockito
 import org.springframework.http.MediaType
 import org.springframework.web.client.ResourceAccessException
 import org.springframework.web.client.RestClient
 import org.springframework.web.client.RestClientResponseException
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

    /**
     * Happy path: se obtiene un token nuevo y mientras no esté por expirar
     */
    @Test
    fun `getAccessToken obtiene token nuevo y lo reutiliza mientras no expira`() {
        val service = newService()

        val tokenClass = TokenResponse::class.java
        val tokenInstance = TokenResponse("m2m-token", 3600, "Bearer")

        // Para el caso de retorno directo, a menudo la referencia simple funciona si el tipo es explícito
        Mockito.`when`(
            restClient
                .post()
                .uri(anyString())
                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
                .body(any())
                .retrieve()
                .body(tokenClass),
        ).thenReturn(tokenInstance)

        val first = service.getAccessToken()
        val second = service.getAccessToken()

        assertEquals("m2m-token", first)
        assertEquals(first, second)

        Mockito.verify(restClient, Mockito.times(1)).post()
    }

    /**
     * Cuando RestClientResponseException sale de la cadena,
     * refreshToken la envuelve en RunTimeError.
     */
    @Test
    fun `refreshToken envuelve RestClientResponseException en RunTimeError`() {
        val service = newService()

        // Usamos la clase directamente
        val tokenClass = TokenResponse::class.java

        val ex = RestClientResponseException(
            "boom",
            500,
            "Internal Server Error",
            null,
            null,
            null,
        )

        // ** CORRECCIÓN CLAVE: Usamos any(Class<T>) y le forzamos el tipo T en el when **
        Mockito.`when`(
            restClient
                .post()
                .uri(anyString())
                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
                .body(any())
                .retrieve()
                .body(Mockito.any(tokenClass) as Class<TokenResponse>), // <--- FUERZA el tipo genérico 'T'
        ).thenThrow(ex)

        val thrown = assertThrows<RunTimeError> {
            service.getAccessToken()
        }

        assertEquals("Fallo al obtener token M2M de Auth0.", thrown.message)
    }

    /**
     * Cuando hay un problema de red (ResourceAccessException),
     * también se envuelve en RunTimeError.
     */
    @Test
    fun `refreshToken envuelve ResourceAccessException en RunTimeError`() {
        val service = newService()
        val tokenClass = TokenResponse::class.java

        val ex = ResourceAccessException("timeout")

        // ** CORRECCIÓN CLAVE: Usamos any(Class<T>) y le forzamos el tipo T en el when **
        Mockito.`when`(
            restClient
                .post()
                .uri(anyString())
                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
                .body(any())
                .retrieve()
                .body(Mockito.any(tokenClass) as Class<TokenResponse>), // <--- FUERZA el tipo genérico 'T'
        ).thenThrow(ex)

        val thrown = assertThrows<RunTimeError> {
            service.getAccessToken()
        }

        assertEquals("Fallo al obtener token M2M de Auth0.", thrown.message)
    }

    /**
     * Si el endpoint devuelve body nulo, tiramos RunTimeError con el mensaje de respuesta nula.
     */
    @Test
    fun `refreshToken lanza RunTimeError si body nulo`() {
        val service = newService()
        val tokenClass = TokenResponse::class.java

        // ** CORRECCIÓN CLAVE: Usamos eq(Class<T>) y le forzamos el tipo T en el when **
        Mockito.`when`(
            restClient
                .post()
                .uri(anyString())
                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
                .body(any())
                .retrieve()
                .body(Mockito.eq(tokenClass) as Class<TokenResponse>), // <--- FUERZA el tipo genérico 'T'
        ).thenReturn(null)

        val thrown = assertThrows<RunTimeError> {
            service.getAccessToken()
        }

        assertEquals("Respuesta nula del endpoint de token de Auth0.", thrown.message)
    }

    /**
     * Caso donde el token ya está seteado y no va a expirar en el próximo minuto:
     * no se hace ninguna llamada a Auth0.
     */
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
