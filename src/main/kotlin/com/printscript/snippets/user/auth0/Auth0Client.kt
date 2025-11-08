package com.printscript.snippets.user.auth0

import com.printscript.snippets.user.User
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

class Auth0Client(
    private val auth0Url: String,
    private val restTemplate: RestTemplate,
    private val auth0ManagementTokenService: Auth0ManagementTokenService,
) : IdentityProviderClient {

    override fun getAllUsers(): List<User> {
        val url = "$auth0Url/api/v2/users"
        val headers = getJsonHeader()
        val entity: HttpEntity<Void> = HttpEntity(headers)

        val response = this.restTemplate.exchange<List<User>>(url, HttpMethod.GET, entity)

        return response.body ?: emptyList()
    }

    override fun getUserById(userId: String): User {
        val url = "$auth0Url/api/v2/users/$userId"
        val headers = getJsonHeader()
        val entity: HttpEntity<Void> = HttpEntity(headers)

        val response = this.restTemplate.exchange<User>(url, HttpMethod.GET, entity)

        if (!response.statusCode.is2xxSuccessful) {
            throw NoSuchElementException("User with ID $userId not found in Auth0 or request failed. Status: ${response.statusCode}")
        }
        return response.body ?: throw NoSuchElementException("Auth0 API returned no user body for ID $userId")
    }

    private fun getJsonHeader(): HttpHeaders {
        // [CAMBIADO] Usa el nuevo servicio para obtener el token
        val token = auth0ManagementTokenService.getAccessToken()

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer $token")
            }
        return headers
    }
}
