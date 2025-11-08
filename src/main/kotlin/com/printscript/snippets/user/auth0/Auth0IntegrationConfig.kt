package com.printscript.snippets.user.auth0

import com.printscript.snippets.auth.Auth0TokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class Auth0IntegrationConfig
constructor(
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val auth0IssuerUri: String,
) {

    @Bean
    fun identityProviderClient(
        restTemplate: RestTemplate,
        auth0TokenService: Auth0TokenService,
    ): IdentityProviderClient {
        return Auth0Client(
            auth0IssuerUri,
            restTemplate,
            auth0TokenService,
        )
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
