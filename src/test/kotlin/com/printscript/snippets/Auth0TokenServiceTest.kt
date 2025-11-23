package com.printscript.snippets

import com.printscript.snippets.auth.Auth0TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.client.RestClient
import java.time.Instant

class Auth0TokenServiceTest {

    // Builder "normal"
    private val builder: RestClient.Builder = Mockito.mock(RestClient.Builder::class.java)

    // RestClient con deep stubs para poder encadenar post().uri()...
    private val restClient: RestClient =
        Mockito.mock(RestClient::class.java, Mockito.RETURNS_DEEP_STUBS)

    private fun newService(): Auth0TokenService {
        // Siempre que construyamos el service, el builder debe devolver este restClient
        Mockito.`when`(builder.build()).thenReturn(restClient)

        return Auth0TokenService(
            issuer = "https://tenant/",
            clientId = "id",
            clientSecret = "secret",
            audience = "aud",
            restBuilder = builder,
        )
    }

//    @Test
//    fun `getAccessToken obtiene token nuevo y lo reutiliza mientras no expira`() {
//        val service = newService()
//
//        // Instanciamos el TokenResponse privado por reflexión
//        val tokenClass =
//            Class.forName("com.printscript.snippets.auth.Auth0TokenService\$TokenResponse")
//        val ctor = tokenClass.getDeclaredConstructor(
//            String::class.java,
//            Int::class.javaPrimitiveType,
//            String::class.java,
//        )
//        ctor.isAccessible = true
//        val tokenInstance = ctor.newInstance("m2m-token", 3600, "Bearer")
//
//        // Stub de TODA la cadena:
//        Mockito.`when`(
//            restClient
//                .post()
//                .uri(anyString())
//                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
//                .body(any())
//                .retrieve()
//                .body(Mockito.eq(tokenClass) as Class<Any>),
//        ).thenReturn(tokenInstance)
//
//        val first = service.getAccessToken()
//        val second = service.getAccessToken()
//
//        assertEquals("m2m-token", first)
//        assertEquals(first, second)
//
//        // Solo se llamó una vez al endpoint de Auth0
//        Mockito.verify(restClient, Mockito.times(1)).post()
//    }
//
//    @Test
//    fun `refreshToken envuelve RestClientResponseException en RunTimeError`() {
//        val service = newService()
//
//        val tokenClass =
//            Class.forName("com.printscript.snippets.auth.Auth0TokenService\$TokenResponse")
//
//        val ex = RestClientResponseException(
//            "boom",
//            500,
//            "Internal Server Error",
//            null,
//            null,
//            null,
//        )
//
//        Mockito.`when`(
//            restClient
//                .post()
//                .uri(anyString())
//                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
//                .body(any())
//                .retrieve()
//                .body(Mockito.eq(tokenClass) as Class<Any>),
//        ).thenThrow(ex)
//
//        val thrown = assertThrows<RunTimeError> {
//            service.getAccessToken()
//        }
//
//        assertEquals("Fallo al obtener token M2M de Auth0.", thrown.message)
//    }
//
//    @Test
//    fun `refreshToken envuelve ResourceAccessException en RunTimeError`() {
//        val service = newService()
//
//        val tokenClass =
//            Class.forName("com.printscript.snippets.auth.Auth0TokenService\$TokenResponse")
//
//        val ex = ResourceAccessException("timeout")
//
//        Mockito.`when`(
//            restClient
//                .post()
//                .uri(anyString())
//                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
//                .body(any())
//                .retrieve()
//                .body(Mockito.eq(tokenClass) as Class<Any>),
//        ).thenThrow(ex)
//
//        val thrown = assertThrows<RunTimeError> {
//            service.getAccessToken()
//        }
//
//        assertEquals("Fallo al obtener token M2M de Auth0.", thrown.message)
//    }
//
//    @Test
//    fun `refreshToken lanza RunTimeError si body nulo`() {
//        val service = newService()
//
//        val tokenClass =
//            Class.forName("com.printscript.snippets.auth.Auth0TokenService\$TokenResponse")
//
//        Mockito.`when`(
//            restClient
//                .post()
//                .uri(anyString())
//                .contentType(Mockito.eq(MediaType.APPLICATION_JSON))
//                .body(any())
//                .retrieve()
//                .body(Mockito.eq(tokenClass) as Class<Any>),
//        ).thenReturn(null)
//
//        val thrown = assertThrows<RunTimeError> {
//            service.getAccessToken()
//        }
//
//        assertEquals("Respuesta nula del endpoint de token de Auth0.", thrown.message)
//    }

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
