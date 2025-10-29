package com.printscript.tests.permission

import com.printscript.tests.auth.Auth0TokenService
import com.printscript.tests.permission.dto.PermissionCreateSnippetInput
import com.printscript.tests.permission.dto.SnippetPermissionListResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class RemoteSnippetPermission(
    private val auth0TokenService: Auth0TokenService, // servicio de token M2M
    private val restClient: RestClient,
    @param:Value("\${authorization.service.url}") private val permissionServiceUrl: String,
) : SnippetPermission {

    override fun createAuthorization(
        input: PermissionCreateSnippetInput,
        token: String, // M2M
    ): ResponseEntity<String> {
        val m2mToken = auth0TokenService.getAccessToken()

        return restClient.post()
            .uri("$permissionServiceUrl/authorization/create")
            .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
            .body(input)
            .retrieve()
            .toEntity(String::class.java)
    }

    override fun getAuthorBySnippetId(snippetId: String, token: String): ResponseEntity<String> {
        val m2mToken = auth0TokenService.getAccessToken()

        return restClient.get()
            .uri("$permissionServiceUrl/authorization/owner/$snippetId")
            .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
            .retrieve()
            .toEntity(String::class.java)
    }

    override fun getAllSnippetsPermission(
        userId: String,
        token: String,
        pageNum: Int,
        pageSize: Int,
    ): ResponseEntity<SnippetPermissionListResponse> {
        val m2mToken = auth0TokenService.getAccessToken()

        val uri = UriComponentsBuilder.fromUriString("$permissionServiceUrl/authorization/my")
            .queryParam("userId", userId)
            .queryParam("pageNum", pageNum)
            .queryParam("pageSize", pageSize)
            .toUriString()

        return restClient.get()
            .uri(uri)
            .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
            .retrieve()
            .toEntity(SnippetPermissionListResponse::class.java)
    }

    override fun deleteSnippetPermissions(snippetId: String, token: String): ResponseEntity<Unit> {
        val m2mToken = auth0TokenService.getAccessToken()
        restClient.delete()
            .uri("$permissionServiceUrl/authorization/snippet/$snippetId")
            .headers { it.set(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken") }
            .retrieve()
            .toBodilessEntity()

        return ResponseEntity.ok().build()
    }
}
