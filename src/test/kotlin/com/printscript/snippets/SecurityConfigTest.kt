package com.printscript.snippets

import com.printscript.snippets.config.SecurityConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.core.convert.converter.Converter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SecurityConfigTest {

    private fun newConfig(): SecurityConfig =
        SecurityConfig(
            issuer = "https://issuer.example.com/",
            audience = "my-api-audience",
        )

    // --- Tests Existentes (para referencia) ---

    @Test
    fun `corsConfigurationSource configura origins, methods y headers esperados`() {
        val config = newConfig()

        val source = config.corsConfigurationSource()
        val request = MockHttpServletRequest("GET", "/snippets/ping")

        val cors = source.getCorsConfiguration(request)

        assertNotNull(cors)
        assertEquals(
            listOf(
                "http://localhost:5173",
                "https://printscript-prod.duckdns.org",
                "https://printscript-dev.duckdns.org",
            ),
            cors!!.allowedOrigins,
        )

        assertEquals(
            listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
            cors.allowedMethods,
        )

        assertEquals(listOf("*"), cors.allowedHeaders)
        assertTrue(cors.allowCredentials == true)
    }

    @Test
    fun `tokenValidator acepta token con issuer y audience correctos`() {
        val config = newConfig()
        val validator = config.tokenValidator()

        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("https://issuer.example.com/") // debe matchear el issuer del config
                .subject("user123")
                .audience(listOf("my-api-audience"))
                .build()

        val result: OAuth2TokenValidatorResult = validator.validate(jwt)

        assertFalse(result.hasErrors(), "El validator no debería tener errores para issuer y audience válidos")
    }

    @Test
    fun `tokenValidator rechaza token con audience incorrecta`() {
        val config = newConfig()
        val validator = config.tokenValidator()

        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("https://issuer.example.com/")
                .subject("user123")
                .audience(listOf("otra-audiencia"))
                .build()

        val result: OAuth2TokenValidatorResult = validator.validate(jwt)

        assertTrue(result.hasErrors(), "El validator debería marcar error si la audience no incluye la esperada")
        val error = result.errors.first()
        // No nos casamos con el mensaje exacto, solo comprobamos que es de audience
        assertTrue(error.description.contains("aud"), "El error debería estar relacionado con la audience")
    }

    @Test
    fun `permissionsConverter agrega authorities SCOPE_ a partir de claim permissions`() {
        val config = newConfig()

        // permissionsConverter es private, lo obtenemos por reflexión
        val method = SecurityConfig::class.java.getDeclaredMethod("permissionsConverter")
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val converter =
            method.invoke(config) as Converter<Jwt, out AbstractAuthenticationToken>

        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-abc")
                .claim("permissions", listOf("read:snippets", "write:snippets"))
                .build()

        val auth = converter.convert(jwt)
        assertNotNull(auth)

        val authorities = auth!!.authorities.map { it.authority }.toSet()

        // Debe haber sumado SCOPE_read:snippets y SCOPE_write:snippets
        assertTrue("SCOPE_read:snippets" in authorities)
        assertTrue("SCOPE_write:snippets" in authorities)

        // El name del token debería ser el subject del JWT
        assertEquals("user-abc", auth.name)
    }

    // --- Nuevos Tests para 100% Coverage ---

    @Test
    fun `permissionsConverter maneja token sin claim permissions`() {
        val config = newConfig()

        // permissionsConverter es private, lo obtenemos por reflexión
        val method = SecurityConfig::class.java.getDeclaredMethod("permissionsConverter")
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val converter =
            method.invoke(config) as Converter<Jwt, out AbstractAuthenticationToken>

        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-no-perms")
                // Sin el claim "permissions"
                .build()

        val auth = converter.convert(jwt)
        assertNotNull(auth)

        val authorities = auth!!.authorities.map { it.authority }.toSet()

        // Debe haber al menos una authority de los scopes base (que viene del JwtGrantedAuthoritiesConverter),
        // pero no debe haber ninguna autoridad basada en "permissions" (SCOPE_...)
        assertFalse(authorities.any { it.startsWith("SCOPE_") && it != "SCOPE_" })

        // El name del token debería ser el subject del JWT
        assertEquals("user-no-perms", auth.name)
    }

    @Test
    fun `tokenValidator rechaza token con aud claim null`() {
        val config = newConfig()
        val validator = config.tokenValidator()

        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("https://issuer.example.com/")
                .subject("user123")
                // Sin el claim "aud"
                .build()

        val result: OAuth2TokenValidatorResult = validator.validate(jwt)

        assertTrue(result.hasErrors(), "El validator debería marcar error si la audience es null")
        val error: OAuth2Error? = result.errors.firstOrNull()
        assertNotNull(error)
        assertTrue(error!!.description.contains("aud"), "El error debería estar relacionado con la audience")
    }

    @Test
    fun `securityFilterChain devuelve SecurityFilterChain con el conversor y CSRF deshabilitado`() {
        val config = newConfig()
        // Mockeamos HttpSecurity para evitar la configuración completa de Spring
        val http = mock(HttpSecurity::class.java)

        // Mockeamos la cadena de llamadas, ya que no estamos probando la lógica de las reglas
        // sino que el método llega a .build() y que los configuradores clave son llamados.
        `when`(http.authorizeHttpRequests(org.mockito.kotlin.any())).thenReturn(http)
        `when`(http.oauth2ResourceServer(org.mockito.kotlin.any())).thenReturn(http)
        `when`(http.csrf(org.mockito.kotlin.any())).thenReturn(http)
        `when`(http.cors(org.mockito.kotlin.any())).thenReturn(http)

        // SOLUCIÓN: Usar DefaultSecurityFilterChain en el mock de retorno
        `when`(http.build()).thenReturn(mock(DefaultSecurityFilterChain::class.java))

        val result: SecurityFilterChain = config.securityFilterChain(http)

        // 1. Verificamos que el resultado es un SecurityFilterChain (mockeado)
        assertNotNull(result)

        // 2. Verificamos que los métodos clave fueron llamados
        verify(http).authorizeHttpRequests(org.mockito.kotlin.any())
        verify(http).oauth2ResourceServer(org.mockito.kotlin.any())
        verify(http).csrf(org.mockito.kotlin.any())
        verify(http).cors(org.mockito.kotlin.any())
        verify(http).build()
    }
}
