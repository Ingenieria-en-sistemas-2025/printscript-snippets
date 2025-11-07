package com.printscript.snippets.permission

import com.printscript.snippets.auth.Auth0TokenService
import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import com.printscript.snippets.permission.dto.SnippetPermissionListResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.util.UriComponentsBuilder

@Component
class RemoteSnippetPermission(
    private val auth0TokenService: Auth0TokenService,
    private val restClient: RestClient,
    @param:Value("\${authorization.service.url}") private val permissionServiceUrl: String,
) : SnippetPermission {

    private val logger = LoggerFactory.getLogger(RemoteSnippetPermission::class.java)

    override fun createAuthorization(
        input: PermissionCreateSnippetInput,
    ): ResponseEntity<String> {
        return try {
            val m2mToken = auth0TokenService.getAccessToken()

            restClient.post()
                .uri("$permissionServiceUrl/authorization/create")
                .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
                .body(input)
                .retrieve()
                .toEntity(String::class.java)
        } catch (ex: RestClientException) {
            logger.error("Failed to create authorization for snippet ${input.snippetId}: ${ex.message}", ex)
            throw ex
        }
    }

    override fun getAuthorBySnippetId(
        snippetId: String,
    ): ResponseEntity<String> {
        return try {
            val m2mToken = auth0TokenService.getAccessToken()

            restClient.get()
                .uri("$permissionServiceUrl/authorization/owner/$snippetId")
                .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
                .retrieve()
                .toEntity(String::class.java)
        } catch (ex: RestClientException) {
            logger.error("Failed to get owner for snippet $snippetId: ${ex.message}", ex)
            throw ex
        }
    }

    override fun getAllSnippetsPermission(
        userId: String,
        pageNum: Int,
        pageSize: Int,
    ): ResponseEntity<SnippetPermissionListResponse> {
        return try {
            val m2mToken = auth0TokenService.getAccessToken()

            val uri = UriComponentsBuilder.fromUriString("$permissionServiceUrl/authorization/my")
                .queryParam("userId", userId)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .toUriString()

            restClient.get()
                .uri(uri)
                .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
                .retrieve()
                .toEntity(SnippetPermissionListResponse::class.java)
        } catch (ex: RestClientException) {
            logger.error("Failed to get permissions for user $userId: ${ex.message}", ex)
            throw ex
        }
    }

    override fun deleteSnippetPermissions(
        snippetId: String,
    ): ResponseEntity<Unit> {
        return try {
            val m2mToken = auth0TokenService.getAccessToken()

            restClient.delete()
                .uri("$permissionServiceUrl/authorization/snippet/$snippetId")
                .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
                .retrieve()
                .toBodilessEntity()

            ResponseEntity.ok().build()
        } catch (ex: RestClientException) {
            logger.error("Failed to delete permissions for snippet $snippetId: ${ex.message}", ex)
            throw ex
        }
    }
}
