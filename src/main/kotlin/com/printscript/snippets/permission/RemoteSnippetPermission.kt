package com.printscript.snippets.permission

import com.printscript.snippets.auth.Auth0TokenService
import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import com.printscript.snippets.permission.dto.SnippetPermissionListResponse
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

    private fun bearer(h: HttpHeaders) {
        h.set(HttpHeaders.AUTHORIZATION, "Bearer ${auth0TokenService.getAccessToken()}")
    }

    private fun trimSlash(base: String) =
        if (base.endsWith("/")) base.dropLast(1) else base

    override fun createAuthorization(
        input: PermissionCreateSnippetInput,
        token: String,
    ): ResponseEntity<String> {
        val url = "${trimSlash(permissionServiceUrl)}/authorization/create"
        return restClient.post()
            .uri(url)
            .headers(::bearer)
            .body(input)
            .retrieve()
            .toEntity(String::class.java)
    }

    override fun getAuthorBySnippetId(snippetId: String, token: String): ResponseEntity<String> {
        val url = "${trimSlash(permissionServiceUrl)}/authorization/owner/$snippetId"
        return restClient.get()
            .uri(url)
            .headers(::bearer)
            .retrieve()
            .toEntity(String::class.java)
    }

    override fun getAllSnippetsPermission(
        userId: String, // ignorado por el controller
        token: String,
        pageNum: Int,
        pageSize: Int,
    ): ResponseEntity<SnippetPermissionListResponse> {
        val url = UriComponentsBuilder
            .fromUriString("${trimSlash(permissionServiceUrl)}/authorization/my")
            .queryParam("page", pageNum)
            .queryParam("size", pageSize)
            .toUriString()

        return restClient.get()
            .uri(url)
            .headers(::bearer)
            .retrieve()
            .toEntity(SnippetPermissionListResponse::class.java)
    }

    override fun deleteSnippetPermissions(snippetId: String, token: String): ResponseEntity<Unit> {
        val url = "${trimSlash(permissionServiceUrl)}/authorization/snippet/$snippetId"
        restClient.delete()
            .uri(url)
            .headers(::bearer)
            .retrieve()
            .toBodilessEntity()

        return ResponseEntity.ok().build()
    }
}