package com.printscript.snippets.auth0


import com.printscript.snippets.user.auth0.Auth0Client
import com.printscript.snippets.user.auth0.Auth0IntegrationConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.web.client.RestTemplate
import com.printscript.snippets.user.auth0.Auth0ManagementTokenService
import com.printscript.snippets.user.auth0.IdentityProviderClient

class Auth0IntegrationConfigTest {

    private val restTemplateMock: RestTemplate = mock(RestTemplate::class.java)
    private val tokenServiceMock: Auth0ManagementTokenService = mock(Auth0ManagementTokenService::class.java)

    @Test
    fun `identityProviderClient crea un Auth0Client con el issuer y dependencias correctas`() {
        // dado un issuer de prueba
        val issuer = "https://auth0.test/"

        val config = Auth0IntegrationConfig(issuer)

        val client: IdentityProviderClient =
            config.identityProviderClient(
                restTemplate = restTemplateMock,
                auth0ManagementTokenService = tokenServiceMock,
            )

        // Es del tipo correcto
        assertTrue(client is Auth0Client)

        client as Auth0Client

        // ==== Reflection para verificar campos privados ====

        val clazz = Auth0Client::class.java

        val urlField = clazz.getDeclaredField("auth0Url").apply { isAccessible = true }
        val restTemplateField = clazz.getDeclaredField("restTemplate").apply { isAccessible = true }
        val tokenServiceField = clazz.getDeclaredField("auth0ManagementTokenService").apply { isAccessible = true }

        val urlValue = urlField.get(client) as String
        val restTemplateValue = restTemplateField.get(client) as RestTemplate
        val tokenServiceValue = tokenServiceField.get(client) as Auth0ManagementTokenService

        assertEquals(issuer, urlValue)
        assertSame(restTemplateMock, restTemplateValue)
        assertSame(tokenServiceMock, tokenServiceValue)
    }

    @Test
    fun `restTemplate bean devuelve una instancia valida`() {
        val config = Auth0IntegrationConfig("https://whatever/")

        val rt1 = config.restTemplate()
        val rt2 = config.restTemplate()

        assertNotNull(rt1)
        assertTrue(rt1 is RestTemplate)

        // opcional: verificar que crea instancias nuevas en cada llamada
        assertNotSame(rt1, rt2)
    }
}
