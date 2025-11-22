package com.printscript.snippets.permission

import com.printscript.snippets.auth.Auth0TokenService
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import io.printscript.contracts.permissions.SnippetPermissionListResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class RemoteSnippetPermission(
    // ya no necesitamos usar el token acá directamente, lo pone el interceptor
    @Qualifier("m2mRestTemplate") private val restTemplate: RestTemplate,
    @param:Value("\${authorization.service.url}") private val permissionServiceUrl: String,
) : SnippetPermission {

    private val logger = LoggerFactory.getLogger(RemoteSnippetPermission::class.java)

    override fun createAuthorization(input: PermissionCreateSnippetInput): ResponseEntity<String> {
        return try {
            // POST /authorization con body JSON y Authorization agregado por el interceptor
            val response = restTemplate.exchange(
                "$permissionServiceUrl/authorization",
                HttpMethod.POST,
                HttpEntity(input),         // body = input, headers los maneja el interceptor
                String::class.java
            )
            response
        } catch (ex: RestClientException) {
            logger.error("Failed to create authorization for snippet ${input.snippetId}: ${ex.message}", ex)
            throw ex
        }
    }

    override fun getAllSnippetsPermission(
        userId: String,
        pageNum: Int,
        pageSize: Int,
    ): ResponseEntity<SnippetPermissionListResponse> {
        return try {
            val uri = UriComponentsBuilder
                .fromUriString("$permissionServiceUrl/authorization/me")
                .queryParam("userId", userId)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .toUriString()

            val response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                SnippetPermissionListResponse::class.java
            )
            response
        } catch (ex: RestClientException) {
            logger.error("Failed to get permissions for user $userId: ${ex.message}", ex)
            throw ex
        }
    }

    override fun deleteSnippetPermissions(snippetId: String): ResponseEntity<Unit> {
        return try {
            restTemplate.exchange(
                "$permissionServiceUrl/authorization/snippet/$snippetId",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void::class.java,
            )

            ResponseEntity.ok().build()
        } catch (ex: RestClientException) {
            logger.error("Failed to delete permissions for snippet $snippetId: ${ex.message}", ex)
            throw ex
        }
    }

    override fun getUserScopeForSnippet(userId: String, snippetId: String): String? {
        return try {
            val res = getAllSnippetsPermission(userId, 0, Int.MAX_VALUE).body
            res?.authorizations
                ?.firstOrNull { it.snippetId.equals(snippetId, ignoreCase = true) }
                ?.scope
        } catch (ex: RestClientException) {
            logger.error("Failed to resolve scope for user $userId on snippet $snippetId: ${ex.message}", ex)
            throw ex
        }
    }
}