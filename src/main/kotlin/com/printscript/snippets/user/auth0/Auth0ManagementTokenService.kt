package com.printscript.snippets.user.auth0

import com.fasterxml.jackson.annotation.JsonProperty
import com.printscript.snippets.error.RunTimeError
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Instant

@Service
class Auth0ManagementTokenService(
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuer: String,
    @param:Value("\${auth0.client-id}")
    private val clientId: String,
    @param:Value("\${auth0.client-secret}")
    private val clientSecret: String,
    @param:Qualifier("plainRestClient")
    private val rest: RestClient,
) {
    private val logger = LoggerFactory.getLogger(Auth0ManagementTokenService::class.java)
    private var accessToken: String = ""
    private var expiresAt: Instant = Instant.EPOCH

    companion object {
        private const val TOKEN_RENEW_WINDOW_SEC: Long = 60

        private const val MANAGEMENT_API_AUDIENCE_SUFFIX = "api/v2/"
    }

    fun getAccessToken(): String {
        val needsRefresh = accessToken.isBlank() ||
            Instant.now().plusSeconds(TOKEN_RENEW_WINDOW_SEC).isAfter(expiresAt)

        if (needsRefresh) {
            refreshToken()
        }
        return accessToken
    }

    private fun refreshToken() {
        logger.info("Token M2M para Management API expirado o próximo a expirar. Solicitando nuevo token a Auth0.")

        val base = if (issuer.endsWith("/")) issuer else "$issuer/"
        val authUrl = base + "oauth/token"

        val managementApiAudience = base + MANAGEMENT_API_AUDIENCE_SUFFIX

        val requestBody = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "audience" to managementApiAudience,
        )

        try {
            val response = rest.post()
                .uri(authUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(TokenResponse::class.java)
                ?: throw RunTimeError("Respuesta nula del endpoint de token de Auth0.")

            this.accessToken = response.accessToken
            this.expiresAt = Instant.now().plusSeconds(response.expiresIn.toLong())
            logger.info("Nuevo token M2M para Management API obtenido. Expira en {} segundos.", response.expiresIn)
        } catch (e: RestClientResponseException) {
            logger.error("Fallo al obtener el token M2M de Auth0 (HTTP ${e.statusCode.value()}). Confirme el scope 'read:users' en la Management API.", e)
            throw RunTimeError("Fallo al obtener token M2M de Auth0.")
        } catch (e: ResourceAccessException) {
            logger.error("Fallo de conexión con Auth0 (timeout o red)", e)
            throw RunTimeError("Fallo al obtener token M2M de Auth0.")
        }
    }

    private data class TokenResponse(
        @get:JsonProperty("access_token") val accessToken: String,
        @get:JsonProperty("expires_in") val expiresIn: Int,
        @get:JsonProperty("token_type") val tokenType: String,
    )
}
