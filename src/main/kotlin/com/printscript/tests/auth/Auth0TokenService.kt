package com.printscript.tests.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.printscript.tests.error.RunTimeError
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

@Service
class Auth0TokenService(
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuer: String,
    @param:Value("\${auth0.client-id}")
    private val clientId: String,
    @param:Value("\${auth0.client-secret}")
    private val clientSecret: String,
    @param:Value("\${auth0.audience}")
    private val audience: String,
    @Qualifier("plainRestClient")
    private val rest: RestClient,
) {
    private val logger = LoggerFactory.getLogger(Auth0TokenService::class.java)
    private var accessToken: String = ""
    private var expiresAt: Instant = Instant.MIN

    fun getAccessToken(): String {
        // Renueva el token si expira en menos de 60 segundos
        if (Instant.now().isAfter(expiresAt.minusSeconds(60))) {
            refreshToken()
        }
        return accessToken
    }

    private fun refreshToken() {
        logger.info("Token M2M expirado o próximo a expirar. Solicitando nuevo token a Auth0.")
        val authUrl = "$issuer" + "oauth/token"

        val requestBody = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "audience" to audience
        )

        try {
            val base = if (issuer.endsWith("/")) issuer else "$issuer/"
            val authUrl = base + "oauth/token"

            val response = rest.post()
                .uri(authUrl)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw RunTimeError("Respuesta nula del endpoint de token de Auth0.")

            this.accessToken = response.accessToken
            this.expiresAt = Instant.now().plusSeconds(response.expiresIn.toLong())
            logger.info("Nuevo token M2M obtenido. Expira en {} segundos.", response.expiresIn)
        } catch (e: Exception) {
            logger.error("Fallo al obtener el token M2M de Auth0. Asegúrate que AUTH0_M2M_CLIENT_ID y SECRET son correctos.", e)
            throw RunTimeError("Fallo al obtener token M2M de Auth0.")
        }
    }

    private data class TokenResponse(
        @get:JsonProperty("access_token") val accessToken: String,
        @get:JsonProperty("expires_in") val expiresIn: Int,
        @get:JsonProperty("token_type") val tokenType: String,
    )
}
